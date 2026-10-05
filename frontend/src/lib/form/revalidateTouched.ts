import type { FieldValues, Path, UseFormReturn } from "react-hook-form";

/**
 * Sửa một ô thì kiểm tra lại các ô liên quan (vd đổi giá người lớn -> kiểm lại giá trẻ em),
 * nhưng chỉ ô người dùng đã chạm vào — tránh báo "chưa nhập" ở ô họ chưa tới.
 */
export function revalidateTouched<T extends FieldValues>(form: UseFormReturn<T>, ...names: Path<T>[]) {
  const touched = names.filter((name) => form.getFieldState(name).isTouched);
  if (touched.length > 0) void form.trigger(touched);
}
