import type { Metadata } from "next";
import destinationCredits from "../../../../public/images/destinations/credits.json";
import tourImageCredits from "@/data/tourImageCredits.json";

export const metadata: Metadata = { title: "Nguồn hình ảnh" };

const linkClass = "text-primary underline-offset-4 hover:underline";

/** Ảnh tour mẫu, gom theo tour (giữ thứ tự xuất hiện trong file). */
function groupByTour() {
  const groups = new Map<string, typeof tourImageCredits>();
  for (const item of tourImageCredits) {
    groups.set(item.tour, [...(groups.get(item.tour) ?? []), item]);
  }
  return [...groups.entries()];
}

/** Ghi nguồn ảnh — bắt buộc theo giấy phép Creative Commons (CC BY / CC BY-SA). */
export default function CreditsPage() {
  return (
    <div className="mx-auto max-w-4xl px-4 py-12 sm:px-6">
      <h1 className="text-3xl font-bold tracking-tight">Nguồn hình ảnh</h1>
      <p className="mt-3 text-muted-foreground">
        Ảnh được lấy từ Wikimedia Commons và sử dụng theo giấy phép ghi bên cạnh (Creative Commons hoặc phạm vi công cộng).
        Ảnh đã được thu nhỏ kích thước, không chỉnh sửa nội dung.
      </p>

      <h2 className="mt-10 text-xl font-semibold">Ảnh địa danh</h2>
      <ul className="mt-4 divide-y rounded-2xl border">
        {destinationCredits.map((item) => (
          <li key={item.file} className="flex flex-col gap-1 p-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p className="font-semibold">{item.place}</p>
              <p className="text-sm text-muted-foreground">Tác giả: {item.author}</p>
            </div>
            <div className="flex shrink-0 gap-4 text-sm">
              <a href={item.licenseUrl ?? undefined} target="_blank" rel="noreferrer" className={linkClass}>
                {item.license}
              </a>
              <a href={item.source} target="_blank" rel="noreferrer" className={linkClass}>
                Xem ảnh gốc
              </a>
            </div>
          </li>
        ))}
      </ul>

      <h2 className="mt-10 text-xl font-semibold">Ảnh các tour mẫu</h2>
      <div className="mt-4 flex flex-col gap-6">
        {groupByTour().map(([tour, items]) => (
          <section key={tour}>
            <h3 className="mb-2 font-semibold">{tour}</h3>
            <ul className="divide-y rounded-2xl border">
              {items.map((item) => (
                <li key={item.source} className="flex flex-col gap-1 p-4 sm:flex-row sm:items-center sm:justify-between">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium">{item.image}</p>
                    <p className="text-sm text-muted-foreground">Tác giả: {item.author}</p>
                  </div>
                  <div className="flex shrink-0 gap-4 text-sm">
                    <a href={item.licenseUrl} target="_blank" rel="noreferrer" className={linkClass}>
                      {item.license}
                    </a>
                    <a href={item.source} target="_blank" rel="noreferrer" className={linkClass}>
                      Xem ảnh gốc
                    </a>
                  </div>
                </li>
              ))}
            </ul>
          </section>
        ))}
      </div>
    </div>
  );
}
