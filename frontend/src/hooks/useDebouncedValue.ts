"use client";

import { useEffect, useState } from "react";

/** Trả về giá trị sau khi người dùng ngừng thay đổi `delay` ms — dùng cho ô tìm kiếm (không gọi API mỗi lần gõ phím). */
export function useDebouncedValue<T>(value: T, delay = 400): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(timer);
  }, [value, delay]);
  return debounced;
}
