import { toast } from "sonner";
import { getAgentDocumentUrl } from "@/lib/api/agent";
import { errorMessage } from "@/lib/api/errors";
import type { AgentDocumentType } from "@/types/agent";

// Khớp FileRule.DOCUMENT của Backend (Backend vẫn kiểm tra lại bằng nội dung file thật)
export const ACCEPTED_DOCUMENT_TYPES = ["application/pdf", "image/jpeg", "image/png"];
export const MAX_DOCUMENT_BYTES = 10 * 1024 * 1024;
export const MAX_OTHER_DOCUMENTS = 5;

export const REQUIRED_DOCUMENTS: { type: Exclude<AgentDocumentType, "OTHER">; label: string; hint: string }[] = [
  { type: "TRAVEL_LICENSE", label: "Giấy phép kinh doanh lữ hành", hint: "Do Sở Du lịch hoặc Cục Du lịch Quốc gia cấp" },
  { type: "BUSINESS_REGISTRATION", label: "Giấy chứng nhận đăng ký kinh doanh", hint: "Có ghi mã số thuế và địa chỉ trụ sở" },
  { type: "REPRESENTATIVE_ID_FRONT", label: "CCCD người đại diện (mặt trước)", hint: "Ảnh rõ nét, đủ 4 góc" },
  { type: "REPRESENTATIVE_ID_BACK", label: "CCCD người đại diện (mặt sau)", hint: "Ảnh rõ nét, đủ 4 góc" },
];

/** Kiểm tra nhanh trên trình duyệt để báo lỗi ngay, không phải chờ tải lên xong mới bị từ chối. */
export function checkDocumentFile(file: File): string | null {
  if (!ACCEPTED_DOCUMENT_TYPES.includes(file.type)) return "Chỉ chấp nhận file PDF, JPG hoặc PNG";
  if (file.size > MAX_DOCUMENT_BYTES) return "File vượt quá 10 MB";
  return null;
}

/**
 * Mở file private ở tab mới bằng link tạm thời (hết hạn sau 5 phút).
 * `getUrl`: hàm xin link (API của Agent hoặc của Admin).
 */
export async function openTemporaryDocument(getUrl: () => Promise<{ url: string }>) {
  // Mở tab NGAY trong lúc bấm (nếu mở sau khi chờ API, trình duyệt sẽ chặn popup)
  const tab = window.open("about:blank", "_blank");
  if (tab) tab.opener = null;
  try {
    const { url } = await getUrl();
    if (tab) tab.location.href = url;
    else window.location.href = url;
  } catch (error) {
    tab?.close();
    toast.error(errorMessage(error));
  }
}

/** Agent xem giấy tờ của chính mình. */
export const openAgentDocument = (documentId: number) => openTemporaryDocument(() => getAgentDocumentUrl(documentId));
