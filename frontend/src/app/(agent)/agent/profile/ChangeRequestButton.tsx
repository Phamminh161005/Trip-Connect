import Link from "next/link";
import { FilePen } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { AgentProfileResponse } from "@/types/agent";

/** Hồ sơ đã duyệt: muốn đổi thông tin pháp lý phải gửi yêu cầu cập nhật (mỗi lúc chỉ 1 yêu cầu chờ duyệt). */
export function ChangeRequestButton({ profile }: { profile: AgentProfileResponse }) {
  if (profile.status !== "APPROVED") return null;
  return (
    <Button asChild variant="outline" size="sm" className="rounded-lg">
      {profile.pendingChangeRequestId ? (
        <a href="#change-requests">Xem yêu cầu đang chờ</a>
      ) : (
        <Link href="/agent/profile/change-request">
          <FilePen /> Yêu cầu cập nhật
        </Link>
      )}
    </Button>
  );
}
