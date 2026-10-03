"use client";

import { useCallback, useEffect, useState } from "react";

/** Đếm ngược theo giây — dùng cho nút "Gửi lại mã" (Backend chỉ cho gửi lại sau 60 giây). */
export function useCountdown(initialSeconds = 0) {
  const [secondsLeft, setSecondsLeft] = useState(initialSeconds);

  useEffect(() => {
    if (secondsLeft <= 0) return;
    const timer = setTimeout(() => setSecondsLeft((s) => s - 1), 1000);
    return () => clearTimeout(timer);
  }, [secondsLeft]);

  const start = useCallback((seconds: number) => setSecondsLeft(seconds), []);

  return { secondsLeft, isRunning: secondsLeft > 0, start };
}
