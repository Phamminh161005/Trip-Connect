package com.tripconnect.backend.service.search;

import com.tripconnect.backend.dto.LocationResponse;
import com.tripconnect.backend.dto.PageResponse;
import com.tripconnect.backend.dto.search.SearchResponses;
import com.tripconnect.backend.dto.search.TourSearchRequest;
import com.tripconnect.backend.entity.AgentProfile;
import com.tripconnect.backend.entity.Location;
import com.tripconnect.backend.entity.Tour;
import com.tripconnect.backend.entity.TourImage;
import com.tripconnect.backend.repository.*;
import com.tripconnect.backend.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Tìm tour cho khách (không cần đăng nhập) + điểm đến nổi bật cho trang chủ. */
@Service
@RequiredArgsConstructor
public class TourSearchService {

    private final TourSearchRepository searchRepository;
    private final TourRepository tourRepository;
    private final TourImageRepository imageRepository;
    private final AgentProfileRepository agentProfileRepository;
    private final LocationRepository locationRepository;
    private final FileStorageService fileStorageService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<SearchResponses.TourCard> search(TourSearchRequest request) {
        TourSearchRepository.Page page = searchRepository.search(request, SearchText.tokens(request.getQ()), LocalDate.now(clock));
        List<SearchResponses.TourCard> cards = toCards(page.hits());
        int totalPages = (int) Math.ceil((double) page.total() / request.getSize());
        return new PageResponse<>(cards, request.getPage(), request.getSize(), page.total(), totalPages);
    }

    @Transactional(readOnly = true)
    public List<SearchResponses.PopularDestination> popularDestinations(int limit) {
        List<TourSearchRepository.DestinationCount> counts = searchRepository.popularDestinations(LocalDate.now(clock), limit);
        if (counts.isEmpty()) return List.of();

        Map<Long, Location> locations = locationRepository
                .findAllById(counts.stream().map(TourSearchRepository.DestinationCount::locationId).toList()).stream()
                .collect(Collectors.toMap(Location::getId, Function.identity()));
        Map<Long, String> covers = coverUrls(counts.stream().map(TourSearchRepository.DestinationCount::sampleTourId).toList());

        return counts.stream()
                .filter(c -> locations.containsKey(c.locationId()))
                .map(c -> {
                    Location l = locations.get(c.locationId());
                    return new SearchResponses.PopularDestination(
                            new LocationResponse(l.getId(), l.getCountry(), l.getProvince()),
                            c.tourCount(), covers.get(c.sampleTourId()));
                })
                .toList();
    }

    /** Nạp thông tin hiển thị cho các tour tìm được — vài câu query cho cả trang, giữ đúng thứ tự kết quả. */
    private List<SearchResponses.TourCard> toCards(List<TourSearchRepository.Hit> hits) {
        if (hits.isEmpty()) return List.of();
        List<Long> ids = hits.stream().map(TourSearchRepository.Hit::tourId).toList();

        Map<Long, Tour> tours = tourRepository.findWithCardDataByIdIn(ids).stream()
                .collect(Collectors.toMap(Tour::getId, Function.identity()));
        Map<Long, String> covers = coverUrls(ids);
        Set<Long> agentIds = tours.values().stream().filter(t -> !t.isPlatformTour())
                .map(t -> t.getAgent().getId()).collect(Collectors.toSet());
        Map<Long, String> companyByAgent = agentIds.isEmpty() ? Map.of()
                : agentProfileRepository.findByUserIdIn(agentIds).stream()
                .filter(p -> p.getCompanyName() != null)
                .collect(Collectors.toMap(p -> p.getUser().getId(), AgentProfile::getCompanyName));

        return hits.stream().filter(h -> tours.containsKey(h.tourId())).map(hit -> {
            Tour t = tours.get(hit.tourId());
            return new SearchResponses.TourCard(
                    t.getId(), t.getTitle(), covers.get(t.getId()),
                    t.getDurationDays(), t.getDurationNights(),
                    t.getDepartureLocation().getProvince(),
                    t.getDestinations().stream()
                            .map(l -> l.getProvince() != null ? l.getProvince() : l.getCountry())
                            .sorted().toList(),
                    t.isInternational(),
                    t.getHighlights().stream().limit(2).toList(),
                    t.getRating(), t.getRatingCount(),
                    t.isPlatformTour() ? "TripConnect" : companyByAgent.getOrDefault(t.getAgent().getId(), "Đối tác TripConnect"),
                    hit.minPrice(), hit.nextDate(), hit.departureCount());
        }).toList();
    }

    /** tourId -> URL ảnh bìa. */
    private Map<Long, String> coverUrls(Collection<Long> tourIds) {
        return imageRepository.findCoverImages(tourIds).stream().collect(Collectors.toMap(
                i -> i.getTour().getId(),
                Function.identity(),
                (a, b) -> a.getId() < b.getId() ? a : b))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> toUrl(e.getValue())));
    }

    private String toUrl(TourImage image) {
        return fileStorageService.publicUrl(image.getPublicId(), image.getFormat());
    }
}
