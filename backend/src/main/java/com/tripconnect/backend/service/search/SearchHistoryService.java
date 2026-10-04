package com.tripconnect.backend.service.search;

import com.tripconnect.backend.dto.LocationResponse;
import com.tripconnect.backend.dto.search.SearchResponses;
import com.tripconnect.backend.dto.search.TourSearchRequest;
import com.tripconnect.backend.entity.Location;
import com.tripconnect.backend.entity.SearchLog;
import com.tripconnect.backend.exception.ResourceNotFoundException;
import com.tripconnect.backend.repository.LocationRepository;
import com.tripconnect.backend.repository.SearchLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Lịch sử tìm kiếm: ghi lại các lần tìm (người đăng nhập hoặc khách ẩn danh), "Tìm kiếm gần đây",
 * gộp lịch sử khách vào tài khoản khi đăng nhập. Dữ liệu này cũng là đầu vào cho gợi ý tour.
 */
@Service
@RequiredArgsConstructor
public class SearchHistoryService {

    /** Mã khách do trình duyệt tự sinh (crypto.randomUUID) — chặn chuỗi lạ ghi vào DB. */
    private static final Pattern VISITOR_ID = Pattern.compile("^[0-9a-fA-F-]{36}$");
    private static final int RECENT_LIMIT = 8;
    /** Đọc nhiều hơn số mục hiển thị vì các lần tìm trùng nhau sẽ được gộp. */
    private static final int RECENT_SCAN = 60;

    private final SearchLogRepository searchLogRepository;
    private final LocationRepository locationRepository;

    public static boolean isValidVisitorId(String visitorId) {
        return visitorId != null && VISITOR_ID.matcher(visitorId).matches();
    }

    /**
     * Ghi một lần tìm. Nơi gọi phải bắt lỗi: ghi lịch sử hỏng KHÔNG được làm hỏng kết quả tìm kiếm.
     *
     * @param userId    null nếu chưa đăng nhập
     * @param visitorId mã khách (bỏ qua nếu không hợp lệ)
     */
    @Transactional
    public void record(TourSearchRequest request, Long userId, String visitorId, long resultCount) {
        if (!request.isTrack() || !request.hasCriteria() || request.getPage() > 0) return;
        String validVisitor = isValidVisitorId(visitorId) ? visitorId : null;
        if (userId == null && validVisitor == null) return;

        SearchLog entry = new SearchLog();
        entry.setUserId(userId);
        entry.setVisitorId(validVisitor);
        entry.setKeyword(request.getQ() == null || request.getQ().isBlank() ? null : request.getQ().trim());
        entry.setDestinationId(request.getDestinationId());
        entry.setDepartureLocationId(request.getDepartureLocationId());
        entry.setDateFrom(request.getDateFrom());
        entry.setDateTo(request.getDateTo());
        entry.setPriceMin(request.getPriceMin());
        entry.setPriceMax(request.getPriceMax());
        entry.setDurationMin(request.getDurationMin() == null ? null : request.getDurationMin().shortValue());
        entry.setDurationMax(request.getDurationMax() == null ? null : request.getDurationMax().shortValue());
        entry.setCategoryIds(request.getCategoryIds() == null ? new ArrayList<>() : new ArrayList<>(request.getCategoryIds()));
        entry.setInternational(request.getInternational());
        entry.setResultCount((int) Math.min(resultCount, Integer.MAX_VALUE));
        searchLogRepository.save(entry);
    }

    /** Gộp lịch sử lúc chưa đăng nhập vào tài khoản (gọi ngay sau khi đăng nhập; gọi lại nhiều lần không sao). */
    @Transactional
    public int claim(Long userId, String visitorId) {
        if (!isValidVisitorId(visitorId)) return 0;
        return searchLogRepository.claimVisitorLogs(visitorId, userId);
    }

    /** Các lần tìm gần đây có từ khóa hoặc điểm đến, gộp trùng, mới nhất trước. */
    @Transactional(readOnly = true)
    public List<SearchResponses.RecentSearch> recent(Long userId) {
        List<SearchLog> logs = searchLogRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, RECENT_SCAN));
        Map<String, SearchLog> unique = new LinkedHashMap<>();
        for (SearchLog entry : logs) {
            if (entry.getKeyword() == null && entry.getDestinationId() == null) continue;
            unique.putIfAbsent(SearchText.normalize(entry.getKeyword()) + "|" + entry.getDestinationId(), entry);
            if (unique.size() == RECENT_LIMIT) break;
        }

        Set<Long> locationIds = unique.values().stream().map(SearchLog::getDestinationId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Location> locations = locationRepository.findAllById(locationIds).stream()
                .collect(Collectors.toMap(Location::getId, Function.identity()));

        return unique.values().stream().map(entry -> {
            Location l = entry.getDestinationId() == null ? null : locations.get(entry.getDestinationId());
            return new SearchResponses.RecentSearch(entry.getId(), entry.getKeyword(),
                    l == null ? null : new LocationResponse(l.getId(), l.getCountry(), l.getProvince()),
                    entry.getDateFrom(), entry.getDateTo(), entry.getCreatedAt());
        }).toList();
    }

    /** Xóa một mục gần đây (mọi lần tìm cùng từ khóa + điểm đến). */
    @Transactional
    public void delete(Long userId, Long logId) {
        SearchLog entry = searchLogRepository.findByIdAndUserId(logId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mục tìm kiếm"));
        searchLogRepository.deleteSameSearch(userId, entry.getKeyword(), entry.getDestinationId());
    }

    @Transactional
    public void clear(Long userId) {
        searchLogRepository.deleteAllByUserId(userId);
    }
}
