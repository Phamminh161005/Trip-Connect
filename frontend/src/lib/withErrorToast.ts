import { toast } from "sonner";
import { errorMessage } from "@/lib/api/errors";

/**
 * Cho onConfirm của ConfirmDialog / ReasonDialog: lỗi thì hiện toast rồi ném lại để hộp thoại giữ nguyên
 * (người dùng đọc lỗi, sửa và thử lại).
 */
export async function withErrorToast<T>(task: () => Promise<T>): Promise<T> {
  try {
    return await task();
  } catch (error) {
    toast.error(errorMessage(error));
    throw error;
  }
}
