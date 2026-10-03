import type { Metadata } from "next";
import credits from "../../../../public/images/destinations/credits.json";

export const metadata: Metadata = { title: "Nguồn hình ảnh" };

/** Ghi nguồn ảnh — bắt buộc theo giấy phép Creative Commons (CC BY / CC BY-SA). */
export default function CreditsPage() {
  return (
    <div className="mx-auto max-w-4xl px-4 py-12 sm:px-6">
      <h1 className="text-3xl font-bold tracking-tight">Nguồn hình ảnh</h1>
      <p className="mt-3 text-muted-foreground">
        Ảnh địa danh được lấy từ Wikimedia Commons và sử dụng theo giấy phép Creative Commons. Ảnh đã được thu nhỏ
        kích thước, không chỉnh sửa nội dung.
      </p>
      <ul className="mt-8 divide-y rounded-2xl border">
        {credits.map((item) => (
          <li key={item.file} className="flex flex-col gap-1 p-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p className="font-semibold">{item.place}</p>
              <p className="text-sm text-muted-foreground">Tác giả: {item.author}</p>
            </div>
            <div className="flex gap-4 text-sm">
              <a href={item.licenseUrl ?? undefined} target="_blank" rel="noreferrer" className="text-primary underline-offset-4 hover:underline">
                {item.license}
              </a>
              <a href={item.source} target="_blank" rel="noreferrer" className="text-primary underline-offset-4 hover:underline">
                Xem ảnh gốc
              </a>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}
