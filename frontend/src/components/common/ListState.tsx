import { Skeleton } from "@/components/ui/skeleton";
import { errorMessage } from "@/lib/api/errors";

/** Trạng thái chung cho các bảng danh sách: đang tải / lỗi / trống. Trả null khi có dữ liệu để hiển thị. */
export function ListState({
  isPending,
  error,
  isEmpty,
  emptyText,
}: {
  isPending: boolean;
  error: unknown;
  isEmpty: boolean;
  emptyText: string;
}) {
  if (isPending) {
    return (
      <div className="flex flex-col gap-2 py-2" aria-busy="true">
        {Array.from({ length: 5 }, (_, i) => (
          <Skeleton key={i} className="h-12 rounded-lg" />
        ))}
      </div>
    );
  }
  if (error) return <p className="py-10 text-center text-sm text-destructive">{errorMessage(error)}</p>;
  if (isEmpty) return <p className="py-10 text-center text-sm text-muted-foreground">{emptyText}</p>;
  return null;
}
