"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { TextField } from "@/components/form/TextField";
import { Button } from "@/components/ui/button";
import { FieldGroup } from "@/components/ui/field";
import { Spinner } from "@/components/ui/spinner";
import { emailOnlySchema, type EmailOnlyValues } from "@/lib/validation/auth";

/** Form chỉ có 1 ô email (quên mật khẩu, hoặc khi mở trang OTP mà thiếu email trên đường dẫn). */
export function EmailStep({
  defaultEmail,
  submitLabel,
  onSubmit,
}: {
  defaultEmail?: string;
  submitLabel: string;
  onSubmit: (email: string) => Promise<void> | void;
}) {
  const form = useForm<EmailOnlyValues>({
    resolver: zodResolver(emailOnlySchema),
    defaultValues: { email: defaultEmail ?? "" },
  });

  return (
    <form noValidate onSubmit={form.handleSubmit((values) => onSubmit(values.email))}>
      <FieldGroup className="gap-4">
        <TextField control={form.control} name="email" label="Email" type="email" autoComplete="email" autoFocus />
        <Button type="submit" size="lg" className="h-12 rounded-xl text-base font-semibold" disabled={form.formState.isSubmitting}>
          {form.formState.isSubmitting && <Spinner />}
          {submitLabel}
        </Button>
      </FieldGroup>
    </form>
  );
}
