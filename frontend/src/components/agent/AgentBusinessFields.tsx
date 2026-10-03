"use client";

import { Controller, type Control } from "react-hook-form";
import { SearchableSelect } from "@/components/form/SearchableSelect";
import { TextField } from "@/components/form/TextField";
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel, FieldLegend, FieldSet } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { useBankOptions, useProvinceOptions } from "./useCatalogOptions";
import { toBankHolderName, type AgentBusinessValues } from "@/lib/validation/auth";

/**
 * Các ô "Thông tin doanh nghiệp" — dùng chung cho form đăng ký Agent và trang Hồ sơ kinh doanh.
 * Chỉ chứa ô nhập; form bọc ngoài tự lo nút bấm và việc gửi dữ liệu.
 */
export function AgentBusinessFields({ control, autoFocus }: { control: Control<AgentBusinessValues>; autoFocus?: boolean }) {
  const { options: provinceOptions, isPending: provincesPending } = useProvinceOptions();
  const { options: bankOptions, isPending: banksPending } = useBankOptions();

  return (
    <FieldGroup className="gap-6">
      <FieldGroup className="gap-4">
        <TextField control={control} name="companyName" label="Tên công ty / hộ kinh doanh" autoComplete="organization" autoFocus={autoFocus} />
        <TextField
          control={control}
          name="taxCode"
          label="Mã số thuế"
          placeholder="0101234567"
          description="10 số (doanh nghiệp), 10 số-3 số (chi nhánh) hoặc 12 số (hộ kinh doanh)."
        />
      </FieldGroup>

      {/* ----- Địa chỉ trụ sở ----- */}
      <FieldSet>
        <FieldLegend variant="label">Địa chỉ trụ sở</FieldLegend>
        <FieldGroup className="gap-3">
          <Controller
            control={control}
            name="addressProvinceId"
            render={({ field, fieldState }) => (
              <Field data-invalid={fieldState.invalid}>
                <FieldLabel htmlFor="addressProvinceId" className="sr-only">
                  Tỉnh / Thành phố
                </FieldLabel>
                {provincesPending ? (
                  <Skeleton className="h-12 rounded-xl" />
                ) : (
                  <SearchableSelect
                    id="addressProvinceId"
                    options={provinceOptions}
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
          <TextField control={control} name="address" label="Địa chỉ chi tiết" placeholder="Số nhà, tên đường, phường/xã" autoComplete="street-address" />
        </FieldGroup>
      </FieldSet>

      {/* ----- Tài khoản ngân hàng ----- */}
      <FieldSet>
        <FieldLegend variant="label">Tài khoản ngân hàng nhận thanh toán</FieldLegend>
        <FieldDescription>Dùng để TripConnect chuyển tiền quyết toán cho bạn.</FieldDescription>
        <FieldGroup className="gap-3">
          <Controller
            control={control}
            name="bankBin"
            render={({ field, fieldState }) => (
              <Field data-invalid={fieldState.invalid}>
                <FieldLabel htmlFor="bankBin">Ngân hàng</FieldLabel>
                {banksPending ? (
                  <Skeleton className="h-12 rounded-xl" />
                ) : (
                  <SearchableSelect
                    id="bankBin"
                    options={bankOptions}
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
          <div className="grid gap-3 sm:grid-cols-2">
            <Controller
              control={control}
              name="bankAccountNumber"
              render={({ field, fieldState }) => (
                <Field data-invalid={fieldState.invalid}>
                  <FieldLabel htmlFor="bankAccountNumber">Số tài khoản</FieldLabel>
                  <Input
                    {...field}
                    id="bankAccountNumber"
                    inputMode="numeric"
                    placeholder="0123456789"
                    aria-invalid={fieldState.invalid}
                    className="h-12 rounded-xl text-base"
                    // Chỉ giữ lại chữ số (người dùng hay dán kèm dấu cách / gạch ngang)
                    onChange={(e) => field.onChange(e.target.value.replace(/\D/g, ""))}
                  />
                  {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
                </Field>
              )}
            />
            <Controller
              control={control}
              name="bankAccountHolder"
              render={({ field, fieldState }) => (
                <Field data-invalid={fieldState.invalid}>
                  <FieldLabel htmlFor="bankAccountHolder">Tên chủ tài khoản</FieldLabel>
                  <Input
                    {...field}
                    id="bankAccountHolder"
                    placeholder="CONG TY TNHH DU LICH ABC"
                    aria-invalid={fieldState.invalid}
                    className="h-12 rounded-xl text-base uppercase"
                    // Tự chuyển sang CHỮ IN HOA KHÔNG DẤU giống cách ngân hàng hiển thị
                    onChange={(e) => field.onChange(toBankHolderName(e.target.value))}
                  />
                  {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
                </Field>
              )}
            />
          </div>
        </FieldGroup>
      </FieldSet>
    </FieldGroup>
  );
}
