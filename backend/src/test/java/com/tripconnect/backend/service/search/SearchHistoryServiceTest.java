package com.tripconnect.backend.service.search;

import com.tripconnect.backend.dto.search.TourSearchRequest;
import com.tripconnect.backend.entity.SearchLog;
import com.tripconnect.backend.repository.LocationRepository;
import com.tripconnect.backend.repository.SearchLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchHistoryServiceTest {

    private static final String VISITOR = "0f8c2a6e-3c1b-4d7a-9e55-1a2b3c4d5e6f";

    @Mock private SearchLogRepository searchLogRepository;
    @Mock private LocationRepository locationRepository;

    @InjectMocks private SearchHistoryService service;

    @Test
    void trackedSearchByVisitor_isRecorded() {
        TourSearchRequest request = request("Hạ Long");
        request.setCategoryIds(List.of(7L));

        service.record(request, null, VISITOR, 3);

        ArgumentCaptor<SearchLog> saved = ArgumentCaptor.forClass(SearchLog.class);
        verify(searchLogRepository).save(saved.capture());
        assertThat(saved.getValue().getVisitorId()).isEqualTo(VISITOR);
        assertThat(saved.getValue().getUserId()).isNull();
        assertThat(saved.getValue().getKeyword()).isEqualTo("Hạ Long");
        assertThat(saved.getValue().getCategoryIds()).containsExactly(7L);
        assertThat(saved.getValue().getResultCount()).isEqualTo(3);
    }

    @Test
    void untrackedOrEmptyOrNextPageSearch_isNotRecorded() {
        TourSearchRequest untracked = request("Hạ Long");
        untracked.setTrack(false);
        service.record(untracked, 1L, null, 1);

        service.record(request(null), 1L, null, 1); // không lọc gì = xem tất cả

        TourSearchRequest page2 = request("Hạ Long");
        page2.setPage(1);
        service.record(page2, 1L, null, 1);

        verify(searchLogRepository, never()).save(any());
    }

    @Test
    void invalidVisitorIdWithoutLogin_isIgnored() {
        service.record(request("Hạ Long"), null, "<script>", 1);
        verify(searchLogRepository, never()).save(any());
    }

    @Test
    void claim_movesVisitorHistoryToAccount_onlyForValidVisitorId() {
        when(searchLogRepository.claimVisitorLogs(VISITOR, 5L)).thenReturn(4);

        assertThat(service.claim(5L, VISITOR)).isEqualTo(4);
        assertThat(service.claim(5L, "abc")).isZero();
        verify(searchLogRepository, times(1)).claimVisitorLogs(any(), any());
    }

    private static TourSearchRequest request(String keyword) {
        TourSearchRequest request = new TourSearchRequest();
        request.setQ(keyword);
        request.setTrack(true);
        return request;
    }
}
