import { Fragment, type ReactNode } from "react";
import Link from "next/link";
import type { TourCard } from "@/types/search";

const INLINE = /(\*\*[^*\n]+\*\*|\[tour:\d+\])/g;
const BULLET = /^(\s*)(?:[*\-+•]|\d+[.)])\s+(.*)$/;
const HEADING = /^#{1,6}\s+(.*)$/;

type Block = { kind: "p"; lines: string[] } | { kind: "ul"; items: { depth: number; text: string }[] };

/**
 * Hiển thị câu trả lời của trợ lý: hỗ trợ phần Markdown mà mô hình hay dùng (đoạn văn, gạch đầu dòng, **đậm**)
 * và đổi mã [tour:ID] thành link tới tour. Không dùng HTML từ mô hình nên không lo chèn mã độc.
 */
export function AssistantText({ text, tours }: { text: string; tours: Map<number, TourCard> }) {
  const blocks: Block[] = [];
  // Mô hình đôi khi bọc cả tên tour: "[Phuket 4N3Đ [tour:19]]" -> "Phuket 4N3Đ [tour:19]"
  // và mã không phải số ("[tour:destination]") thì bỏ
  const cleaned = text
    .replace(/\[([^[\]\n]*?)\s*(\[tour:\d+\])\]/g, "$1 $2")
    .replace(/\s?\[tour:[^\]\d][^\]]*\]/g, "");
  for (const raw of cleaned.split("\n")) {
    const line = raw.trimEnd();
    if (line.trim() === "") {
      blocks.push({ kind: "p", lines: [] });
      continue;
    }
    const bullet = BULLET.exec(line);
    const last = blocks.at(-1);
    if (bullet) {
      const item = { depth: bullet[1].length >= 2 ? 1 : 0, text: bullet[2] };
      if (last?.kind === "ul") last.items.push(item);
      else blocks.push({ kind: "ul", items: [item] });
      continue;
    }
    const heading = HEADING.exec(line);
    const content = heading ? `**${heading[1]}**` : line.trim();
    if (last?.kind === "p") last.lines.push(content);
    else blocks.push({ kind: "p", lines: [content] });
  }

  return (
    <div className="flex flex-col gap-2 text-sm leading-relaxed">
      {blocks.map((block, index) => {
        if (block.kind === "ul") {
          return (
            <ul key={index} className="flex flex-col gap-1">
              {block.items.map((item, i) => (
                <li key={i} className={item.depth > 0 ? "ml-4 flex gap-2" : "flex gap-2"}>
                  <span className="mt-2 size-1.5 shrink-0 rounded-full bg-current opacity-50" />
                  <span className="min-w-0">{inline(item.text, tours)}</span>
                </li>
              ))}
            </ul>
          );
        }
        if (block.lines.length === 0) return null;
        return (
          <p key={index}>
            {block.lines.map((l, i) => (
              <Fragment key={i}>
                {i > 0 && <br />}
                {inline(l, tours)}
              </Fragment>
            ))}
          </p>
        );
      })}
    </div>
  );
}

function inline(text: string, tours: Map<number, TourCard>): ReactNode[] {
  return text.split(INLINE).map((part, i) => {
    if (part.startsWith("**") && part.endsWith("**") && part.length > 4) {
      // Mô hình hay in đậm cả tên tour lẫn mã: "**Nha Trang 3N2Đ [tour:9]**" -> vẫn phải đổi mã thành link
      return <strong key={i}>{inline(part.slice(2, -2), tours)}</strong>;
    }
    const ref = /^\[tour:(\d+)\]$/.exec(part);
    if (ref) {
      const id = Number(ref[1]);
      // Mã tour không có trong kết quả tìm kiếm (mô hình nhớ nhầm) thì ẩn đi
      if (!tours.has(id)) return null;
      return (
        <Link
          key={i}
          href={`/tours/${id}`}
          className="mx-0.5 inline-flex items-center rounded-md bg-primary/10 px-1.5 py-px text-xs font-medium text-primary hover:bg-primary/20"
        >
          Xem tour
        </Link>
      );
    }
    return part;
  });
}
