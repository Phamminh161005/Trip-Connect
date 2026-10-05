import type { ReactNode } from "react";

/**
 * Danh sách "nhãn: giá trị", dùng ở các trang chi tiết quản trị.
 * Chia 2 cột theo độ rộng của chính danh sách (container query) — đặt trong cột hẹp thì nhãn nằm trên giá trị.
 */
export function InfoList({ children }: { children: ReactNode }) {
  return <dl className="@container divide-y">{children}</dl>;
}

export function InfoRow({ label, value, hint }: { label: string; value: ReactNode; hint?: ReactNode }) {
  return (
    <div className="grid gap-1 py-3 @lg:grid-cols-[200px_1fr] @lg:gap-4">
      <dt className="text-sm text-muted-foreground">{label}</dt>
      <dd className="min-w-0 text-sm font-medium wrap-break-word">
        {value || <span className="font-normal text-muted-foreground italic">Chưa có</span>}
        {hint && <div className="mt-1.5 font-normal">{hint}</div>}
      </dd>
    </div>
  );
}
