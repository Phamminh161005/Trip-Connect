"use client";

import { useState } from "react";
import Link from "next/link";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowRight, Check, TriangleAlert, X } from "lucide-react";
import { toast } from "sonner";
import { DocumentItem } from "@/components/admin/AdminDocumentList";
import { PageHeader } from "@/components/common/PageHeader";
import { ReasonDialog } from "@/components/common/ReasonDialog";
import { ChangeRequestStatusBadge } from "@/components/admin/StatusBadges";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { bankHolderMismatch } from "@/lib/admin/bankHolderCheck";
import { approveChangeRequest, getChangeRequestForAdmin, rejectChangeRequest } from "@/lib/api/admin";
import { errorMessage } from "@/lib/api/errors";
import { formatDateTime } from "@/lib/format";
import { cn } from "@/lib/utils";
import type { AdminChangeRequestDetailResponse } from "@/types/admin";
import type { AgentChangeRequestResponse, AgentProfileResponse } from "@/types/agent";
import { ADMIN_KEY } from "../../adminQueries";

const BACK = { href: "/admin/change-requests", label: "Danh sách yêu cầu" };

export function ChangeRequestReviewView({ id }: { id: number }) {
  const query = useQuery({ queryKey: [...ADMIN_KEY, "change-request", id], queryFn: () => getChangeRequestForAdmin(id) });

  if (query.isPending) {
    return (
      <div className="flex flex-col gap-4">
        <Skeleton className="h-10 w-72" />
        <Skeleton className="h-72 rounded-2xl" />
      </div>
    );
  }
  if (query.isError) {
    return (
      <>
        <PageHeader title="Yêu cầu cập nhật hồ sơ" back={BACK} />
        <p className="text-sm text-destructive">{errorMessage(query.error)}</p>
      </>
    );
  }
  return <ChangeRequestReview detail={query.data} />;
}

interface CompareRow {
  label: string;
  current: string | null;
  /** null = yêu cầu không đổi trường này */
  proposed: string | null;
}

function addressText(address: string | null | undefined, province: string | null | undefined) {
  return [address, province].filter(Boolean).join(", ") || null;
}

/** Dựng bảng so sánh "Hiện tại → Đề xuất". Trường null trong yêu cầu = không đổi. */
function compareRows(request: AgentChangeRequestResponse, profile: AgentProfileResponse): CompareRow[] {
  const changesAddress = request.address !== null || request.addressProvince !== null;
  const changesBank = request.bank !== null;
  return [
    { label: "Tên công ty", current: profile.companyName, proposed: request.companyName },
    { label: "Mã số thuế", current: profile.taxCode, proposed: request.taxCode },
    { label: "Số giấy phép lữ hành", current: profile.businessLicense, proposed: request.businessLicense },
    {
      label: "Địa chỉ trụ sở",
      current: addressText(profile.address, profile.addressProvince?.province),
      proposed: changesAddress
        ? addressText(request.address ?? profile.address, (request.addressProvince ?? profile.addressProvince)?.province)
        : null,
    },
    {
      label: "Ngân hàng",
      current: profile.bank ? `${profile.bank.shortName} — ${profile.bank.name}` : null,
      proposed: changesBank && request.bank ? `${request.bank.shortName} — ${request.bank.name}` : null,
    },
    { label: "Số tài khoản", current: profile.bankAccountNumber, proposed: changesBank ? request.bankAccountNumber : null },
    { label: "Tên chủ tài khoản", current: profile.bankAccountHolder, proposed: changesBank ? request.bankAccountHolder : null },
  ];
}

function ChangeRequestReview({ detail }: { detail: AdminChangeRequestDetailResponse }) {
  const { changeRequest: request, currentProfile: profile } = detail;
  const queryClient = useQueryClient();
  const [approveOpen, setApproveOpen] = useState(false);
  const [rejectOpen, setRejectOpen] = useState(false);
  const pending = request.status === "PENDING";

  const rows = compareRows(request, profile);
  const changedRows = rows.filter((row) => row.proposed !== null);
  // Kiểm tra tên chủ tài khoản theo tên công ty SAU khi áp dụng thay đổi
  const holderMismatch =
    request.bank !== null && bankHolderMismatch(request.companyName ?? profile.companyName, request.bankAccountHolder);

  const refresh = () => queryClient.invalidateQueries({ queryKey: ADMIN_KEY });

  const approve = async () => {
    try {
      await approveChangeRequest(request.id);
      toast.success("Đã duyệt. Hồ sơ đối tác đã được cập nhật.");
      await refresh();
    } catch (error) {
      toast.error(errorMessage(error));
      await refresh();
      throw error;
    }
  };

  const reject = async (reason: string) => {
    try {
      await rejectChangeRequest(request.id, reason);
      toast.success("Đã từ chối yêu cầu và gửi email cho đối tác.");
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
            {profile.companyName ?? `Hồ sơ #${profile.id}`}
            <ChangeRequestStatusBadge status={request.status} />
          </span>
        }
        description={`Gửi lúc ${formatDateTime(request.createdAt)}${request.reviewedAt ? ` · Xử lý lúc ${formatDateTime(request.reviewedAt)}` : ""}`}
        actions={
          pending && (
            <>
              <Button variant="outline" className="rounded-xl" onClick={() => setRejectOpen(true)}>
                <X /> Từ chối
              </Button>
              <Button className="rounded-xl" onClick={() => setApproveOpen(true)}>
                <Check /> Duyệt thay đổi
              </Button>
            </>
          )
        }
      />

      <div className="flex flex-col gap-6">
        {request.status === "REJECTED" && request.rejectionReason && (
          <Alert className="rounded-2xl border-red-200 bg-red-50">
            <AlertTitle>Lý do từ chối</AlertTitle>
            <AlertDescription className="whitespace-pre-line">{request.rejectionReason}</AlertDescription>
          </Alert>
        )}
        {request.status === "APPROVED" && (
          <Alert className="rounded-2xl">
            <AlertDescription>
              Yêu cầu đã được duyệt — cột “Hồ sơ hiện tại” bên dưới đã bao gồm các thay đổi này.
            </AlertDescription>
          </Alert>
        )}

        <Card className="rounded-2xl">
          <CardHeader>
            <CardTitle className="text-lg">Thông tin thay đổi</CardTitle>
            <CardDescription>
              {changedRows.length > 0
                ? `Đối tác đề xuất đổi ${changedRows.length} mục (tô màu). Các mục khác giữ nguyên.`
                : "Yêu cầu này không đổi thông tin nào, chỉ bổ sung giấy tờ."}
            </CardDescription>
          </CardHeader>
          <CardContent className="flex flex-col gap-3">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-44">Mục</TableHead>
                  <TableHead>Hồ sơ hiện tại</TableHead>
                  <TableHead>Đề xuất</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {rows.map((row) => {
                  const changed = row.proposed !== null;
                  return (
                    <TableRow key={row.label} className={cn(changed && "bg-amber-50 hover:bg-amber-50")}>
                      <TableCell className="align-top text-muted-foreground">{row.label}</TableCell>
                      <TableCell className={cn("align-top whitespace-normal", changed && "text-muted-foreground line-through")}>
                        {row.current ?? "—"}
                      </TableCell>
                      <TableCell className="align-top font-medium whitespace-normal">
                        {changed ? row.proposed : <span className="font-normal text-muted-foreground">Không đổi</span>}
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
            {holderMismatch && (
              <p className="flex items-start gap-1.5 text-sm text-amber-800">
                <TriangleAlert className="mt-0.5 size-4 shrink-0" />
                Tên chủ tài khoản mới không chứa tên công ty — hãy kiểm tra đây có phải tài khoản của doanh nghiệp không.
              </p>
            )}
          </CardContent>
        </Card>

        <DocumentsCompare request={request} profile={profile} />

        <Card className="rounded-2xl">
          <CardHeader>
            <CardTitle className="text-lg">Ghi chú của đối tác</CardTitle>
          </CardHeader>
          <CardContent>
            {request.note ? (
              <p className="text-sm whitespace-pre-line">{request.note}</p>
            ) : (
              <p className="text-sm text-muted-foreground italic">Không có ghi chú.</p>
            )}
            <Link
              href={`/admin/agents/${profile.id}`}
              className="mt-4 flex w-fit items-center gap-1 text-sm font-medium text-primary hover:underline"
            >
              Xem toàn bộ hồ sơ đối tác <ArrowRight className="size-3.5" />
            </Link>
          </CardContent>
        </Card>
      </div>

      <ConfirmDialog
        open={approveOpen}
        onOpenChange={setApproveOpen}
        title="Duyệt yêu cầu cập nhật?"
        description="Thông tin và giấy tờ mới sẽ thay thế thông tin hiện tại của hồ sơ (giấy tờ cũ được chuyển vào lưu trữ). Đối tác sẽ nhận email thông báo."
        confirmLabel="Duyệt thay đổi"
        onConfirm={approve}
      />
      <ReasonDialog
        open={rejectOpen}
        onOpenChange={setRejectOpen}
        title="Từ chối yêu cầu cập nhật"
        description="Hồ sơ đối tác giữ nguyên như hiện tại; giấy tờ đính kèm trong yêu cầu sẽ bị xóa."
        label="Lý do từ chối"
        placeholder="Ví dụ: Giấy phép mới chưa có dấu xác nhận của cơ quan cấp."
        confirmLabel="Từ chối yêu cầu"
        onConfirm={reject}
      />
    </>
  );
}

/** Giấy tờ mới đính kèm, đặt cạnh giấy tờ đang dùng cùng loại (nếu có) để dễ đối chiếu. */
function DocumentsCompare({ request, profile }: { request: AgentChangeRequestResponse; profile: AgentProfileResponse }) {
  if (request.documents.length === 0) return null;
  const pending = request.status === "PENDING";

  return (
    <Card className="rounded-2xl">
      <CardHeader>
        <CardTitle className="text-lg">Giấy tờ đính kèm</CardTitle>
        <CardDescription>
          {pending
            ? "Khi duyệt: giấy tờ mới thay thế giấy tờ cùng loại; “Giấy tờ khác” được thêm vào hồ sơ. Link xem có hiệu lực 5 phút."
            : "Link xem có hiệu lực 5 phút."}
        </CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        {!pending && (
          <ul className="flex flex-col gap-2">
            {request.documents.map((document) => (
              <DocumentItem key={document.id} profileId={profile.id} document={document} />
            ))}
          </ul>
        )}
        {pending && request.documents.map((document) => {
          const current =
            document.type !== "OTHER"
              ? profile.documents.find((d) => d.type === document.type && d.status === "ACTIVE")
              : undefined;
          return (
            <div key={document.id} className="grid gap-2 md:grid-cols-2">
              <div>
                <p className="mb-1 text-xs font-medium text-muted-foreground uppercase">Đang dùng</p>
                {current ? (
                  <ul>
                    <DocumentItem profileId={profile.id} document={current} />
                  </ul>
                ) : (
                  <p className="rounded-xl border border-dashed px-3 py-2.5 text-sm text-muted-foreground">
                    {document.type === "OTHER" ? "Bổ sung thêm" : "—"}
                  </p>
                )}
              </div>
              <div>
                <p className="mb-1 text-xs font-medium text-muted-foreground uppercase">Mới</p>
                <ul>
                  <DocumentItem profileId={profile.id} document={document} />
                </ul>
              </div>
            </div>
          );
        })}
      </CardContent>
    </Card>
  );
}
