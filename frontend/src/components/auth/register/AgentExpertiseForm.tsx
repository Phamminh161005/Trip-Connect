"use client";

import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useQuery } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Skeleton } from "@/components/ui/skeleton";
import { Spinner } from "@/components/ui/spinner";
import { getLocations, getTourCategories } from "@/lib/api/catalog";
import { cn } from "@/lib/utils";
import { agentExpertiseSchema, type AgentExpertiseValues } from "@/lib/validation/auth";
import { LocationMultiSelect } from "./LocationMultiSelect";

interface AgentExpertiseFormProps {
  defaultValues?: Partial<AgentExpertiseValues>;
  submitLabel: string;
  /** Không truyền = không có nút "Quay lại" (dùng ở trang Hồ sơ kinh doanh). */
  onBack?: () => void;
  onSubmit: (values: AgentExpertiseValues) => Promise<void>;
}

/** Khu vực phụ trách + loại hình tour thế mạnh — dùng để hệ thống phân bổ yêu cầu tư vấn phù hợp. */
export function AgentExpertiseForm({ defaultValues, submitLabel, onBack, onSubmit }: AgentExpertiseFormProps) {
  const locationsQuery = useQuery({ queryKey: ["locations"], queryFn: getLocations, staleTime: Infinity });
  const categoriesQuery = useQuery({ queryKey: ["tour-categories"], queryFn: getTourCategories, staleTime: Infinity });

  const form = useForm<AgentExpertiseValues>({
    resolver: zodResolver(agentExpertiseSchema),
    defaultValues: { locationIds: [], categoryIds: [], ...defaultValues },
  });

  return (
    <form noValidate onSubmit={form.handleSubmit(onSubmit)}>
      <FieldGroup className="gap-6">
        <Controller
          control={form.control}
          name="locationIds"
          render={({ field, fieldState }) => (
            <Field data-invalid={fieldState.invalid}>
              <FieldLabel htmlFor="locationIds">Khu vực phụ trách</FieldLabel>
              <FieldDescription>Các tỉnh/thành, quốc gia mà bạn tổ chức tour.</FieldDescription>
              {locationsQuery.isPending ? (
                <Skeleton className="h-12 rounded-xl" />
              ) : (
                <LocationMultiSelect
                  id="locationIds"
                  locations={locationsQuery.data ?? []}
                  value={field.value}
                  onChange={field.onChange}
                  invalid={fieldState.invalid}
                />
              )}
              {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
            </Field>
          )}
        />

        <Controller
          control={form.control}
          name="categoryIds"
          render={({ field, fieldState }) => (
            <Field data-invalid={fieldState.invalid}>
              <FieldLabel>Loại hình tour thế mạnh</FieldLabel>
              {categoriesQuery.isPending ? (
                <Skeleton className="h-24 rounded-xl" />
              ) : (
                <div className="flex flex-wrap gap-2" role="group" aria-label="Loại hình tour thế mạnh">
                  {(categoriesQuery.data ?? []).map((category) => {
                    const selected = field.value.includes(category.id);
                    return (
                      <button
                        key={category.id}
                        type="button"
                        aria-pressed={selected}
                        onClick={() =>
                          field.onChange(selected ? field.value.filter((id) => id !== category.id) : [...field.value, category.id])
                        }
                        className={cn(
                          "rounded-full border px-3.5 py-1.5 text-sm transition-colors outline-none focus-visible:ring-3 focus-visible:ring-ring/50",
                          selected ? "border-primary bg-primary text-primary-foreground" : "bg-card hover:border-primary/50 hover:bg-accent",
                        )}
                      >
                        {category.name}
                      </button>
                    );
                  })}
                </div>
              )}
              {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
            </Field>
          )}
        />

        <div className="mt-2 flex gap-3">
          {onBack && (
            <Button type="button" variant="outline" size="lg" className="h-12 flex-1 rounded-xl" onClick={onBack}>
              Quay lại
            </Button>
          )}
          <Button
            type="submit"
            size="lg"
            className={onBack ? "h-12 flex-1 rounded-xl text-base font-semibold" : "h-11 rounded-xl px-6 font-semibold"}
            disabled={form.formState.isSubmitting || (!onBack && !form.formState.isDirty)}
          >
            {form.formState.isSubmitting && <Spinner />}
            {submitLabel}
          </Button>
        </div>
      </FieldGroup>
    </form>
  );
}
