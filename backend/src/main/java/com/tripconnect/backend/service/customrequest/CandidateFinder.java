package com.tripconnect.backend.service.customrequest;

import com.tripconnect.backend.dto.customrequest.CustomRequestResponses;
import com.tripconnect.backend.entity.AgentProfile;
import com.tripconnect.backend.entity.AgentServiceArea;
import com.tripconnect.backend.entity.CustomRequest;
import com.tripconnect.backend.entity.Location;
import com.tripconnect.backend.repository.AgentProfileRepository;
import com.tripconnect.backend.repository.AgentServiceAreaRepository;
import com.tripconnect.backend.repository.CustomRequestAssignmentRepository;
import com.tripconnect.backend.repository.CustomRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Lọc và xếp hạng Agent cho một yêu cầu. Đủ điều kiện: hồ sơ đã duyệt, tài khoản hoạt động, đang bật
 * "Nhận yêu cầu", số yêu cầu đang mở chưa đạt giới hạn, chưa từng được giao yêu cầu này.
 * Xếp theo điểm phù hợp ({@link AgentMatcher}), bằng điểm thì ai đăng nhập gần đây hơn đứng trước.
 */
@Component
@RequiredArgsConstructor
public class CandidateFinder {

    private final AgentProfileRepository agentProfileRepository;
    private final AgentServiceAreaRepository serviceAreaRepository;
    private final CustomRequestRepository requestRepository;
    private final CustomRequestAssignmentRepository assignmentRepository;

    public List<CustomRequestResponses.Candidate> find(CustomRequest request) {
        Set<Long> excluded = new HashSet<>(assignmentRepository.findAgentIdsByRequestId(request.getId()));
        List<AgentProfile> profiles = agentProfileRepository.findAcceptingRequests().stream()
                .filter(p -> !excluded.contains(p.getUser().getId())).toList();
        if (profiles.isEmpty()) return List.of();

        List<Long> agentIds = profiles.stream().map(p -> p.getUser().getId()).toList();
        Map<Long, Long> open = requestRepository.countOpenByAgent(agentIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> ((Number) row[1]).longValue()));
        Map<Long, List<Location>> areas = serviceAreaRepository.findByAgentIdIn(agentIds).stream()
                .collect(Collectors.groupingBy(a -> a.getAgent().getId(), Collectors.mapping(AgentServiceArea::getLocation, Collectors.toList())));
        Set<Long> destinationIds = request.getDestinations().stream().map(Location::getId).collect(Collectors.toSet());
        Set<String> destinationCountries = request.getDestinations().stream().map(Location::getCountry).collect(Collectors.toSet());

        List<CustomRequestResponses.Candidate> ranked = profiles.stream()
                .filter(p -> open.getOrDefault(p.getUser().getId(), 0L) < p.getMaxOpenRequests())
                .map(p -> {
                    List<Location> agentAreas = areas.getOrDefault(p.getUser().getId(), List.of());
                    double location = AgentMatcher.locationScore(request.getDestinations(), agentAreas);
                    double rating = AgentMatcher.ratingScore(p.getRating());
                    List<String> matched = agentAreas.stream()
                            .filter(a -> destinationIds.contains(a.getId())
                                    || (a.getProvince() != null && destinationCountries.contains(a.getCountry())))
                            .map(a -> a.getProvince() != null ? a.getProvince() : a.getCountry())
                            .sorted().limit(5).toList();
                    return new CustomRequestResponses.Candidate(
                            p.getUser().getId(), p.getId(), p.getCompanyName(), p.getRating(), p.getRatingCount(),
                            round(location), round(rating), AgentMatcher.total(location, rating),
                            open.getOrDefault(p.getUser().getId(), 0L).intValue(), p.getMaxOpenRequests(), matched,
                            p.getUser().getLastLoginAt(), false);
                })
                .sorted(Comparator.comparing(CustomRequestResponses.Candidate::totalScore).reversed()
                        .thenComparing(CustomRequestResponses.Candidate::lastLoginAt, Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder())))
                .limit(CustomRequestRules.MAX_CANDIDATES)
                .toList();

        List<CustomRequestResponses.Candidate> result = new ArrayList<>();
        for (int i = 0; i < ranked.size(); i++) {
            var c = ranked.get(i);
            result.add(new CustomRequestResponses.Candidate(c.agentId(), c.agentProfileId(), c.companyName(), c.rating(),
                    c.ratingCount(), c.locationScore(), c.ratingScore(), c.totalScore(), c.openRequests(), c.maxOpenRequests(),
                    c.matchedAreas(), c.lastLoginAt(), i < CustomRequestRules.TOP_SUGGESTIONS));
        }
        return result;
    }

    private static BigDecimal round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP);
    }
}
