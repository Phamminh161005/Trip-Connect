"use client";

import type { ReactNode } from "react";
import { Controller, type Control, type FieldPath, type FieldValues } from "react-hook-form";
import { Field, FieldDescription, FieldError, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { PasswordInput } from "./PasswordInput";

interface TextFieldProps<T extends FieldValues> {
  control: Control<T>;
  name: FieldPath<T>;
  label: string;
  type?: "text" | "email" | "tel" | "password";
  placeholder?: string;
  autoComplete?: string;
  description?: ReactNode;
  disabled?: boolean;
  autoFocus?: boolean;
}

/** Ô nhập chữ gắn với react-hook-form: nhãn + ô nhập + lỗi (ngay bên dưới ô). */
export function TextField<T extends FieldValues>({
  control,
  name,
  label,
  type = "text",
  placeholder,
  autoComplete,
  description,
  disabled,
  autoFocus,
}: TextFieldProps<T>) {
  return (
    <Controller
      control={control}
      name={name}
      render={({ field, fieldState }) => {
        const inputProps = {
          ...field,
          value: field.value ?? "",
          id: name,
          placeholder,
          autoComplete,
          disabled,
          autoFocus,
          "aria-invalid": fieldState.invalid,
          className: "h-12 rounded-xl text-base",
        };
        return (
          <Field data-invalid={fieldState.invalid}>
            <FieldLabel htmlFor={name}>{label}</FieldLabel>
            {type === "password" ? <PasswordInput {...inputProps} /> : <Input type={type} {...inputProps} />}
            {description && <FieldDescription>{description}</FieldDescription>}
            {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
          </Field>
        );
      }}
    />
  );
}
