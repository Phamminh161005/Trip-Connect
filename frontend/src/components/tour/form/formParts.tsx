"use client";

import { Controller, get, useFieldArray, useFormState, type Control, type FieldArrayPath, type FieldValues, type Path } from "react-hook-form";
import { Plus, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Field, FieldDescription, FieldError, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";

/** Nhóm nút bấm chọn nhiều (loại hình tour, phương tiện). Đạt giới hạn thì các nút chưa chọn bị khóa. */
export function ChipMultiSelect<V extends string | number>({
  options,
  value,
  onChange,
  max,
  label,
}: {
  options: { value: V; label: string }[];
  value: V[];
  onChange: (value: V[]) => void;
  max?: number;
  label: string;
}) {
  const full = max !== undefined && value.length >= max;
  return (
    <div className="flex flex-wrap gap-2" role="group" aria-label={label}>
      {options.map((option) => {
        const selected = value.includes(option.value);
        return (
          <button
            key={option.value}
            type="button"
            aria-pressed={selected}
            disabled={!selected && full}
            onClick={() => onChange(selected ? value.filter((v) => v !== option.value) : [...value, option.value])}
            className={cn(
              "rounded-full border px-3.5 py-1.5 text-sm transition-colors outline-none focus-visible:ring-3 focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:opacity-40",
              selected ? "border-primary bg-primary text-primary-foreground" : "bg-card hover:border-primary/50 hover:bg-accent",
            )}
          >
            {option.label}
          </button>
        );
      })}
    </div>
  );
}

/**
 * Danh sách các dòng chữ ngắn có nút thêm / xóa (điểm nổi bật, dịch vụ bao gồm...).
 * Mỗi phần tử trong form có dạng { value: string }.
 */
export function LinesField<T extends FieldValues>({
  control,
  name,
  label,
  description,
  placeholder,
  addLabel,
  min = 0,
  max,
}: {
  control: Control<T>;
  name: FieldArrayPath<T>;
  label: string;
  description?: string;
  placeholder: string;
  addLabel: string;
  min?: number;
  max: number;
}) {
  const { fields, append, remove } = useFieldArray({ control, name });
  // Lỗi của cả danh sách (vd "cần ít nhất 3 dòng"), khác với lỗi của từng dòng
  const { errors } = useFormState({ control, name: name as unknown as Path<T> });
  const listError = get(errors, name) as { message?: string; root?: { message?: string } } | undefined;
  const listMessage = listError?.message ?? listError?.root?.message;

  return (
    <Field data-invalid={Boolean(listMessage)}>
      <FieldLabel>{label}</FieldLabel>
      {description && <FieldDescription>{description}</FieldDescription>}
      <div className="flex flex-col gap-2">
        {fields.map((item, index) => (
          <Controller
            key={item.id}
            control={control}
            name={`${name}.${index}.value` as Path<T>}
            render={({ field, fieldState: lineState }) => (
              <div className="flex flex-col gap-1">
                <div className="flex items-center gap-2">
                  <span className="w-5 shrink-0 text-right text-sm text-muted-foreground">{index + 1}.</span>
                  <Input
                    {...field}
                    value={field.value ?? ""}
                    placeholder={placeholder}
                    aria-invalid={lineState.invalid}
                    aria-label={`${label} dòng ${index + 1}`}
                    className="h-11 rounded-xl"
                  />
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    className="shrink-0 rounded-lg"
                    disabled={fields.length <= min}
                    onClick={() => remove(index)}
                    aria-label={`Xóa dòng ${index + 1}`}
                  >
                    <X />
                  </Button>
                </div>
                {lineState.error && <p className="pl-7 text-sm text-destructive">{lineState.error.message}</p>}
              </div>
            )}
          />
        ))}
      </div>
      {fields.length < max && (
        <Button
          type="button"
          variant="outline"
          size="sm"
          className="w-fit rounded-lg"
          onClick={() => append({ value: "" } as never, { shouldFocus: true })}
        >
          <Plus /> {addLabel}
        </Button>
      )}
      {listMessage && <FieldError>{listMessage}</FieldError>}
    </Field>
  );
}
