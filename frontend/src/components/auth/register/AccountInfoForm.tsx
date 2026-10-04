"use client";

import { useEffect } from "react";
import { useForm, type UseFormSetError } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { TextField } from "@/components/form/TextField";
import { Button } from "@/components/ui/button";
import { FieldGroup } from "@/components/ui/field";
import { Spinner } from "@/components/ui/spinner";
import { accountInfoSchema, type AccountInfoValues } from "@/lib/validation/auth";
import { focusNextOnEnter } from "@/lib/form/focusNextOnEnter";

interface AccountInfoFormProps {
  defaultValues?: Partial<AccountInfoValues>;
  submitLabel: string;
  /** Lỗi email do Backend báo ở bước sau (vd: email đã được sử dụng) -> hiện lại ngay dưới ô email. */
  emailError?: string | null;
  onSubmit: (values: AccountInfoValues, setError: UseFormSetError<AccountInfoValues>) => Promise<void> | void;
}

/** Thông tin tài khoản: họ tên, email, số điện thoại, mật khẩu. */
export function AccountInfoForm({ defaultValues, submitLabel, emailError, onSubmit }: AccountInfoFormProps) {
  const form = useForm<AccountInfoValues>({
    resolver: zodResolver(accountInfoSchema),
    defaultValues: { fullName: "", email: "", phone: "", password: "", confirmPassword: "", ...defaultValues },
  });

  useEffect(() => {
    if (emailError) form.setError("email", { type: "server", message: emailError }, { shouldFocus: true });
  }, [emailError, form]);

  return (
    <form noValidate onKeyDown={focusNextOnEnter} onSubmit={form.handleSubmit((values) => onSubmit(values, form.setError))}>
      <FieldGroup className="gap-4">
        <TextField control={form.control} name="fullName" label="Họ và tên" autoComplete="name" autoFocus />
        <TextField control={form.control} name="email" label="Email" type="email" autoComplete="email" />
        <TextField control={form.control} name="phone" label="Số điện thoại" type="tel" autoComplete="tel" placeholder="0901234567" />
        <TextField
          control={form.control}
          name="password"
          label="Mật khẩu"
          type="password"
          autoComplete="new-password"
          description="Ít nhất 8 ký tự, gồm chữ hoa, chữ thường và số."
        />
        <TextField control={form.control} name="confirmPassword" label="Nhập lại mật khẩu" type="password" autoComplete="new-password" />
        <Button type="submit" size="lg" className="mt-2 h-12 rounded-xl text-base font-semibold" disabled={form.formState.isSubmitting}>
          {form.formState.isSubmitting && <Spinner />}
          {submitLabel}
        </Button>
      </FieldGroup>
    </form>
  );
}
