import { notFound } from "next/navigation";

/** Id trên đường dẫn (/tours/15) phải là số nguyên dương, sai thì hiện trang 404. */
export function parseIdParam(value: string): number {
  const id = Number(value);
  if (!Number.isSafeInteger(id) || id <= 0) notFound();
  return id;
}
