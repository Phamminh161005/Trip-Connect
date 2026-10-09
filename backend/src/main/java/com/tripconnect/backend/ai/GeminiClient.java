package com.tripconnect.backend.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Gemini API (REST v1beta). Chat dùng streamGenerateContent (SSE) để chữ hiện dần; model trong danh sách bị quá tải (503)
 * / hết hạn mức (429) thì thử model kế tiếp. Không ghi khóa API ra log.
 */
@Slf4j
@Component
public class GeminiClient implements LlmClient {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final int EMBED_BATCH = 100;
    /** Model vừa lỗi (quá tải / hết hạn mức) được xếp xuống cuối trong khoảng này, đỡ mất vài giây thử lại mỗi câu. */
    private static final Duration COOLDOWN = Duration.ofMinutes(2);

    private final String apiKey;
    private final String baseUrl;
    private final List<String> chatModels;
    private final String embeddingModel;
    private final int dimensions;
    private final String thinkingLevel;
    private final Duration timeout;
    private final HttpClient http;
    private final Map<String, Instant> coolingUntil = new java.util.concurrent.ConcurrentHashMap<>();

    public GeminiClient(@Value("${ai.gemini.api-key:}") String apiKey,
                        @Value("${ai.gemini.base-url}") String baseUrl,
                        @Value("${ai.gemini.chat-models}") List<String> chatModels,
                        @Value("${ai.gemini.embedding-model}") String embeddingModel,
                        @Value("${ai.gemini.embedding-dimensions}") int dimensions,
                        @Value("${ai.gemini.thinking-level:low}") String thinkingLevel,
                        @Value("${ai.gemini.timeout-seconds}") int timeoutSeconds) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.chatModels = chatModels.stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
        this.embeddingModel = embeddingModel;
        this.dimensions = dimensions;
        this.thinkingLevel = thinkingLevel;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public boolean isConfigured() {
        return !apiKey.isEmpty();
    }

    // ===================== Embedding =====================

    @Override
    public List<float[]> embed(List<String> texts, EmbedTask task) {
        requireConfigured();
        List<float[]> result = new ArrayList<>(texts.size());
        for (int from = 0; from < texts.size(); from += EMBED_BATCH) {
            List<String> batch = texts.subList(from, Math.min(texts.size(), from + EMBED_BATCH));
            ObjectNode body = JSON.createObjectNode();
            ArrayNode requests = body.putArray("requests");
            for (String text : batch) {
                ObjectNode r = requests.addObject();
                r.put("model", "models/" + embeddingModel);
                r.putObject("content").putArray("parts").addObject().put("text", text);
                r.put("taskType", task == EmbedTask.DOCUMENT ? "RETRIEVAL_DOCUMENT" : "RETRIEVAL_QUERY");
                r.put("outputDimensionality", dimensions);
            }
            HttpResponse<String> response = send(post("models/" + embeddingModel + ":batchEmbedContents", body),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw failure(response.statusCode(), response.body());
            for (JsonNode e : JSON.readTree(response.body()).path("embeddings")) {
                JsonNode values = e.path("values");
                float[] v = new float[values.size()];
                for (int i = 0; i < v.length; i++) v[i] = (float) values.get(i).asDouble();
                result.add(VectorMath.normalize(v));
            }
        }
        return result;
    }

    // ===================== Chat =====================

    @Override
    public ModelTurn chat(ChatRequest request, Consumer<String> onText) {
        requireConfigured();
        ObjectNode body = buildChatBody(request);
        AiUnavailableException last = null;
        java.util.concurrent.atomic.AtomicBoolean streamed = new java.util.concurrent.atomic.AtomicBoolean();
        Consumer<String> tracking = delta -> {
            streamed.set(true);
            onText.accept(delta);
        };
        Instant now = Instant.now();
        List<String> ordered = chatModels.stream()
                .sorted(Comparator.comparing(m -> coolingUntil.getOrDefault(m, Instant.EPOCH).isAfter(now)))
                .toList();
        for (String model : ordered) {
            try {
                ModelTurn turn = streamChat(model, body, tracking);
                coolingUntil.remove(model);
                return turn;
            } catch (AiUnavailableException e) {
                // Đã gửi một phần câu trả lời thì không đổi model giữa chừng (người dùng sẽ thấy chữ bị lặp)
                if (streamed.get()) throw e;
                coolingUntil.put(model, Instant.now().plus(COOLDOWN));
                last = e;
                log.info("Model {} không phục vụ được ({}), thử model kế tiếp", model, e.getMessage());
            }
        }
        throw last != null ? last : new AiUnavailableException("Chưa cấu hình model chat", false);
    }

    private ModelTurn streamChat(String model, ObjectNode body, Consumer<String> onText) {
        HttpResponse<InputStream> response = send(post("models/" + model + ":streamGenerateContent?alt=sse", body),
                HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            String error;
            try (InputStream in = response.body()) {
                error = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                error = "";
            }
            throw failure(response.statusCode(), error);
        }
        StringBuilder text = new StringBuilder();
        List<FunctionCall> calls = new ArrayList<>();
        ArrayNode rawParts = JSON.createArrayNode();
        try (InputStream in = response.body();
             Stream<String> lines = new java.io.BufferedReader(new java.io.InputStreamReader(in, StandardCharsets.UTF_8)).lines()) {
            lines.filter(line -> line.startsWith("data:")).forEach(line -> {
                JsonNode chunk = JSON.readTree(line.substring(5).trim());
                for (JsonNode part : chunk.path("candidates").path(0).path("content").path("parts")) {
                    rawParts.add(part);
                    if (part.has("functionCall")) {
                        JsonNode fc = part.get("functionCall");
                        @SuppressWarnings("unchecked")
                        Map<String, Object> args = fc.has("args") ? JSON.convertValue(fc.get("args"), Map.class) : Map.of();
                        calls.add(new FunctionCall(fc.path("id").asString(null), fc.path("name").asString(""), args));
                    } else if (part.has("text") && !part.path("thought").asBoolean(false)) {
                        String delta = part.get("text").asString("");
                        text.append(delta);
                        if (!delta.isEmpty()) onText.accept(delta);
                    }
                }
            });
        } catch (IOException | java.io.UncheckedIOException e) {
            throw new AiUnavailableException("Mất kết nối khi nhận câu trả lời: " + e.getMessage(), false);
        }
        return new ModelTurn(text.toString(), calls, rawParts);
    }

    private ObjectNode buildChatBody(ChatRequest request) {
        ObjectNode body = JSON.createObjectNode();
        if (request.systemInstruction() != null) {
            body.putObject("systemInstruction").putArray("parts").addObject().put("text", request.systemInstruction());
        }
        ArrayNode contents = body.putArray("contents");
        for (Turn turn : request.turns()) {
            if (turn instanceof UserTurn u) {
                ObjectNode c = contents.addObject().put("role", "user");
                c.putArray("parts").addObject().put("text", u.text());
            } else if (turn instanceof ModelTurn m) {
                ObjectNode c = contents.addObject().put("role", "model");
                if (m.raw() instanceof ArrayNode raw && !raw.isEmpty()) {
                    c.set("parts", raw);
                } else {
                    c.putArray("parts").addObject().put("text", m.text() == null ? "" : m.text());
                }
            } else if (turn instanceof ToolResultsTurn t) {
                ObjectNode c = contents.addObject().put("role", "user");
                ArrayNode parts = c.putArray("parts");
                for (FunctionResult r : t.results()) {
                    ObjectNode fr = parts.addObject().putObject("functionResponse");
                    if (r.id() != null) fr.put("id", r.id());
                    fr.put("name", r.name());
                    fr.putObject("response").set("result", JSON.valueToTree(r.response()));
                }
            }
        }
        if (request.tools() != null && !request.tools().isEmpty()) {
            ArrayNode declarations = body.putArray("tools").addObject().putArray("functionDeclarations");
            for (Tool tool : request.tools()) {
                ObjectNode d = declarations.addObject();
                d.put("name", tool.name());
                d.put("description", tool.description());
                d.set("parameters", JSON.valueToTree(tool.parameters()));
            }
        }
        ObjectNode config = body.putObject("generationConfig").put("temperature", 0.4).put("maxOutputTokens", 2048);
        // Mức suy luận cao làm câu trả lời chậm hơn hẳn (hàng chục giây) mà trợ lý tra cứu không cần
        if (thinkingLevel != null && !thinkingLevel.isBlank()) {
            config.putObject("thinkingConfig").put("thinkingLevel", thinkingLevel.trim());
        }
        return body;
    }

    // ===================== HTTP =====================

    private HttpRequest post(String path, JsonNode body) {
        return HttpRequest.newBuilder(URI.create(baseUrl + "/" + path))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();
    }

    private <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler) {
        try {
            return http.send(request, handler);
        } catch (IOException e) {
            throw new AiUnavailableException("Không kết nối được tới Gemini: " + e.getMessage(), false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiUnavailableException("Bị ngắt khi gọi Gemini", false);
        }
    }

    private static AiUnavailableException failure(int status, String body) {
        String message = body;
        try {
            message = JSON.readTree(body).path("error").path("message").asString(body);
        } catch (RuntimeException ignored) {
            // Không phải JSON -> giữ nguyên
        }
        if (message != null && message.length() > 300) message = message.substring(0, 300);
        return new AiUnavailableException("Gemini trả " + status + ": " + message, status == 429);
    }

    private void requireConfigured() {
        if (!isConfigured()) throw new AiUnavailableException("Chưa cấu hình GEMINI_API_KEY", false);
    }
}
