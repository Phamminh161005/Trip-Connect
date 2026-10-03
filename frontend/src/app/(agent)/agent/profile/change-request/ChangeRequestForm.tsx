"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Controller, useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useQueryClient } from "@tanstack/react-query";
import { Plus, Send, X } from "lucide-react";
import { toast } from "sonner";
import { useBankOptions, useProvinceOptions } from "@/components/agent/useCatalogOptions";
import { SearchableSelect } from "@/components/form/SearchableSelect";
import { TextField } from "@/components/form/TextField";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { Spinner } from "@/components/ui/spinner";
import { Textarea } from "@/components/ui/textarea";
import { MAX_OTHER_DOCUMENTS, REQUIRED_DOCUMENTS, checkDocumentFile } from "@/lib/agent/documents";
import { createChangeRequest } from "@/lib/api/agent";
import { ApiError } from "@/lib/api/errors";
import { applyApiError } from "@/lib/form/applyApiError";
import { toBankHolderName } from "@/lib/validation/auth";
import type { AgentDocumentType, AgentProfileResponse } from "@/types/agent";
import { AGENT_PROFILE_KEY } from "../useAgentProfile";
import { CHANGE_REQUESTS_KEY } from "../ChangeRequestHistory";
import { changeRequestSchema, type ChangeRequestValues } from "./changeRequestSchema";
import { StagedFilePicker } from "./StagedFilePicker";

type RequiredType = (typeof REQUIRED_DOCUMENTS)[number]["type"];

// Tên trường file trong AgentChangeRequestForm của Backend
const FILE_FIELD: Record<RequiredType, string> = {
  TRAVEL_LICENSE: "travelLicense",
  BUSINESS_REGISTRATION: "businessRegistration",
  REPRESENTATIVE_ID_FRONT: "representativeIdFront",
  REPRESENTATIVE_ID_BACK: "representativeIdBack",
};

const same = (a: string | null | undefined, b: string | null | undefined) => (a ?? "").trim() === (b ?? "").trim();

export function ChangeRequestForm({ profile }: { profile: AgentProfileResponse }) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const provinces = useProvinceOptions();
  const banks = useBankOptions();

  const [files, setFiles] = useState<Partial<Record<RequiredType, File>>>({});
  const [otherFiles, setOtherFiles] = useState<File[]>([]);

  const form = useForm<ChangeRequestValues>({
    resolver: zodResolver(changeRequestSchema),
    // Điền sẵn thông tin HIỆN TẠI -> người dùng chỉ sửa chỗ muốn đổi
    defaultValues: {
      companyName: profile.companyName ?? "",
      taxCode: profile.taxCode ?? "",
      businessLicense: profile.businessLicense ?? "",
      addressProvinceId: profile.addressProvince?.id,
      address: profile.address ?? "",
      changeBank: false,
      bankBin: "",
      bankAccountNumber: "",
      bankAccountHolder: "",
      currentPassword: "",
      note: "",
    },
  });

  const values = useWatch({ control: form.control });
  const changeBank = values.changeBank ?? false;
  const existingOthers = profile.documents.filter((d) => d.type === "OTHER").length;
  const otherSlots = MAX_OTHER_DOCUMENTS - existingOthers;

  // Danh sách thay đổi sẽ gửi (hiện cho người dùng biết mình đang yêu cầu đổi gì)
  const changes: string[] = [];
  if (!same(values.companyName, profile.companyName)) changes.push("Tên công ty");
  if (!same(values.taxCode, profile.taxCode)) changes.push("Mã số thuế");
  if (!same(values.businessLicense, profile.businessLicense)) changes.push("Số giấy phép lữ hành");
  if (values.addressProvinceId !== profile.addressProvince?.id || !same(values.address, profile.address)) {
    changes.push("Địa chỉ trụ sở");
  }
  if (changeBank) changes.push("Tài khoản ngân hàng");
  REQUIRED_DOCUMENTS.forEach(({ type, label }) => files[type] && changes.push(label));
  if (otherFiles.length) changes.push(`${otherFiles.length} giấy tờ khác`);

  const onSubmit = async (data: ChangeRequestValues) => {
    if (changes.length === 0) {
      toast.error("Bạn chưa thay đổi thông tin nào");
      return;
    }

    // Chỉ gửi những trường THỰC SỰ thay đổi (Backend: trường trống = không đổi)
    const body = new FormData();
    if (!same(data.companyName, profile.companyName)) body.append("companyName", data.companyName);
    if (!same(data.taxCode, profile.taxCode)) body.append("taxCode", data.taxCode);
    if (!same(data.businessLicense, profile.businessLicense)) body.append("businessLicense", data.businessLicense);
    if (data.addressProvinceId !== profile.addressProvince?.id) body.append("addressProvinceId", String(data.addressProvinceId));
    if (!same(data.address, profile.address)) body.append("address", data.address);
    if (data.changeBank) {
      body.append("bankBin", data.bankBin);
      body.append("bankAccountNumber", data.bankAccountNumber);
      body.append("bankAccountHolder", data.bankAccountHolder);
      body.append("currentPassword", data.currentPassword);
    }
    if (data.note) body.append("note", data.note);
    (Object.entries(files) as [RequiredType, File][]).forEach(([type, file]) => body.append(FILE_FIELD[type], file));
    otherFiles.forEach((file) => body.append("otherDocuments", file));

    try {
      await createChangeRequest(body);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: AGENT_PROFILE_KEY }),
        queryClient.invalidateQueries({ queryKey: CHANGE_REQUESTS_KEY }),
      ]);
      toast.success("Đã gửi yêu cầu cập nhật. Kết quả sẽ được gửi qua email.");
      router.push("/agent/profile#change-requests");
    } catch (error) {
      if (error instanceof ApiError && error.status === 400 && /mật khẩu/i.test(error.message)) {
        form.setError("currentPassword", { type: "server", message: error.message }, { shouldFocus: true });
        return;
      }
      applyApiError(error, form.setError);
    }
  };

  const addOtherFile = (file: File | undefined) => {
    if (!file) return;
    const problem = checkDocumentFile(file);
    if (problem) toast.error(problem);
    else setOtherFiles((list) => [...list, file]);
  };

  return (
    <form noValidate onSubmit={form.handleSubmit(onSubmit)} className="flex flex-col gap-6">
      {/* ----- Thông tin doanh nghiệp ----- */}
      <Card className="rounded-2xl">
        <CardHeader>
          <CardTitle className="text-lg">Thông tin doanh nghiệp</CardTitle>
          <CardDescription>Đang hiển thị thông tin hiện tại — chỉ sửa những ô cần thay đổi.</CardDescription>
        </CardHeader>
        <CardContent>
          <FieldGroup className="gap-4">
            <TextField control={form.control} name="companyName" label="Tên công ty / hộ kinh doanh" />
            <div className="grid gap-4 sm:grid-cols-2">
              <TextField control={form.control} name="taxCode" label="Mã số thuế" />
              <TextField control={form.control} name="businessLicense" label="Số giấy phép lữ hành" />
            </div>
            <Controller
              control={form.control}
              name="addressProvinceId"
              render={({ field, fieldState }) => (
                <Field data-invalid={fieldState.invalid}>
                  <FieldLabel htmlFor="addressProvinceId">Tỉnh / Thành phố của trụ sở</FieldLabel>
                  {provinces.isPending ? (
                    <Skeleton className="h-12 rounded-xl" />
                  ) : (
                    <SearchableSelect
                      id="addressProvinceId"
                      options={provinces.options}
                      value={field.value}
                      onChange={field.onChange}
                      onBlur={field.onBlur}
                      placeholder="Chọn Tỉnh / Thành phố"
                      searchPlaceholder="Gõ để tìm, ví dụ: Hà Nội"
                      emptyText="Không tìm thấy tỉnh/thành"
                      invalid={fieldState.invalid}
                    />
                  )}
                  {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
                </Field>
              )}
            />
            <TextField control={form.control} name="address" label="Địa chỉ chi tiết" placeholder="Số nhà, tên đường, phường/xã" />
          </FieldGroup>
        </CardContent>
      </Card>

      {/* ----- Tài khoản ngân hàng ----- */}
      <Card className="rounded-2xl">
        <CardHeader>
          <CardTitle className="text-lg">Tài khoản ngân hàng nhận thanh toán</CardTitle>
          <CardDescription>
            Hiện tại: {profile.bank?.shortName} · {profile.bankAccountNumber} · {profile.bankAccountHolder}
          </CardDescription>
        </CardHeader>
        <CardContent>
          <FieldGroup className="gap-4">
            <Controller
              control={form.control}
              name="changeBank"
              render={({ field }) => (
                <Field orientation="horizontal">
                  <Checkbox id="changeBank" checked={field.value} onCheckedChange={(v) => field.onChange(v === true)} />
                  <FieldLabel htmlFor="changeBank" className="font-normal">
                    Tôi muốn đổi tài khoản ngân hàng nhận thanh toán
                  </FieldLabel>
                </Field>
              )}
            />

            {changeBank && (
              <>
                <Controller
                  control={form.control}
                  name="bankBin"
                  render={({ field, fieldState }) => (
                    <Field data-invalid={fieldState.invalid}>
                      <FieldLabel htmlFor="bankBin">Ngân hàng mới</FieldLabel>
                      {banks.isPending ? (
                        <Skeleton className="h-12 rounded-xl" />
                      ) : (
                        <SearchableSelect
                          id="bankBin"
                          options={banks.options}
                          value={field.value || null}
                          onChange={field.onChange}
                          onBlur={field.onBlur}
                          placeholder="Chọn ngân hàng"
                          searchPlaceholder="Gõ để tìm, ví dụ: Vietcombank"
                          emptyText="Không tìm thấy ngân hàng"
                          invalid={fieldState.invalid}
                        />
                      )}
                      {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
                    </Field>
                  )}
                />
                <div className="grid gap-4 sm:grid-cols-2">
                  <Controller
                    control={form.control}
                    name="bankAccountNumber"
                    render={({ field, fieldState }) => (
                      <Field data-invalid={fieldState.invalid}>
                        <FieldLabel htmlFor="bankAccountNumber">Số tài khoản mới</FieldLabel>
                        <Input
                          {...field}
                          id="bankAccountNumber"
                          inputMode="numeric"
                          aria-invalid={fieldState.invalid}
                          className="h-12 rounded-xl text-base"
                          onChange={(e) => field.onChange(e.target.value.replace(/\D/g, ""))}
                        />
                        {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
                      </Field>
                    )}
                  />
                  <Controller
                    control={form.control}
                    name="bankAccountHolder"
                    render={({ field, fieldState }) => (
                      <Field data-invalid={fieldState.invalid}>
                        <FieldLabel htmlFor="bankAccountHolder">Tên chủ tài khoản</FieldLabel>
                        <Input
                          {...field}
                          id="bankAccountHolder"
                          aria-invalid={fieldState.invalid}
                          className="h-12 rounded-xl text-base uppercase"
                          onChange={(e) => field.onChange(toBankHolderName(e.target.value))}
                        />
                        {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
                      </Field>
                    )}
                  />
                </div>
                <TextField
                  control={form.control}
                  name="currentPassword"
                  label="Mật khẩu hiện tại"
                  type="password"
                  autoComplete="current-password"
                  description="Xác nhận chính bạn đang đổi nơi nhận tiền (phòng trường hợp người khác chiếm phiên đăng nhập)."
                />
              </>
            )}
          </FieldGroup>
        </CardContent>
      </Card>

      {/* ----- Giấy tờ thay thế ----- */}
      <Card className="rounded-2xl">
        <CardHeader>
          <CardTitle className="text-lg">Giấy tờ thay thế</CardTitle>
          <CardDescription>
            Chỉ chọn file cho giấy tờ cần thay. Giấy tờ cũ được lưu trữ lại sau khi yêu cầu được duyệt.
          </CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-3">
          {REQUIRED_DOCUMENTS.map(({ type, label }) => (
            <StagedFilePicker
              key={type}
              label={label}
              currentFilename={profile.documents.find((d) => d.type === (type as AgentDocumentType))?.originalFilename}
              file={files[type] ?? null}
              onChange={(file) =>
                setFiles((current) => {
                  const next = { ...current };
                  if (file) next[type] = file;
                  else delete next[type];
                  return next;
                })
              }
            />
          ))}

          <div className="rounded-2xl border border-dashed p-4">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div>
                <p className="font-medium">Thêm giấy tờ khác</p>
                <p className="text-sm text-muted-foreground">Còn thêm được {Math.max(otherSlots - otherFiles.length, 0)} file.</p>
              </div>
              {otherFiles.length < otherSlots && (
                <label className="inline-flex cursor-pointer items-center gap-1.5 rounded-lg border px-3 py-1.5 text-sm font-medium hover:bg-muted">
                  <Plus className="size-4" /> Chọn file
                  <input
                    type="file"
                    accept="application/pdf,image/jpeg,image/png"
                    className="hidden"
                    onChange={(e) => {
                      addOtherFile(e.target.files?.[0]);
                      e.target.value = "";
                    }}
                  />
                </label>
              )}
            </div>
            {otherFiles.length > 0 && (
              <ul className="mt-3 flex flex-col gap-2">
                {otherFiles.map((file, index) => (
                  <li key={`${file.name}-${index}`} className="flex items-center justify-between gap-2 rounded-xl bg-muted/50 px-3 py-2 text-sm">
                    <span className="truncate">{file.name}</span>
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      className="rounded-lg"
                      onClick={() => setOtherFiles((list) => list.filter((_, i) => i !== index))}
                    >
                      <X /> Bỏ
                    </Button>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </CardContent>
      </Card>

      {/* ----- Ghi chú + gửi ----- */}
      <Card className="rounded-2xl">
        <CardContent className="flex flex-col gap-5">
          <Controller
            control={form.control}
            name="note"
            render={({ field, fieldState }) => (
              <Field data-invalid={fieldState.invalid}>
                <FieldLabel htmlFor="note">Ghi chú cho quản trị viên (không bắt buộc)</FieldLabel>
                <Textarea
                  {...field}
                  id="note"
                  rows={3}
                  placeholder="Ví dụ: Công ty chuyển trụ sở từ ngày 01/10/2026"
                  aria-invalid={fieldState.invalid}
                  className="rounded-xl text-base"
                />
                <FieldDescription>Giúp quản trị viên hiểu lý do thay đổi để duyệt nhanh hơn.</FieldDescription>
                {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
              </Field>
            )}
          />

          <div className="rounded-xl bg-muted/50 px-4 py-3 text-sm">
            {changes.length ? (
              <>
                <span className="font-medium">Sẽ gửi yêu cầu thay đổi: </span>
                {changes.join(", ")}
              </>
            ) : (
              <span className="text-muted-foreground">Chưa có thay đổi nào.</span>
            )}
          </div>

          <div className="flex flex-wrap gap-3">
            <Button
              type="button"
              variant="outline"
              className="h-11 rounded-xl px-5"
              onClick={() => router.push("/agent/profile")}
              disabled={form.formState.isSubmitting}
            >
              Hủy
            </Button>
            <Button
              type="submit"
              className="h-11 rounded-xl px-6 font-semibold"
              disabled={form.formState.isSubmitting || changes.length === 0}
            >
              {form.formState.isSubmitting ? <Spinner /> : <Send />}
              Gửi yêu cầu
            </Button>
          </div>
        </CardContent>
      </Card>
    </form>
  );
}
