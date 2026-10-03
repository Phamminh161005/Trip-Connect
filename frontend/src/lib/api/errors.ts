/**
 * Lỗi thống nhất cho mọi lần gọi API. Đọc đúng 2 dạng lỗi của backend:
 *  - { message }        -> lỗi chung (hiện toast)
 *  - { fieldErrors }    -> lỗi theo từng ô nhập (gắn vào đúng ô trong form)
 */
export class ApiError extends Error {
  readonly status: number;
  readonly fieldErrors: Record<string, string>;

  constructor(status: number, message: string, fieldErrors: Record<string, string> = {}) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.fieldErrors = fieldErrors;
  }

  get hasFieldErrors(): boolean {
    return Object.keys(this.fieldErrors).length > 0;
  }
}

const DEFAULT_MESSAGES: Record<number, string> = {
  400: "Dữ liệu chưa hợp lệ, vui lòng kiểm tra lại",
  401: "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại",
  403: "Bạn không có quyền thực hiện thao tác này",
  404: "Không tìm thấy dữ liệu",
  409: "Dữ liệu đã thay đổi, vui lòng tải lại trang",
  413: "File tải lên quá lớn",
  423: "Tài khoản đang tạm khóa",
  429: "Bạn thao tác quá nhanh, vui lòng thử lại sau",
  500: "Hệ thống đang gặp sự cố, vui lòng thử lại sau",
  502: "Dịch vụ tạm thời gián đoạn, vui lòng thử lại sau",
};

export async function toApiError(response: Response): Promise<ApiError> {
  let body: { message?: string; fieldErrors?: Record<string, string> } | null = null;
  try {
    body = await response.json();
  } catch {
    // Body rỗng hoặc không phải JSON
  }
  const fieldErrors = body?.fieldErrors ?? {};
  const message =
    body?.message ??
    (Object.keys(fieldErrors).length > 0
      ? DEFAULT_MESSAGES[400]
      : DEFAULT_MESSAGES[response.status] ?? DEFAULT_MESSAGES[500]);
  return new ApiError(response.status, message, fieldErrors);
}

export const NETWORK_ERROR = new ApiError(0, "Không kết nối được máy chủ. Vui lòng kiểm tra mạng và thử lại");

/** Lấy câu thông báo hiển thị được từ một lỗi bất kỳ. */
export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  return "Đã có lỗi xảy ra, vui lòng thử lại";
}
