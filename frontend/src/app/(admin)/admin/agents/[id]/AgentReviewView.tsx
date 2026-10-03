"use client";

import { useState } from "react";
import Link from "next/link";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowRight, Check, TriangleAlert, Undo2 } from "lucide-react";
import { toast } from "sonner";
import { AdminDocumentList } from "@/components/admin/AdminDocumentList";
import { InfoList, InfoRow } from "@/components/common/InfoList";
import { PageHeader } from "@/components/common/PageHeader";
import { ReasonDialog } from "@/components/common/ReasonDialog";
import { AgentStatusBadge } from "@/components/admin/StatusBadges";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { locationLabel } from "@/components/auth/register/LocationMultiSelect";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { bankHolderMismatch } from "@/lib/admin/bankHolderCheck";
import { approveAgentProfile, getAgentProfileForAdmin, rejectAgentProfile } from "@/lib/api/admin";
import { errorMessage } from "@/lib/api/errors";
import { formatDateTime } from "@/lib/format";
import type { AgentProfileResponse } from "@/types/agent";
import { ADMIN_KEY } from "../../adminQueries";

const BACK = { href: "/admin/agents", label: "Danh sách hồ sơ" };

export function AgentReviewView({ id }: { id: number }) {
  const query = useQuery({ queryKey: [...ADMIN_KEY, "agent-profile", id], queryFn: () => getAgentProfileForAdmin(id) });

  if (query.isPending) {
    return (
      <div className="flex flex-col gap-4">
        <Skeleton className="h-10 w-72" />
        <Skeleton className="h-64 rounded-2xl" />
        <Skeleton className="h-64 rounded-2xl" />
      </div>
    );
  }
  if (query.isError) {
    return (
      <>
        <PageHeader title="Hồ sơ đối tác" back={BACK} />
        <p className="text-sm text-destructive">{errorMessage(query.error)}</p>
      </>
    );
  }
  return <AgentReview profile={query.data} />;
}

function AgentReview({ profile }: { profile: AgentProfileResponse }) {
  const queryClient = useQueryClient();
  const [approveOpen, setApproveOpen] = useState(false);
  const [rejectOpen, setRejectOpen] = useState(false);
  const canReview = profile.status === "PENDING_APPROVAL";
  const holderMismatch = bankHolderMismatch(profile.companyName, profile.bankAccountHolder);

  // Làm mới mọi dữ liệu quản trị (danh sách, số đếm ở menu, chính hồ sơ này)
  const refresh = () => queryClient.invalidateQueries({ queryKey: ADMIN_KEY });

  const approve = async () => {
    try {
      await approveAgentProfile(profile.id);
      toast.success("Đã duyệt hồ sơ. Đối tác sẽ nhận được email thông báo.");
      await refresh();
    } catch (error) {
      toast.error(errorMessage(error));
      await refresh();
      throw error;
    }
  };

  const reject = async (reason: string) => {
    try {
      await rejectAgentProfile(profile.id, reason);
      toast.success("Đã gửi yêu cầu bổ sung tới đối tác.");
      await refresh();
    } catch (error) {
      toast.error(errorMessage(error));
      throw error;
    }
  };

  return (
    <>
      <PageHeader
        back={BACK}
        title={
          <span className="flex flex-wrap items-center gap-3">
            {profile.companyName ?? "Chưa đặt tên công ty"}
            <AgentStatusBadge status={profile.status} />
          </span>
        }
        description={
          profile.submittedAt
            ? `Nộp lúc ${formatDateTime(profile.submittedAt)}${profile.reviewedAt ? ` · Xét duyệt lần cuối ${formatDateTime(profile.reviewedAt)}` : ""}`
            : "Đối tác chưa nộp hồ sơ."
        }
        actions={
          canReview && (
            <>
              <Button variant="outline" className="rounded-xl" onClick={() => setRejectOpen(true)}>
                <Undo2 /> Yêu cầu bổ sung
              </Button>
              <Button className="rounded-xl" onClick={() => setApproveOpen(true)}>
                <Check /> Duyệt hồ sơ
              </Button>
            </>
          )
        }
      />

      <div className="flex flex-col gap-6">
        {profile.status === "NEEDS_REVISION" && profile.rejectionReason && (
          <Alert className="rounded-2xl border-red-200 bg-red-50">
            <AlertTitle>Đã yêu cầu bổ sung</AlertTitle>
            <AlertDescription className="whitespace-pre-line">{profile.rejectionReason}</AlertDescription>
          </Alert>
        )}
        {profile.pendingChangeRequestId && (
          <Alert className="rounded-2xl border-amber-200 bg-amber-50">
            <AlertTitle>Đối tác đang có yêu cầu cập nhật hồ sơ chờ duyệt</AlertTitle>
            <AlertDescription>
              <Link href={`/admin/change-requests/${profile.pendingChangeRequestId}`} className="flex items-center gap-1 font-medium underline">
                Xem yêu cầu <ArrowRight className="size-3.5" />
              </Link>
            </AlertDescription>
          </Alert>
        )}

        <Card className="rounded-2xl">
          <CardHeader>
            <CardTitle className="text-lg">Thông tin doanh nghiệp</CardTitle>
            <CardDescription>Đối chiếu với giấy tờ bên dưới trước khi duyệt.</CardDescription>
          </CardHeader>
          <CardContent>
            <InfoList>
              <InfoRow label="Tên công ty / hộ kinh doanh" value={profile.companyName} />
              <InfoRow label="Mã số thuế" value={profile.taxCode} />
              <InfoRow label="Số giấy phép lữ hành" value={profile.businessLicense} />
              <InfoRow
                label="Địa chỉ trụ sở"
                value={profile.address && profile.addressProvince ? `${profile.address}, ${profile.addressProvince.province}` : null}
              />
              <InfoRow label="Ngân hàng" value={profile.bank ? `${profile.bank.shortName} — ${profile.bank.name}` : null} />
              <InfoRow label="Số tài khoản" value={profile.bankAccountNumber} />
              <InfoRow
                label="Tên chủ tài khoản"
                value={profile.bankAccountHolder}
                hint={
                  holderMismatch && (
                    <span className="flex items-start gap-1.5 text-sm text-amber-800">
                      <TriangleAlert className="mt-0.5 size-4 shrink-0" />
                      Tên chủ tài khoản không chứa tên công ty — hãy kiểm tra đây có phải tài khoản của doanh nghiệp không.
                    </span>
                  )
                }
              />
            </InfoList>
          </CardContent>
        </Card>

        <Card className="rounded-2xl">
          <CardHeader>
            <CardTitle className="text-lg">Giấy tờ pháp lý</CardTitle>
            <CardDescription>Link xem giấy tờ chỉ có hiệu lực 5 phút.</CardDescription>
          </CardHeader>
          <CardContent>
            <AdminDocumentList profileId={profile.id} documents={profile.documents} />
          </CardContent>
        </Card>

        <div className="grid gap-6 lg:grid-cols-2">
          <Card className="rounded-2xl">
            <CardHeader>
              <CardTitle className="text-lg">Người đại diện</CardTitle>
            </CardHeader>
            <CardContent>
              <InfoList>
                <InfoRow label="Họ và tên" value={profile.fullName} />
                <InfoRow label="Email" value={profile.email} />
                <InfoRow label="Số điện thoại" value={profile.phone} />
                <InfoRow label="Đăng ký lúc" value={formatDateTime(profile.createdAt)} />
              </InfoList>
            </CardContent>
          </Card>

          <Card className="rounded-2xl">
            <CardHeader>
              <CardTitle className="text-lg">Chuyên môn</CardTitle>
            </CardHeader>
            <CardContent className="flex flex-col gap-4">
              <Chips title="Khu vực phụ trách" items={profile.serviceAreas.map((l) => ({ id: l.id, name: locationLabel(l) }))} />
              <Chips title="Loại hình tour" items={profile.specialties} />
            </CardContent>
          </Card>
        </div>
      </div>

      <ConfirmDialog
        open={approveOpen}
        onOpenChange={setApproveOpen}
        title="Duyệt hồ sơ đối tác?"
        description="Sau khi duyệt, đối tác có thể đăng tour và nhận yêu cầu thiết kế tour. Hệ thống sẽ gửi email thông báo cho họ."
        confirmLabel="Duyệt hồ sơ"
        onConfirm={approve}
      />
      <ReasonDialog
        open={rejectOpen}
        onOpenChange={setRejectOpen}
        title="Yêu cầu bổ sung hồ sơ"
        description="Hồ sơ sẽ chuyển về trạng thái “Cần bổ sung” để đối tác sửa và nộp lại."
        label="Cần bổ sung / sửa những gì?"
        placeholder="Ví dụ: Ảnh CCCD mặt sau bị mờ, vui lòng chụp lại rõ nét."
        confirmLabel="Gửi yêu cầu bổ sung"
        onConfirm={reject}
      />
    </>
  );
}

function Chips({ title, items }: { title: string; items: { id: number; name: string }[] }) {
  return (
    <div>
      <p className="mb-2 text-sm text-muted-foreground">{title}</p>
      {items.length === 0 ? (
        <p className="text-sm text-muted-foreground italic">Chưa chọn</p>
      ) : (
        <div className="flex flex-wrap gap-2">
          {items.map((item) => (
            <Badge key={item.id} variant="secondary" className="px-2.5 py-1">
              {item.name}
            </Badge>
          ))}
        </div>
      )}
    </div>
  );
}
