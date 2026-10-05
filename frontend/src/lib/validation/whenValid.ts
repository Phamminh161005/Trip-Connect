import type { z } from "zod";

/**
 * Mặc định zod chỉ chạy luật nhiều ô (refine / superRefine trên cả object) khi mọi ô của form đã hợp lệ,
 * nên lỗi kiểu "ngày muộn nhất phải sau ngày sớm nhất" chỉ hiện khi đã điền xong cả form.
 * Dùng làm `when` để luật chạy ngay khi các ô nó cần đã hợp lệ.
 */
export function whenValid<S extends z.ZodObject>(schema: S, ...keys: (keyof S["shape"] & string)[]) {
  const picked = schema.pick(Object.fromEntries(keys.map((key) => [key, true])) as never);
  return (payload: { value: unknown }) => picked.safeParse(payload.value).success;
}
