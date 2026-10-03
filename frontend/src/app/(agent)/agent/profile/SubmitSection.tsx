"use client";

import { useState } from "react";
import { CircleAlert, Send } from "lucide-react";
import { toast } from "sonner";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { Button } from "@/components/ui/button";
import { submitAgentProfile } from "@/lib/api/agent";
import { errorMessage } from "@/lib/api/errors";
import { useAuth } from "@/lib/auth/AuthProvider";
import type { AgentProfileResponse } from "@/types/agent";
import { useSetAgentProfile } from "./useAgentProfile";

/** Danh sách mục còn thiếu (do Backend tính) + nút Nộp hồ sơ. Chỉ hiện khi hồ sơ Nháp / Cần bổ sung. */
export function SubmitSection({ profile }: { profile: AgentProfileResponse }) {
  const { set } = useSetAgentProfile();
  const { reloadUser } = useAuth();
  const [confirming, setConfirming] = useState(false);

  const missing = profile.missingItems;
  const resubmission = profile.status === "NEEDS_REVISION";

  return (
    <section className="flex flex-col gap-4 rounded-2xl border bg-card p-5 sm:flex-row sm:items-center sm:justify-between">
      {missing.length > 0 ? (
        <div className="flex items-start gap-3">
          <CircleAlert className="mt-0.5 size-5 shrink-0 text-amber-600" />
          <div>
            <p className="font-medium">Hồ sơ còn thiếu {missing.length} mục</p>
            <p className="text-sm text-muted-foreground first-letter:uppercase">{missing.join(", ")}.</p>
          </div>
        </div>
      ) : (
        <p className="font-medium text-emerald-700">Hồ sơ đã đầy đủ, bạn có thể nộp để quản trị viên duyệt.</p>
      )}

      <Button
        size="lg"
        className="h-12 shrink-0 rounded-xl px-6 font-semibold"
        disabled={missing.length > 0}
        onClick={() => setConfirming(true)}
      >
        <Send /> {resubmission ? "Nộp lại hồ sơ" : "Nộp hồ sơ để duyệt"}
      </Button>

      <ConfirmDialog
        open={confirming}
        onOpenChange={setConfirming}
        title={resubmission ? "Nộp lại hồ sơ?" : "Nộp hồ sơ để duyệt?"}
        description="Sau khi nộp, bạn không thể chỉnh sửa thông tin và giấy tờ cho đến khi có kết quả duyệt. Kết quả sẽ được gửi qua email."
        confirmLabel="Nộp hồ sơ"
        onConfirm={async () => {
          try {
            set(await submitAgentProfile());
            await reloadUser(); // cập nhật trạng thái Agent trong phiên đăng nhập
            toast.success("Đã nộp hồ sơ. Chúng tôi sẽ phản hồi qua email.");
            window.scrollTo({ top: 0, behavior: "smooth" });
          } catch (error) {
            toast.error(errorMessage(error));
            throw error;
          }
        }}
      />
    </section>
  );
}
