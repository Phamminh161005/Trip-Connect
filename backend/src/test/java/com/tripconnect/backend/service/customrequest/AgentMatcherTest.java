package com.tripconnect.backend.service.customrequest;

import com.tripconnect.backend.entity.Location;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AgentMatcherTest {

    private static Location location(long id, String country, String province) {
        Location l = new Location();
        l.setId(id);
        l.setCountry(country);
        l.setProvince(province);
        return l;
    }

    private static final Location HA_NOI = location(1, "Việt Nam", "Thành phố Hà Nội");
    private static final Location LAO_CAI = location(6, "Việt Nam", "Tỉnh Lào Cai");
    private static final Location DA_NANG = location(32, "Việt Nam", "Thành phố Đà Nẵng");
    private static final Location JAPAN = location(171, "Nhật Bản", null);
    private static final Location KOREA = location(233, "Hàn Quốc", null);

    @Test
    void sameLocationScores100_sameCountry60_elsewhere0_averagedOverDestinations() {
        assertThat(AgentMatcher.locationScore(List.of(LAO_CAI), List.of(LAO_CAI, HA_NOI))).isEqualTo(100);
        assertThat(AgentMatcher.locationScore(List.of(DA_NANG), List.of(HA_NOI))).isEqualTo(60);
        assertThat(AgentMatcher.locationScore(List.of(JAPAN), List.of(HA_NOI))).isZero();
        // Lào Cai (100) + Nhật Bản (0) -> trung bình 50
        assertThat(AgentMatcher.locationScore(List.of(LAO_CAI, JAPAN), List.of(LAO_CAI))).isEqualTo(50);
        assertThat(AgentMatcher.locationScore(List.of(JAPAN, KOREA), List.of(JAPAN, KOREA))).isEqualTo(100);
    }

    @Test
    void ratingIsNormalized_andNewAgentsGetNeutralScore() {
        assertThat(AgentMatcher.ratingScore(new BigDecimal("5"))).isEqualTo(100);
        assertThat(AgentMatcher.ratingScore(new BigDecimal("1"))).isZero();
        assertThat(AgentMatcher.ratingScore(new BigDecimal("4.2"))).isEqualTo(80, org.assertj.core.data.Offset.offset(0.001));
        assertThat(AgentMatcher.ratingScore(null)).isEqualTo(60);
    }

    @Test
    void totalIsSixtyPercentLocationFortyPercentRating() {
        assertThat(AgentMatcher.total(100, 50)).isEqualByComparingTo("80.0");
        assertThat(AgentMatcher.total(60, 80)).isEqualByComparingTo("68.0");
    }
}
