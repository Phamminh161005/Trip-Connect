"use client";

import { Controller, type Control, type FieldArrayWithId } from "react-hook-form";
import { TextField } from "@/components/form/TextField";
import { Field, FieldError, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { slotLabel, type PassengerSlot } from "@/lib/booking/passengerSlots";

export interface PassengerSlotsValues {
  passengers: PassengerSlot[];
}

/** Các ô nhập hành khách đã gắn loại (Người lớn 1, Trẻ em 1...). */
export function PassengerSlotsFields({
  control,
  fields,
  slots,
  startDate,
  international,
}: {
  control: Control<PassengerSlotsValues>;
  fields: FieldArrayWithId<PassengerSlotsValues, "passengers">[];
  slots: PassengerSlot[];
  startDate: string;
  international: boolean;
}) {
  return (
    <div className="flex flex-col gap-4">
      {fields.map((item, index) => (
        <fieldset key={item.id} className="flex flex-col gap-4 rounded-2xl border p-4">
          <legend className="px-1 text-sm font-semibold">{slots[index] ? slotLabel(slots, index) : `Khách ${index + 1}`}</legend>
          <div className={`grid gap-4 ${international ? "sm:grid-cols-3" : "sm:grid-cols-[2fr_1fr]"}`}>
            <TextField control={control} name={`passengers.${index}.fullName`} label="Họ và tên" />
            <Controller
              control={control}
              name={`passengers.${index}.dateOfBirth`}
              render={({ field, fieldState }) => (
                <Field data-invalid={fieldState.invalid}>
                  <FieldLabel htmlFor={`dob-${index}`}>Ngày sinh</FieldLabel>
                  <Input
                    id={`dob-${index}`}
                    type="date"
                    max={startDate}
                    {...field}
                    aria-invalid={fieldState.invalid}
                    className="h-12 rounded-xl text-base"
                  />
                  {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
                </Field>
              )}
            />
            {international && <TextField control={control} name={`passengers.${index}.passportNumber`} label="Số hộ chiếu" />}
          </div>
        </fieldset>
      ))}
    </div>
  );
}
