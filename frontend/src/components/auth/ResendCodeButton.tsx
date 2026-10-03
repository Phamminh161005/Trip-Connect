"use client";

import { useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { useCountdown } from "@/hooks/useCountdown";
import { errorMessage } from "@/lib/api/errors";

const RESEND_COOLDOWN_SECONDS = 60; // khớp với OTP_RESEND_COOLDOWN_SECONDS của Backend

/** Nút "Gửi lại mã" có đếm ngược 60 giây. `startCoolingDown`: mã vừa được gửi ở bước trước. */
export function ResendCodeButton({ onResend, startCoolingDown = true }: { onResend: () => Promise<void>; startCoolingDown?: boolean }) {
  const { secondsLeft, isRunning, start } = useCountdown(startCoolingDown ? RESEND_COOLDOWN_SECONDS : 0);
  const [sending, setSending] = useState(false);

  const handleClick = async () => {
    setSending(true);
    try {
      await onResend();
      toast.success("Đã gửi mã mới, vui lòng kiểm tra email (kể cả mục Spam)");
      start(RESEND_COOLDOWN_SECONDS);
    } catch (error) {
      toast.error(errorMessage(error));
    } finally {
      setSending(false);
    }
  };

  return (
    <Button type="button" variant="link" className="h-auto p-0" disabled={isRunning || sending} onClick={handleClick}>
      {isRunning ? `Gửi lại mã sau ${secondsLeft}s` : sending ? "Đang gửi..." : "Gửi lại mã"}
    </Button>
  );
}
