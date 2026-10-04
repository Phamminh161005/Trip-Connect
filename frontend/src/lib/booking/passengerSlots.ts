import { z } from "zod";
import type { BookingPassenger, PassengerInput, PassengerType } from "@/types/booking";
import { PASSENGER_TYPE, classifyPassenger } from "./labels";

// Danh sách hành khách nhập theo "ô" đã gắn loại sẵn (từ số khách đã chọn): giá chốt theo số này,
// ô nào cũng phải điền và ngày sinh phải khớp loại của ô. Khớp BookingService.replacePassengers.

export const PASSENGER_LIST_DEADLINE_DAYS = 3;

export interface PassengerSlot {
  type: PassengerType;
  fullName: string;
  dateOfBirth: string;
  passportNumber: string;
}

export interface TravellerCounts {
  adults: number;
  children: number;
  infants: number;
}

export const passengerSlotSchema = z.object({
  type: z.enum(["ADULT", "CHILD", "INFANT"]),
  fullName: z.string(),
  dateOfBirth: z.string(),
  passportNumber: z.string(),
});

const TYPES: PassengerType[] = ["ADULT", "CHILD", "INFANT"];

const countOf = (counts: TravellerCounts, type: PassengerType) =>
  type === "ADULT" ? counts.adults : type === "CHILD" ? counts.children : counts.infants;

/** Dựng lại các ô theo số khách mới, giữ thông tin đã nhập của từng loại (bớt khách thì bỏ ô cuối của loại đó). */
export function buildSlots(counts: TravellerCounts, current: PassengerSlot[]): PassengerSlot[] {
  return TYPES.flatMap((type) => {
    const existing = current.filter((s) => s.type === type);
    return Array.from({ length: countOf(counts, type) }, (_, i) => existing[i] ?? { type, fullName: "", dateOfBirth: "", passportNumber: "" });
  });
}

/** Ô của một đơn đã đặt, điền sẵn hành khách đã có. */
export function slotsFromBooking(counts: TravellerCounts, passengers: BookingPassenger[]): PassengerSlot[] {
  return buildSlots(
    counts,
    passengers.map((p) => ({ type: p.type, fullName: p.fullName, dateOfBirth: p.dateOfBirth, passportNumber: p.passportNumber ?? "" })),
  );
}

/** "Người lớn 2", "Trẻ em 1"... */
export function slotLabel(slots: PassengerSlot[], index: number): string {
  const type = slots[index].type;
  const order = slots.slice(0, index + 1).filter((s) => s.type === type).length;
  return `${PASSENGER_TYPE[type]} ${order}`;
}

export interface SlotRules {
  startDate: string;
  international: boolean;
}

/** Kiểm tra các ô (dùng trong superRefine của form). */
export function checkSlots(slots: PassengerSlot[], rules: SlotRules, ctx: z.RefinementCtx, path: (string | number)[]) {
  slots.forEach((s, i) => {
    const issue = (field: keyof PassengerSlot, message: string) =>
      ctx.addIssue({ code: "custom", path: [...path, i, field], message });
    if (!s.fullName.trim()) issue("fullName", "Nhập họ tên hành khách");
    else if (s.fullName.trim().length > 100) issue("fullName", "Tối đa 100 ký tự");

    if (!/^\d{4}-\d{2}-\d{2}$/.test(s.dateOfBirth)) {
      issue("dateOfBirth", "Nhập ngày sinh");
    } else {
      const actual = classifyPassenger(s.dateOfBirth, rules.startDate);
      if (actual === null) issue("dateOfBirth", "Ngày sinh không hợp lệ");
      else if (actual !== s.type) {
        issue("dateOfBirth", `Theo ngày sinh, khách này là ${PASSENGER_TYPE[actual].toLowerCase()} vào ngày đi — không khớp ô ${PASSENGER_TYPE[s.type].toLowerCase()}`);
      }
    }

    const passport = s.passportNumber.trim();
    if (rules.international && !passport) issue("passportNumber", "Nhập số hộ chiếu");
    else if (passport && !/^[A-Za-z0-9]{6,20}$/.test(passport)) issue("passportNumber", "Số hộ chiếu gồm 6-20 chữ cái hoặc chữ số");
  });
}

export const slotsToRequest = (slots: PassengerSlot[]): PassengerInput[] =>
  slots.map((s) => ({ fullName: s.fullName.trim(), dateOfBirth: s.dateOfBirth, passportNumber: s.passportNumber.trim() || null }));

/** Khách tự sửa danh sách được tới hết ngày này, vd "2026-10-12". */
export function passengerListDeadline(startDate: string): string {
  const [y, m, d] = startDate.split("-").map(Number);
  const date = new Date(Date.UTC(y, m - 1, d - PASSENGER_LIST_DEADLINE_DAYS));
  return date.toISOString().slice(0, 10);
}
