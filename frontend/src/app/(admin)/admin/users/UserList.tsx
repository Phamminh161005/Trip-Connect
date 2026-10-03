"use client";

import { useState } from "react";
import Link from "next/link";
import { useQueryClient } from "@tanstack/react-query";
import { Ban, RotateCcw, Store } from "lucide-react";
import { toast } from "sonner";
import { ListState } from "@/components/common/ListState";
import { PageHeader } from "@/components/common/PageHeader";
import { PaginationBar } from "@/components/common/PaginationBar";
import { ReasonDialog } from "@/components/common/ReasonDialog";
import { SearchBox } from "@/components/common/SearchBox";
import { ActiveBadge, USER_ROLE_LABELS } from "@/components/admin/StatusBadges";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { usePagedQuery } from "@/hooks/usePagedQuery";
import { useSearchParamsState } from "@/hooks/useSearchParamsState";
import { activateUser, deactivateUser, listUsers } from "@/lib/api/admin";
import { errorMessage } from "@/lib/api/errors";
import { formatDateTime } from "@/lib/format";
import type { AdminUserResponse } from "@/types/admin";
import type { UserRole } from "@/types/auth";
import { ADMIN_KEY } from "../adminQueries";

const PAGE_SIZE = 20;
const ALL = "ALL"; // Select của Radix không cho giá trị rỗng -> dùng "ALL" thay cho "không lọc"
const ROLES: UserRole[] = ["CUSTOMER", "AGENT", "ADMIN"];
const ACTIVE_OPTIONS = [
  { value: "true", label: "Đang hoạt động" },
  { value: "false", label: "Đã vô hiệu hóa" },
];

export function UserList() {
  const queryClient = useQueryClient();
  const params = useSearchParamsState();
  const keyword = params.get("q") ?? "";
  const role = ROLES.find((r) => r === params.get("role"));
  const active = ACTIVE_OPTIONS.find((o) => o.value === params.get("active"))?.value;
  const page = params.page;

  const [deactivating, setDeactivating] = useState<AdminUserResponse | null>(null);
  const [activating, setActivating] = useState<AdminUserResponse | null>(null);

  const filters = { q: keyword, role, active: active === undefined ? undefined : active === "true", size: PAGE_SIZE };
  const setPage = (next: number) => params.set({ page: next || undefined }, { resetPage: false });
  const query = usePagedQuery({
    queryKey: [...ADMIN_KEY, "users"],
    page,
    paramsFor: (p) => ({ ...filters, page: p }),
    queryFn: listUsers,
    onPageChange: setPage,
  });
  const rows = query.data?.content ?? [];

  const refresh = () => queryClient.invalidateQueries({ queryKey: ADMIN_KEY });

  const deactivate = async (reason: string) => {
    if (!deactivating) return;
    try {
      await deactivateUser(deactivating.id, reason);
      toast.success(`Đã vô hiệu hóa tài khoản ${deactivating.email}`);
      await refresh();
    } catch (error) {
      toast.error(errorMessage(error));
      throw error;
    }
  };

  const activate = async () => {
    if (!activating) return;
    try {
      await activateUser(activating.id);
      toast.success(`Đã kích hoạt lại tài khoản ${activating.email}`);
      await refresh();
    } catch (error) {
      toast.error(errorMessage(error));
      throw error;
    }
  };

  return (
    <>
      <PageHeader title="Người dùng" description="Tìm kiếm tài khoản, vô hiệu hóa hoặc kích hoạt lại khi cần." />
      <Card className="rounded-2xl">
        <CardContent className="flex flex-col gap-4">
          <div className="flex flex-col gap-3 md:flex-row md:items-center">
            <SearchBox value={keyword} onSearch={(q) => params.set({ q })} placeholder="Tìm theo tên hoặc email" />
            <div className="flex gap-3">
              <Select value={role ?? ALL} onValueChange={(value) => params.set({ role: value === ALL ? undefined : value })}>
                <SelectTrigger className="h-10! w-40 rounded-xl" aria-label="Lọc theo vai trò">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={ALL}>Mọi vai trò</SelectItem>
                  {ROLES.map((r) => (
                    <SelectItem key={r} value={r}>
                      {USER_ROLE_LABELS[r]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <Select value={active ?? ALL} onValueChange={(value) => params.set({ active: value === ALL ? undefined : value })}>
                <SelectTrigger className="h-10! w-44 rounded-xl" aria-label="Lọc theo trạng thái">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={ALL}>Mọi trạng thái</SelectItem>
                  {ACTIVE_OPTIONS.map((o) => (
                    <SelectItem key={o.value} value={o.value}>
                      {o.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          <ListState
            isPending={query.isPending}
            error={query.error}
            isEmpty={rows.length === 0}
            emptyText="Không tìm thấy người dùng phù hợp."
          />

          {rows.length > 0 && (
            <>
              <Table className={query.isPlaceholderData ? "opacity-60" : undefined}>
                <TableHeader>
                  <TableRow>
                    <TableHead>Người dùng</TableHead>
                    <TableHead className="hidden md:table-cell">Vai trò</TableHead>
                    <TableHead>Trạng thái</TableHead>
                    <TableHead className="hidden lg:table-cell">Đăng nhập gần nhất</TableHead>
                    <TableHead className="text-right">Thao tác</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {rows.map((user) => (
                    <TableRow key={user.id}>
                      <TableCell className="max-w-72">
                        <p className="truncate font-medium">{user.fullName ?? "—"}</p>
                        <p className="truncate text-xs text-muted-foreground">
                          {user.email}
                          {user.phone && ` · ${user.phone}`}
                        </p>
                      </TableCell>
                      <TableCell className="hidden md:table-cell">
                        <Badge variant="secondary">{USER_ROLE_LABELS[user.role]}</Badge>
                      </TableCell>
                      <TableCell className="max-w-56">
                        <ActiveBadge active={user.active} />
                        {!user.active && user.deactivatedReason && (
                          <p className="mt-1 truncate text-xs text-muted-foreground" title={user.deactivatedReason}>
                            {user.deactivatedReason}
                          </p>
                        )}
                      </TableCell>
                      <TableCell className="hidden text-muted-foreground lg:table-cell">
                        {user.lastLoginAt ? formatDateTime(user.lastLoginAt) : "Chưa đăng nhập"}
                      </TableCell>
                      <TableCell>
                        <div className="flex justify-end gap-2">
                          {user.agentProfileId && (
                            <Button asChild variant="ghost" size="sm" className="rounded-lg">
                              <Link href={`/admin/agents/${user.agentProfileId}`}>
                                <Store /> Hồ sơ
                              </Link>
                            </Button>
                          )}
                          {user.role !== "ADMIN" &&
                            (user.active ? (
                              <Button
                                variant="outline"
                                size="sm"
                                className="rounded-lg text-destructive hover:text-destructive"
                                onClick={() => setDeactivating(user)}
                              >
                                <Ban /> Vô hiệu hóa
                              </Button>
                            ) : (
                              <Button variant="outline" size="sm" className="rounded-lg" onClick={() => setActivating(user)}>
                                <RotateCcw /> Kích hoạt lại
                              </Button>
                            ))}
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
              <PaginationBar
                page={page}
                size={PAGE_SIZE}
                totalElements={query.data?.totalElements ?? 0}
                totalPages={query.data?.totalPages ?? 0}
                onPageChange={setPage}
              />
            </>
          )}
        </CardContent>
      </Card>

      <ReasonDialog
        open={deactivating !== null}
        onOpenChange={(open) => !open && setDeactivating(null)}
        title="Vô hiệu hóa tài khoản?"
        description={`${deactivating?.email ?? ""} sẽ không thể đăng nhập cho tới khi được kích hoạt lại (phiên đang mở tự hết hạn trong vài phút). Người dùng nhận email kèm lý do.`}
        label="Lý do vô hiệu hóa"
        placeholder="Ví dụ: Vi phạm điều khoản sử dụng — đăng thông tin sai sự thật."
        confirmLabel="Vô hiệu hóa"
        onConfirm={deactivate}
      />
      <ConfirmDialog
        open={activating !== null}
        onOpenChange={(open) => !open && setActivating(null)}
        title="Kích hoạt lại tài khoản?"
        description={`${activating?.email ?? ""} sẽ đăng nhập và sử dụng TripConnect bình thường trở lại.`}
        confirmLabel="Kích hoạt lại"
        onConfirm={activate}
      />
    </>
  );
}
