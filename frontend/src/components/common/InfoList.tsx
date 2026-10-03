import type { ReactNode } from "react";

/** Danh sách "nhãn: giá trị" dạng 2 cột, dùng ở các trang chi tiết quản trị. */
export function InfoList({ children }: { children: ReactNode }) {
  return <dl className="divide-y">{children}</dl>;
}

export function InfoRow({ label, value, hint }: { label: string; value: ReactNode; hint?: ReactNode }) {
  return (
    <div className="grid gap-1 py-3 sm:grid-cols-[200px_1fr] sm:gap-4">
      <dt className="text-sm text-muted-foreground">{label}</dt>
      <dd className="text-sm font-medium wrap-break-word">
        {value || <span className="font-normal text-muted-foreground italic">Chưa có</span>}
        {hint && <div className="mt-1.5 font-normal">{hint}</div>}
      </dd>
    </div>
  );
}
