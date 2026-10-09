package com.tripconnect.backend.ai.assistant;

import com.tripconnect.backend.ai.LlmClient;
import com.tripconnect.backend.ai.rag.TourCandidates;
import com.tripconnect.backend.ai.rag.VectorIndex;
import com.tripconnect.backend.dto.PageResponse;
import com.tripconnect.backend.dto.assistant.AssistantDtos;
import com.tripconnect.backend.dto.search.SearchResponses;
import com.tripconnect.backend.dto.search.TourSearchRequest;
import com.tripconnect.backend.dto.tour.TourResponses;
import com.tripconnect.backend.entity.Location;
import com.tripconnect.backend.entity.TourCategory;
import com.tripconnect.backend.exception.ResourceNotFoundException;
import com.tripconnect.backend.service.customrequest.CustomRequestRules;
import com.tripconnect.backend.service.search.TourSearchService;
import com.tripconnect.backend.service.tour.PublicTourService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Các hàm mô hình được gọi để lấy dữ liệu thật của TripConnect.
 * Mô hình chỉ thấy dữ liệu công khai (tour đang bán, chính sách) — không có thông tin cá nhân nào.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssistantTools {

    static final String SEARCH_TOURS = "search_tours";
    static final String SEMANTIC_SEARCH_TOURS = "semantic_search_tours";
    static final String GET_TOUR = "get_tour";
    static final String SEARCH_KNOWLEDGE = "search_knowledge";
    static final String SUGGEST_CUSTOM_REQUEST = "suggest_custom_request";

    /** Số tour trả về cho mô hình mỗi lần tìm. */
    static final int TOURS_PER_RESULT = 6;
    /** Số tour ứng viên (đã qua bộ lọc) đem xếp hạng theo độ giống nghĩa. */
    static final int SEMANTIC_CANDIDATES = 100;
    static final int KNOWLEDGE_HITS = 4;
    static final int DEPARTURES_SHOWN = 8;
    static final int ITINERARY_TEXT_MAX = 400;

    private final TourSearchService searchService;
    private final PublicTourService publicTourService;
    private final TourCandidates candidates;
    private final LlmClient llm;
    private final VectorIndex vectorIndex;
    private final PlaceMatcher places;

    /** Những gì các hàm thu được trong một lượt trả lời (thẻ tour để hiển thị, bản nháp yêu cầu). */
    public static final class Context {
        final Map<Long, SearchResponses.TourCard> tours = new LinkedHashMap<>();
        AssistantDtos.CustomRequestDraft customRequest;
    }

    // ===================== Khai báo =====================

    public List<LlmClient.Tool> declarations() {
        Map<String, Object> filters = filterProperties();

        Map<String, Object> searchProps = new LinkedHashMap<>();
        searchProps.put("keyword", prop("STRING", "Từ khóa có trong tên / nội dung tour, vd \"Hạ Long du thuyền\". Bỏ trống nếu đã có điểm đến."));
        searchProps.putAll(filters);
        searchProps.put("sort", Map.of("type", "STRING",
                "enum", List.of("RECOMMENDED", "PRICE_ASC", "PRICE_DESC", "DEPARTURE_SOON", "RATING"),
                "description", "Thứ tự: RECOMMENDED (mặc định), PRICE_ASC rẻ trước, PRICE_DESC, DEPARTURE_SOON khởi hành sớm, RATING đánh giá cao"));

        Map<String, Object> semanticProps = new LinkedHashMap<>();
        semanticProps.put("description", prop("STRING",
                "Mô tả chuyến đi khách mong muốn bằng lời, vd \"nghỉ dưỡng yên tĩnh gần biển cho gia đình có trẻ nhỏ\""));
        semanticProps.putAll(filters);

        return List.of(
                new LlmClient.Tool(SEARCH_TOURS,
                        "Tìm tour ghép đoàn đang bán theo bộ lọc rõ ràng (điểm đến, ngày, giá, số ngày, loại hình). "
                                + "Trả về các tour còn lịch khởi hành phù hợp kèm giá thấp nhất.",
                        object(searchProps, List.of())),
                new LlmClient.Tool(SEMANTIC_SEARCH_TOURS,
                        "Tìm tour theo nhu cầu / sở thích mô tả bằng lời (khi khách không nêu rõ điểm đến, "
                                + "hoặc mô tả kiểu chuyến đi). Có thể kèm bộ lọc. Kết quả xếp theo độ phù hợp với mô tả.",
                        object(semanticProps, List.of("description"))),
                new LlmClient.Tool(GET_TOUR,
                        "Xem chi tiết một tour: lịch trình từng ngày, dịch vụ bao gồm / không bao gồm, "
                                + "các lịch khởi hành còn bán với giá và số chỗ.",
                        object(Map.of("tourId", prop("INTEGER", "Mã tour (lấy từ kết quả tìm kiếm)")), List.of("tourId"))),
                new LlmClient.Tool(SEARCH_KNOWLEDGE,
                        "Tra cứu quy định và hướng dẫn của TripConnect: cách đặt tour, thanh toán, hủy và hoàn tiền, "
                                + "tour thiết kế riêng, đánh giá, tài khoản, đối tác. Luôn dùng hàm này trước khi trả lời câu hỏi về quy định.",
                        object(Map.of("question", prop("STRING", "Câu hỏi cần tra, viết đầy đủ ý")), List.of("question"))),
                new LlmClient.Tool(SUGGEST_CUSTOM_REQUEST,
                        "Hiển thị nút để khách gửi yêu cầu thiết kế tour riêng, điền sẵn thông tin đã biết. "
                                + "Dùng khi không có tour ghép đoàn phù hợp hoặc khách muốn đi riêng / lịch trình theo ý mình.",
                        object(customRequestProperties(), List.of("destinations"))));
    }

    private Map<String, Object> filterProperties() {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("destination", prop("STRING",
                "Tên tỉnh / thành phố Việt Nam hoặc tên quốc gia (tiếng Việt). Với địa danh hãy dùng tỉnh: "
                        + "Hạ Long → Quảng Ninh, Đà Lạt → Lâm Đồng, Sa Pa → Lào Cai, Phú Quốc → Kiên Giang, Hội An → Quảng Nam"));
        p.put("departureFrom", prop("STRING", "Nơi khởi hành (tỉnh / thành phố)"));
        p.put("dateFrom", prop("STRING", "Khởi hành từ ngày (yyyy-MM-dd)"));
        p.put("dateTo", prop("STRING", "Khởi hành đến ngày (yyyy-MM-dd)"));
        p.put("priceMin", prop("INTEGER", "Giá người lớn thấp nhất (VNĐ)"));
        p.put("priceMax", prop("INTEGER", "Giá người lớn cao nhất (VNĐ)"));
        p.put("durationMin", prop("INTEGER", "Số ngày ít nhất"));
        p.put("durationMax", prop("INTEGER", "Số ngày nhiều nhất"));
        p.put("categories", Map.of("type", "ARRAY", "items", Map.of("type", "STRING"),
                "description", "Loại hình tour, chọn trong: " + String.join(", ", places.categoryNames())));
        p.put("international", prop("BOOLEAN", "true = tour nước ngoài, false = trong nước, bỏ trống = cả hai"));
        return p;
    }

    private static Map<String, Object> customRequestProperties() {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("destinations", Map.of("type", "ARRAY", "items", Map.of("type", "STRING"),
                "description", "Các điểm đến (tỉnh / thành phố hoặc quốc gia)"));
        p.put("departureFrom", prop("STRING", "Nơi khởi hành"));
        p.put("earliestStart", prop("STRING", "Ngày sớm nhất có thể khởi hành (yyyy-MM-dd)"));
        p.put("latestStart", prop("STRING", "Ngày muộn nhất có thể khởi hành (yyyy-MM-dd)"));
        p.put("durationDays", prop("INTEGER", "Số ngày của chuyến đi"));
        p.put("adults", prop("INTEGER", "Số người lớn"));
        p.put("children", prop("INTEGER", "Số trẻ em (2-11 tuổi)"));
        p.put("infants", prop("INTEGER", "Số trẻ sơ sinh (dưới 2 tuổi)"));
        p.put("budgetMax", prop("INTEGER", "Ngân sách tối đa cho cả đoàn (VNĐ)"));
        p.put("notes", prop("STRING", "Mong muốn khác của khách, viết ngắn gọn"));
        return p;
    }

    private static Map<String, Object> prop(String type, String description) {
        return Map.of("type", type, "description", description);
    }

    private static Map<String, Object> object(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "OBJECT");
        schema.put("properties", properties);
        if (!required.isEmpty()) schema.put("required", required);
        return schema;
    }

    // ===================== Thực thi =====================

    /** Chạy một lời gọi hàm. Lỗi dữ liệu không ném ra ngoài mà trả về cho mô hình để nó tự điều chỉnh. */
    public Object execute(LlmClient.FunctionCall call, Context ctx) {
        Args args = new Args(call.args());
        try {
            return switch (call.name()) {
                case SEARCH_TOURS -> searchTours(args, ctx);
                case SEMANTIC_SEARCH_TOURS -> semanticSearch(args, ctx);
                case GET_TOUR -> getTour(args, ctx);
                case SEARCH_KNOWLEDGE -> searchKnowledge(args);
                case SUGGEST_CUSTOM_REQUEST -> suggestCustomRequest(args, ctx);
                default -> Map.of("error", "Không có hàm " + call.name());
            };
        } catch (ResourceNotFoundException e) {
            return Map.of("error", "Không tìm thấy tour này hoặc tour không còn bán");
        } catch (IllegalArgumentException e) {
            return Map.of("error", e.getMessage());
        }
    }

    private Object searchTours(Args args, Context ctx) {
        List<String> notes = new ArrayList<>();
        TourSearchRequest request = filters(args, notes);
        request.setQ(args.text("keyword", 200));
        request.setSort(args.enumValue("sort", TourSearchRequest.Sort.class, TourSearchRequest.Sort.RECOMMENDED));
        request.setSize(TOURS_PER_RESULT);
        PageResponse<SearchResponses.TourCard> page = searchService.search(request);
        page.content().forEach(c -> ctx.tours.put(c.id(), c));
        return tourResult(page.totalElements(), page.content(), notes);
    }

    private Object semanticSearch(Args args, Context ctx) {
        String description = args.text("description", 500);
        if (description == null) throw new IllegalArgumentException("Thiếu mô tả chuyến đi");
        List<String> notes = new ArrayList<>();
        TourCandidates.Result found = candidates.find(filters(args, notes), SEMANTIC_CANDIDATES);
        List<SearchResponses.TourCard> pool = found.cards();
        long total = found.total();
        if (pool.isEmpty()) return tourResult(0, List.of(), notes);

        Map<Long, SearchResponses.TourCard> byId = pool.stream()
                .collect(Collectors.toMap(SearchResponses.TourCard::id, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        List<SearchResponses.TourCard> ranked;
        if (vectorIndex.tourCount() == 0) {
            // Chưa có véc-tơ (mô hình embedding lỗi lúc khởi động): giữ thứ tự gợi ý mặc định
            ranked = pool.stream().limit(TOURS_PER_RESULT).toList();
        } else {
            float[] query = llm.embed(List.of(description), LlmClient.EmbedTask.QUERY).get(0);
            ranked = vectorIndex.nearestTours(query, byId.keySet(), TOURS_PER_RESULT, 0).stream()
                    .map(hit -> byId.get(hit.item()))
                    .toList();
        }
        ranked.forEach(c -> ctx.tours.put(c.id(), c));
        return tourResult(total, ranked, notes);
    }

    private Object getTour(Args args, Context ctx) {
        Long id = args.longValue("tourId");
        if (id == null) throw new IllegalArgumentException("Thiếu mã tour");
        TourResponses.Detail t = publicTourService.get(id);
        ctx.tours.put(t.id(), toCard(t));

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", t.id());
        r.put("title", t.title());
        r.put("organizer", t.provider() == null ? "TripConnect" : t.provider().companyName());
        r.put("categories", t.categories().stream().map(c -> c.getName()).toList());
        r.put("departureFrom", place(t.departureLocation().getProvince(), t.departureLocation().getCountry()));
        r.put("destinations", t.destinations().stream().map(l -> place(l.getProvince(), l.getCountry())).toList());
        r.put("duration", t.durationDays() + " ngày " + t.durationNights() + " đêm");
        r.put("highlights", t.highlights());
        r.put("itinerary", t.itinerary().stream().map(d -> Map.of(
                "day", d.dayNumber(),
                "title", d.title(),
                "description", truncate(d.description(), ITINERARY_TEXT_MAX))).toList());
        r.put("transport", t.transportModes().stream().map(m -> m.label()).toList());
        if (t.accommodationType() != null) r.put("accommodation", t.accommodationType().label());
        if (t.meetingPoint() != null) r.put("meetingPoint", t.meetingPoint());
        r.put("included", t.includedServices());
        r.put("excluded", t.excludedServices());
        if (t.notes() != null) r.put("notes", truncate(t.notes(), 800));
        r.put("rating", t.ratingCount() == 0 ? "chưa có đánh giá" : t.rating() + "/5 (" + t.ratingCount() + " đánh giá)");
        List<TourResponses.Departure> open = t.departures().stream().filter(TourResponses.Departure::bookable).toList();
        r.put("departures", open.stream().limit(DEPARTURES_SHOWN).map(d -> Map.of(
                "startDate", d.startDate().toString(),
                "seatsLeft", d.seatsAvailable(),
                "adultPrice", d.adultPrice(),
                "childPrice", d.childPrice())).toList());
        if (open.size() > DEPARTURES_SHOWN) r.put("moreDepartures", open.size() - DEPARTURES_SHOWN);
        if (open.isEmpty()) r.put("note", "Tour hiện không còn lịch khởi hành nào đặt được");
        return r;
    }

    private Object searchKnowledge(Args args) {
        String question = args.text("question", 500);
        if (question == null) throw new IllegalArgumentException("Thiếu câu hỏi");
        if (vectorIndex.knowledgeCount() == 0) {
            return Map.of("error", "Kho quy định tạm thời chưa sẵn sàng");
        }
        float[] query = llm.embed(List.of(question), LlmClient.EmbedTask.QUERY).get(0);
        return Map.of("passages", vectorIndex.nearestKnowledge(query, KNOWLEDGE_HITS, 0).stream()
                .map(hit -> Map.of("title", hit.item().title(), "content", hit.item().content()))
                .toList());
    }

    private Object suggestCustomRequest(Args args, Context ctx) {
        List<String> notes = new ArrayList<>();
        List<Location> destinations = new ArrayList<>();
        for (String name : args.textList("destinations")) {
            Optional<Location> l = places.location(name);
            if (l.isPresent() && destinations.stream().noneMatch(d -> d.getId().equals(l.get().getId()))) {
                destinations.add(l.get());
            } else if (l.isEmpty()) {
                notes.add("Không có điểm đến \"" + name + "\" trong hệ thống, khách sẽ tự chọn trên form");
            }
        }
        Location departure = Optional.ofNullable(args.text("departureFrom", 100)).flatMap(places::location).orElse(null);
        Integer duration = args.intValue("durationDays");
        if (duration != null && (duration < 1 || duration > CustomRequestRules.MAX_DURATION_DAYS)) duration = null;

        ctx.customRequest = new AssistantDtos.CustomRequestDraft(
                departure == null ? null : departure.getId(),
                departure == null ? null : PlaceMatcher.label(departure),
                destinations.stream().limit(CustomRequestRules.MAX_DESTINATIONS).map(Location::getId).toList(),
                destinations.stream().limit(CustomRequestRules.MAX_DESTINATIONS).map(PlaceMatcher::label).toList(),
                args.date("earliestStart"),
                args.date("latestStart"),
                duration,
                nonNegative(args.intValue("adults")),
                nonNegative(args.intValue("children")),
                nonNegative(args.intValue("infants")),
                Optional.ofNullable(args.longValue("budgetMax")).filter(b -> b > 0).orElse(null),
                args.text("notes", 2000));

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("shown", true);
        r.put("info", "Đã hiện nút \"Gửi yêu cầu tour riêng\" mở form điền sẵn; khách kiểm tra, bổ sung rồi gửi (cần đăng nhập). "
                + "Sau khi gửi, TripConnect chọn một đơn vị tổ chức phù hợp; đơn vị đó gửi đề xuất lịch trình và báo giá "
                + "để khách đồng ý hoặc yêu cầu chỉnh sửa, và trao đổi với khách qua khung chat trong trang yêu cầu. "
                + "Ngày khởi hành sớm nhất phải cách hôm nay ít nhất " + CustomRequestRules.MIN_LEAD_DAYS + " ngày.");
        if (!notes.isEmpty()) r.put("notes", notes);
        return r;
    }

    // ===================== Hỗ trợ =====================

    /** Bộ lọc chung của hai hàm tìm tour. Tên không khớp thì ghi chú cho mô hình thay vì báo lỗi. */
    private TourSearchRequest filters(Args args, List<String> notes) {
        TourSearchRequest r = new TourSearchRequest();
        String destination = args.text("destination", 100);
        if (destination != null) {
            Optional<Location> l = places.location(destination);
            if (l.isPresent()) {
                r.setDestinationId(l.get().getId());
            } else {
                notes.add("Không có điểm đến \"" + destination + "\" trong hệ thống, đã bỏ qua bộ lọc này");
            }
        }
        String departure = args.text("departureFrom", 100);
        if (departure != null) {
            Optional<Location> l = places.location(departure);
            if (l.isPresent()) r.setDepartureLocationId(l.get().getId());
            else notes.add("Không có nơi khởi hành \"" + departure + "\", đã bỏ qua bộ lọc này");
        }
        r.setDateFrom(args.date("dateFrom"));
        r.setDateTo(args.date("dateTo"));
        if (r.getDateFrom() != null && r.getDateTo() != null && r.getDateFrom().isAfter(r.getDateTo())) {
            r.setDateTo(null);
        }
        r.setPriceMin(nonNegativeLong(args.longValue("priceMin")));
        r.setPriceMax(nonNegativeLong(args.longValue("priceMax")));
        if (r.getPriceMin() != null && r.getPriceMax() != null && r.getPriceMin() > r.getPriceMax()) r.setPriceMin(null);
        r.setDurationMin(clampDays(args.intValue("durationMin")));
        r.setDurationMax(clampDays(args.intValue("durationMax")));
        List<Long> categoryIds = new ArrayList<>();
        for (String name : args.textList("categories")) {
            places.category(name).map(TourCategory::getId).ifPresentOrElse(categoryIds::add,
                    () -> notes.add("Không có loại hình \"" + name + "\""));
        }
        if (!categoryIds.isEmpty()) r.setCategoryIds(categoryIds);
        r.setInternational(args.bool("international"));
        return r;
    }

    private static Map<String, Object> tourResult(long total, Collection<SearchResponses.TourCard> cards, List<String> notes) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("totalMatches", total);
        r.put("tours", cards.stream().map(AssistantTools::compact).toList());
        if (!notes.isEmpty()) r.put("notes", notes);
        return r;
    }

    private static Map<String, Object> compact(SearchResponses.TourCard c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.id());
        m.put("title", c.title());
        m.put("duration", c.durationDays() + " ngày " + c.durationNights() + " đêm");
        m.put("departureFrom", c.departureLocation());
        m.put("destinations", c.destinations());
        m.put("fromPrice", c.minPrice());
        if (c.nextDepartureDate() != null) m.put("nextDeparture", c.nextDepartureDate().toString());
        m.put("departureCount", c.departureCount());
        m.put("rating", c.ratingCount() == 0 ? "chưa có" : c.rating() + "/5 (" + c.ratingCount() + ")");
        m.put("organizer", c.providerName());
        m.put("highlights", c.highlights());
        return m;
    }

    /** Thẻ tour dựng từ trang chi tiết (giá / ngày tính trên các lịch còn đặt được). */
    private static SearchResponses.TourCard toCard(TourResponses.Detail t) {
        List<TourResponses.Departure> open = t.departures().stream().filter(TourResponses.Departure::bookable)
                .sorted(Comparator.comparing(TourResponses.Departure::startDate)).toList();
        return new SearchResponses.TourCard(
                t.id(), t.title(),
                t.images().stream().min(Comparator.comparingInt(TourResponses.Image::sortOrder)).map(TourResponses.Image::url).orElse(null),
                t.durationDays(), t.durationNights(),
                t.departureLocation().getProvince(),
                t.destinations().stream().map(l -> place(l.getProvince(), l.getCountry())).sorted().toList(),
                t.international(),
                t.highlights().stream().limit(2).toList(),
                t.rating(), t.ratingCount(),
                t.provider() == null ? "TripConnect" : t.provider().companyName(),
                open.stream().mapToLong(TourResponses.Departure::adultPrice).min().orElse(0),
                open.isEmpty() ? null : open.get(0).startDate(),
                open.size());
    }

    private static String place(String province, String country) {
        return province != null ? province : country;
    }

    private static String truncate(String s, int max) {
        if (s == null || s.length() <= max) return s;
        return s.substring(0, max) + "…";
    }

    private static Integer clampDays(Integer days) {
        return days == null ? null : Math.max(1, Math.min(CustomRequestRules.MAX_DURATION_DAYS, days));
    }

    private static Integer nonNegative(Integer n) {
        return n == null || n < 0 ? null : n;
    }

    private static Long nonNegativeLong(Long n) {
        return n == null || n < 0 ? null : n;
    }

    /** Đọc tham số mô hình gửi lên (kiểu dữ liệu có thể lệch: số dạng chuỗi, số thực...). */
    static final class Args {
        private final Map<String, Object> values;

        Args(Map<String, Object> values) {
            this.values = values == null ? Map.of() : values;
        }

        String text(String key, int maxLength) {
            Object v = values.get(key);
            if (v == null) return null;
            String s = v.toString().strip();
            if (s.isEmpty()) return null;
            return s.length() > maxLength ? s.substring(0, maxLength) : s;
        }

        List<String> textList(String key) {
            Object v = values.get(key);
            if (v instanceof Collection<?> list) {
                return list.stream().filter(o -> o != null && !o.toString().isBlank())
                        .map(o -> o.toString().strip()).limit(20).toList();
            }
            String single = text(key, 100);
            return single == null ? List.of() : List.of(single);
        }

        Long longValue(String key) {
            Object v = values.get(key);
            if (v instanceof Number n) return n.longValue();
            if (v instanceof String s) {
                String t = s.strip();
                // "5.000" là năm nghìn (cách viết Việt Nam), "2.5" là số thập phân
                if (t.matches("\\d+(\\.\\d+)?") && !t.matches("\\d{1,3}(\\.\\d{3})+")) return (long) Double.parseDouble(t);
                // "5.000.000", "5,000,000 VNĐ" -> chỉ giữ chữ số
                String digits = t.replaceAll("\\D", "");
                return digits.isEmpty() || digits.length() > 15 ? null : Long.parseLong(digits);
            }
            return null;
        }

        Integer intValue(String key) {
            Long v = longValue(key);
            return v == null ? null : (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, v));
        }

        Boolean bool(String key) {
            Object v = values.get(key);
            if (v instanceof Boolean b) return b;
            if (v instanceof String s && !s.isBlank()) return Boolean.parseBoolean(s.strip());
            return null;
        }

        LocalDate date(String key) {
            String s = text(key, 10);
            if (s == null) return null;
            try {
                return LocalDate.parse(s);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Ngày \"" + s + "\" không đúng định dạng yyyy-MM-dd");
            }
        }

        <E extends Enum<E>> E enumValue(String key, Class<E> type, E fallback) {
            String s = text(key, 40);
            if (s == null) return fallback;
            try {
                return Enum.valueOf(type, s.toUpperCase());
            } catch (IllegalArgumentException e) {
                return fallback;
            }
        }
    }
}
