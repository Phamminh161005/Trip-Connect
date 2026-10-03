"use client";

import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { errorMessage } from "@/lib/api/errors";
import { useAgentProfile } from "../useAgentProfile";
import { ChangeRequestForm } from "./ChangeRequestForm";

export function ChangeRequestView() {
  const { data: profile, isPending, isError, error } = useAgentProfile();

  const header = (
    <div className="flex flex-col gap-2">
      <Button asChild variant="ghost" size="sm" className="-ml-2 self-start rounded-lg text-muted-foreground">
        <Link href="/agent/profile">
          <ArrowLeft /> Hồ sơ kinh doanh
        </Link>
      </Button>
      <h1 className="text-2xl font-bold tracking-tight">Yêu cầu cập nhật hồ sơ</h1>
      <p className="text-muted-foreground">
        Sửa những thông tin cần thay đổi. Thay đổi chỉ được áp dụng sau khi quản trị viên duyệt; trong lúc chờ, bạn vẫn
        hoạt động bình thường với thông tin hiện tại.
      </p>
    </div>
  );

  if (isPending) {
    return (
      <div className="flex flex-col gap-6">
        {header}
        <Skeleton className="h-96 rounded-2xl" />
      </div>
    );
  }

  // Chỉ hồ sơ đã duyệt, và chưa có yêu cầu nào đang chờ, mới gửi được yêu cầu mới (Backend cũng chặn)
  const blocked = isError
    ? errorMessage(error)
    : profile.status !== "APPROVED"
      ? "Hồ sơ chưa được duyệt nên bạn có thể sửa trực tiếp ở trang Hồ sơ kinh doanh (khi hồ sơ ở trạng thái Nháp hoặc Cần bổ sung)."
      : profile.pendingChangeRequestId
        ? "Bạn đang có một yêu cầu cập nhật chờ duyệt. Vui lòng chờ kết quả, hoặc hủy yêu cầu đó để gửi yêu cầu mới."
        : null;

  return (
    <div className="flex flex-col gap-6">
      {header}
      {blocked || !profile ? (
        <div className="flex flex-col items-start gap-4 rounded-2xl border bg-card p-6">
          <p>{blocked}</p>
          <Button asChild className="rounded-xl">
            <Link href="/agent/profile#change-requests">Về trang Hồ sơ kinh doanh</Link>
          </Button>
        </div>
      ) : (
        <ChangeRequestForm profile={profile} />
      )}
    </div>
  );
}
