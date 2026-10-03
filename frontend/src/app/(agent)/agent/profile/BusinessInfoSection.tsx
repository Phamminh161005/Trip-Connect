"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Pencil } from "lucide-react";
import { toast } from "sonner";
import { AgentBusinessFields } from "@/components/agent/AgentBusinessFields";
import { Button } from "@/components/ui/button";
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Spinner } from "@/components/ui/spinner";
import { updateMyAgentProfile } from "@/lib/api/agent";
import { applyApiError } from "@/lib/form/applyApiError";
import { agentBusinessSchema, type AgentBusinessValues } from "@/lib/validation/auth";
import type { AgentProfileResponse } from "@/types/agent";
import { ChangeRequestButton } from "./ChangeRequestButton";
import { useSetAgentProfile } from "./useAgentProfile";

function toFormValues(profile: AgentProfileResponse): Partial<AgentBusinessValues> {
  return {
    companyName: profile.companyName ?? "",
    taxCode: profile.taxCode ?? "",
    addressProvinceId: profile.addressProvince?.id,
    address: profile.address ?? "",
    bankBin: profile.bank?.bin ?? "",
    bankAccountNumber: profile.bankAccountNumber ?? "",
    bankAccountHolder: profile.bankAccountHolder ?? "",
  };
}

function InfoRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="grid gap-1 py-3 sm:grid-cols-[200px_1fr] sm:gap-4">
      <dt className="text-sm text-muted-foreground">{label}</dt>
      <dd className="text-sm font-medium wrap-break-word">{value || <span className="font-normal text-destructive">Chưa có</span>}</dd>
    </div>
  );
}

export function BusinessInfoSection({ profile, editable }: { profile: AgentProfileResponse; editable: boolean }) {
  const [editing, setEditing] = useState(false);

  return (
    <Card className="rounded-2xl">
      <CardHeader>
        <CardTitle className="text-lg">Thông tin doanh nghiệp</CardTitle>
        <CardDescription>Tên, mã số thuế, địa chỉ trụ sở và tài khoản nhận thanh toán.</CardDescription>
        {editable && !editing && (
          <CardAction>
            <Button variant="outline" size="sm" className="rounded-lg" onClick={() => setEditing(true)}>
              <Pencil /> Chỉnh sửa
            </Button>
          </CardAction>
        )}
        {profile.status === "APPROVED" && (
          <CardAction>
            <ChangeRequestButton profile={profile} />
          </CardAction>
        )}
      </CardHeader>
      <CardContent>
        {editing ? (
          <BusinessInfoForm profile={profile} onDone={() => setEditing(false)} />
        ) : (
          <dl className="divide-y">
            <InfoRow label="Tên công ty / hộ kinh doanh" value={profile.companyName} />
            <InfoRow label="Mã số thuế" value={profile.taxCode} />
            <InfoRow
              label="Địa chỉ trụ sở"
              value={profile.address && profile.addressProvince ? `${profile.address}, ${profile.addressProvince.province}` : null}
            />
            <InfoRow label="Ngân hàng" value={profile.bank ? `${profile.bank.shortName} — ${profile.bank.name}` : null} />
            <InfoRow label="Số tài khoản" value={profile.bankAccountNumber} />
            <InfoRow label="Tên chủ tài khoản" value={profile.bankAccountHolder} />
          </dl>
        )}
      </CardContent>
    </Card>
  );
}

function BusinessInfoForm({ profile, onDone }: { profile: AgentProfileResponse; onDone: () => void }) {
  const { set } = useSetAgentProfile();
  const form = useForm<AgentBusinessValues>({
    resolver: zodResolver(agentBusinessSchema),
    defaultValues: toFormValues(profile),
  });

  const onSubmit = async (values: AgentBusinessValues) => {
    try {
      // Gửi kèm số giấy phép hiện tại: API PUT thay toàn bộ, bỏ trống sẽ làm mất số giấy phép đã nhập
      set(await updateMyAgentProfile({ ...values, businessLicense: profile.businessLicense }));
      toast.success("Đã lưu thông tin doanh nghiệp");
      onDone();
    } catch (error) {
      applyApiError(error, form.setError);
    }
  };

  return (
    <form noValidate onSubmit={form.handleSubmit(onSubmit)} className="flex flex-col gap-6">
      <AgentBusinessFields control={form.control} autoFocus />
      <div className="flex gap-3">
        <Button type="button" variant="outline" className="h-11 rounded-xl px-5" onClick={onDone} disabled={form.formState.isSubmitting}>
          Hủy
        </Button>
        <Button type="submit" className="h-11 rounded-xl px-6 font-semibold" disabled={form.formState.isSubmitting}>
          {form.formState.isSubmitting && <Spinner />}
          Lưu thay đổi
        </Button>
      </div>
    </form>
  );
}
