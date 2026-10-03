"use client";

import { Controller, type Control, type FieldPath, type FieldValues } from "react-hook-form";
import { REGEXP_ONLY_DIGITS } from "input-otp";
import { Field, FieldError, FieldLabel } from "@/components/ui/field";
import { InputOTP, InputOTPGroup, InputOTPSlot } from "@/components/ui/input-otp";

/** 6 ô nhập mã OTP (chỉ nhận số, dán cả mã từ email vào được). */
export function OtpField<T extends FieldValues>({ control, name }: { control: Control<T>; name: FieldPath<T> }) {
  return (
    <Controller
      control={control}
      name={name}
      render={({ field, fieldState }) => (
        <Field data-invalid={fieldState.invalid} className="items-center">
          <FieldLabel htmlFor={name} className="sr-only">
            Mã OTP
          </FieldLabel>
          <InputOTP
            id={name}
            maxLength={6}
            pattern={REGEXP_ONLY_DIGITS}
            value={field.value ?? ""}
            onChange={field.onChange}
            onBlur={field.onBlur}
            autoFocus
            aria-invalid={fieldState.invalid}
            containerClassName="justify-center"
          >
            <InputOTPGroup>
              {Array.from({ length: 6 }, (_, i) => (
                <InputOTPSlot key={i} index={i} className="size-12 text-lg" />
              ))}
            </InputOTPGroup>
          </InputOTP>
          {fieldState.invalid && <FieldError className="text-center" errors={[fieldState.error]} />}
        </Field>
      )}
    />
  );
}
