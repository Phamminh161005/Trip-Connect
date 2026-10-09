package com.tripconnect.backend.ai;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Mô hình ngôn ngữ dùng cho trợ lý: trò chuyện có gọi hàm (function calling) và tạo véc-tơ (embedding).
 * Phần còn lại của hệ thống chỉ biết lớp này — đổi nhà cung cấp chỉ cần viết bản cài đặt khác.
 */
public interface LlmClient {

    /** Có khóa API — không có thì trợ lý tắt. */
    boolean isConfigured();

    /** Véc-tơ cho văn bản được lưu để tìm (DOCUMENT) hay cho câu hỏi đem đi tìm (QUERY). */
    enum EmbedTask { DOCUMENT, QUERY }

    /** Véc-tơ đã chuẩn hóa độ dài 1 (tích vô hướng = độ giống cosine). */
    List<float[]> embed(List<String> texts, EmbedTask task);

    /** Hàm cho mô hình gọi. parameters: lược đồ tham số dạng JSON Schema (type viết hoa: OBJECT, STRING...). */
    record Tool(String name, String description, Map<String, Object> parameters) {
    }

    record FunctionCall(String id, String name, Map<String, Object> args) {
    }

    record FunctionResult(String id, String name, Object response) {
    }

    /** Một lượt trong cuộc hội thoại gửi lên mô hình. */
    sealed interface Turn permits UserTurn, ModelTurn, ToolResultsTurn {
    }

    record UserTurn(String text) implements Turn {
    }

    /**
     * Lượt trả lời của mô hình.
     *
     * @param raw nội dung gốc của nhà cung cấp — phải gửi lại nguyên văn ở lượt sau (Gemini 3 kèm chữ ký suy luận)
     */
    record ModelTurn(String text, List<FunctionCall> calls, Object raw) implements Turn {
        public boolean wantsTools() {
            return !calls.isEmpty();
        }
    }

    record ToolResultsTurn(List<FunctionResult> results) implements Turn {
    }

    record ChatRequest(String systemInstruction, List<Turn> turns, List<Tool> tools) {
    }

    /**
     * Gửi hội thoại, nhận lượt trả lời (chữ đến dần qua onText; lời gọi hàm trả về trong ModelTurn).
     *
     * @throws AiUnavailableException mô hình quá tải / hết hạn mức / lỗi mạng
     */
    ModelTurn chat(ChatRequest request, Consumer<String> onText);
}
