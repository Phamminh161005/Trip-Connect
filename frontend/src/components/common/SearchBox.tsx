"use client";

import { useEffect, useState } from "react";
import { Search } from "lucide-react";
import { Input } from "@/components/ui/input";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";

/** Ô tìm kiếm: chỉ báo ra ngoài sau khi người dùng ngừng gõ 0,4 giây (không gọi API mỗi lần gõ phím). */
export function SearchBox({
  value,
  onSearch,
  placeholder,
}: {
  value: string;
  onSearch: (keyword: string) => void;
  placeholder: string;
}) {
  const [text, setText] = useState(value);
  const debounced = useDebouncedValue(text);

  useEffect(() => {
    if (debounced.trim() !== value) onSearch(debounced.trim());
    // Chỉ chạy khi giá trị đã debounce thay đổi
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debounced]);

  return (
    <div className="relative w-full sm:max-w-sm">
      <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
      <Input
        type="search"
        value={text}
        onChange={(e) => setText(e.target.value)}
        placeholder={placeholder}
        aria-label={placeholder}
        className="h-10 rounded-xl pl-9"
      />
    </div>
  );
}
