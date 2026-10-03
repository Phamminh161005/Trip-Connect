import type { FieldValues, Path, UseFormSetError } from "react-hook-form";
import { toast } from "sonner";
import { ApiError, errorMessage } from "@/lib/api/errors";

/**
 * Hiển thị lỗi từ Backend lên form:
 *  - Lỗi theo từng ô (fieldErrors) -> gắn vào đúng ô (tên trường trùng với DTO của Backend)
 *  - Còn lại -> thông báo nhanh (toast)
 * `fieldMap` dùng khi tên ô trong form khác tên trường của Backend (ví dụ "agentProfile.companyName" -> "companyName").
 */
export function applyApiError<T extends FieldValues>(
  error: unknown,
  setError: UseFormSetError<T>,
  fieldMap: Record<string, Path<T>> = {},
): void {
  if (error instanceof ApiError && error.hasFieldErrors) {
    let mappedAny = false;
    for (const [backendField, message] of Object.entries(error.fieldErrors)) {
      const formField = fieldMap[backendField] ?? (backendField as Path<T>);
      setError(formField, { type: "server", message }, { shouldFocus: !mappedAny });
      mappedAny = true;
    }
    return;
  }
  toast.error(errorMessage(error));
}
