import { CircleAlert, CircleCheck, Clock, PencilLine } from "lucide-react";
import { formatDateTime } from "@/lib/format";
import { cn } from "@/lib/utils";
import type { AgentProfileResponse } from "@/types/agent";

/** Thanh trạng thái đầu trang: màu + nội dung đổi theo trạng thái hồ sơ. */
export function StatusBanner({ profile, action }: { profile: AgentProfileResponse; action?: React.ReactNode }) {
  const config = {
    DRAFT: {
      icon: PencilLine,
      className: "border-sky-200 bg-sky-50 text-sky-900",
      title: "Hoàn thiện hồ sơ để gửi duyệt",
      body: "Bổ sung đầy đủ thông tin và giấy tờ bên dưới, sau đó bấm \"Nộp hồ sơ\". Quản trị viên sẽ kiểm tra và phản hồi qua email.",
    },
    PENDING_APPROVAL: {
      icon: Clock,
      className: "border-amber-200 bg-amber-50 text-amber-900",
      title: "Hồ sơ đang chờ duyệt",
      body: `Bạn đã nộp hồ sơ lúc ${formatDateTime(profile.submittedAt)}. Trong thời gian chờ, thông tin pháp lý không thể chỉnh sửa. Bạn vẫn dùng được mọi chức năng của khách hàng.`,
    },
    NEEDS_REVISION: {
      icon: CircleAlert,
      className: "border-red-200 bg-red-50 text-red-900",
      title: "Hồ sơ cần bổ sung",
      body: "Vui lòng chỉnh sửa theo yêu cầu của quản trị viên rồi nộp lại hồ sơ.",
    },
    APPROVED: {
      icon: CircleCheck,
      className: "border-emerald-200 bg-emerald-50 text-emerald-900",
      title: "Hồ sơ đã được duyệt",
      body: `Bạn có thể đăng bán tour và nhận yêu cầu tư vấn. Để thay đổi thông tin pháp lý, hãy gửi yêu cầu cập nhật hồ sơ.`,
    },
  }[profile.status];

  const Icon = config.icon;

  return (
    <section role="status" className={cn("flex flex-col gap-3 rounded-2xl border p-5 sm:flex-row sm:items-start", config.className)}>
      <Icon className="size-6 shrink-0" aria-hidden="true" />
      <div className="flex flex-1 flex-col gap-1.5">
        <h2 className="font-semibold">{config.title}</h2>
        <p className="text-sm opacity-90">{config.body}</p>
        {profile.status === "NEEDS_REVISION" && profile.rejectionReason && (
          <p className="mt-1 rounded-xl bg-white/70 px-4 py-3 text-sm">
            <span className="font-semibold">Lý do từ quản trị viên: </span>
            {profile.rejectionReason}
          </p>
        )}
        {profile.status === "APPROVED" && (
          <p className="text-xs opacity-80">
            Khi tạm ngừng, hệ thống không phân bổ yêu cầu tư vấn mới cho bạn. Hệ thống tự tạm ngừng nếu bạn không đăng
            nhập quá 14 ngày.
          </p>
        )}
        {profile.pendingChangeRequestId && (
          <a href="#change-requests" className="mt-1 w-fit text-sm font-semibold underline underline-offset-4">
            Bạn có một yêu cầu cập nhật hồ sơ đang chờ duyệt →
          </a>
        )}
      </div>
      {action}
    </section>
  );
}
