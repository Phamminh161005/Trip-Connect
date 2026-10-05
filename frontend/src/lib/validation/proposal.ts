import { z } from "zod";
import { CUSTOM_REQUEST_RULES } from "@/lib/customRequest/labels";
import { TOUR_RULES, localDateString } from "@/lib/tour/labels";
import type { CustomRequestDetail, ProposalRequest, ProposalView } from "@/types/customRequest";
import { emptyItineraryDay, itineraryDay, line } from "./tour";
import { whenValid } from "./whenValid";

// Luật kiểm tra đề xuất — giống CustomRequestRequests.Proposal / CustomProposalService.validate của Backend.

export function proposalSchema(request: Pick<CustomRequestDetail, "earliestStart" | "latestStart">) {
  const minStart = localDateString(CUSTOM_REQUEST_RULES.minProposalLeadDays);
  const base = z.object({
      title: z.string().trim().min(10, "Tên chuyến đi từ 10 đến 150 ký tự").max(150, "Tên chuyến đi từ 10 đến 150 ký tự"),
      startDate: z
        .string()
        .regex(/^\d{4}-\d{2}-\d{2}$/, "Vui lòng chọn ngày khởi hành")
        .refine((d) => d >= request.earliestStart && d <= request.latestStart, "Ngày khởi hành phải trong khoảng khách mong muốn")
        .refine((d) => d >= minStart, `Ngày khởi hành phải cách hôm nay ít nhất ${CUSTOM_REQUEST_RULES.minProposalLeadDays} ngày`),
      durationDays: z
        .number({ error: "Vui lòng nhập số ngày" })
        .int("Số ngày phải là số nguyên")
        .min(1, "Chuyến đi ít nhất 1 ngày")
        .max(CUSTOM_REQUEST_RULES.maxDurationDays, `Chuyến đi tối đa ${CUSTOM_REQUEST_RULES.maxDurationDays} ngày`),
      durationNights: z.number({ error: "Vui lòng chọn thời lượng" }).int().min(0, "Số đêm không hợp lệ"),
      itinerary: z.array(itineraryDay),
      transportModes: z.array(z.string()).min(1, "Chọn ít nhất 1 phương tiện"),
      accommodationType: z.string({ error: "Vui lòng chọn tiêu chuẩn lưu trú" }).min(1, "Vui lòng chọn tiêu chuẩn lưu trú"),
      meetingPoint: z.string().trim().min(1, "Điểm đón không được để trống").max(255, "Tối đa 255 ký tự"),
      meetingTime: z.string().regex(/^\d{2}:\d{2}$/, "Vui lòng nhập giờ tập trung"),
      includedServices: z
        .array(line(255, "Dòng dịch vụ không được để trống"))
        .min(1, "Nhập ít nhất 1 dịch vụ bao gồm")
        .max(TOUR_RULES.maxServiceItems, `Tối đa ${TOUR_RULES.maxServiceItems} dòng`),
      excludedServices: z
        .array(line(255, "Dòng dịch vụ không được để trống"))
        .max(TOUR_RULES.maxServiceItems, `Tối đa ${TOUR_RULES.maxServiceItems} dòng`),
      notes: z.string().trim().max(2000, "Tối đa 2000 ký tự"),
      adultPrice: z
        .number({ error: "Vui lòng nhập giá người lớn" })
        .min(TOUR_RULES.minAdultPrice, "Giá người lớn tối thiểu 10.000 VNĐ")
        .max(TOUR_RULES.maxPrice, "Giá quá lớn"),
      childPrice: z.number({ error: "Vui lòng nhập giá trẻ em" }).min(0).max(TOUR_RULES.maxPrice, "Giá quá lớn"),
      message: z.string().trim().max(2000, "Tối đa 2000 ký tự"),
    });
  return base
    .refine((v) => v.durationNights === v.durationDays || v.durationNights === v.durationDays - 1, {
      path: ["durationNights"],
      message: "Số đêm bằng số ngày hoặc ít hơn 1 (vd 3 ngày 2 đêm)",
      when: whenValid(base, "durationDays", "durationNights"),
    })
    .refine((v) => v.itinerary.length === v.durationDays, {
      path: ["itinerary"],
      message: "Số ngày trong lịch trình phải bằng số ngày của chuyến đi",
      when: whenValid(base, "durationDays"),
    })
    .refine((v) => v.childPrice <= v.adultPrice, {
      path: ["childPrice"],
      message: "Giá trẻ em không được cao hơn giá người lớn",
      when: whenValid(base, "adultPrice", "childPrice"),
    });
}

export type ProposalValues = z.infer<ReturnType<typeof proposalSchema>>;

const toLines = (values: string[]) => values.map((value) => ({ value }));

/** Bản chỉnh sửa điền sẵn từ bản trước; bản đầu điền sẵn những gì khách đã yêu cầu. */
export function proposalFormValues(request: CustomRequestDetail, previous?: ProposalView): ProposalValues {
  if (previous) {
    return {
      title: previous.title,
      startDate: previous.startDate,
      durationDays: previous.durationDays,
      durationNights: previous.durationNights,
      itinerary: previous.itinerary.map((d) => ({
        title: d.title,
        description: d.description,
        breakfast: d.breakfast,
        lunch: d.lunch,
        dinner: d.dinner,
        accommodation: d.accommodation ?? "",
      })),
      transportModes: previous.transportModes,
      accommodationType: previous.accommodationType,
      meetingPoint: previous.meetingPoint,
      meetingTime: previous.meetingTime,
      includedServices: toLines(previous.includedServices),
      excludedServices: toLines(previous.excludedServices),
      notes: previous.notes ?? "",
      adultPrice: previous.adultPrice,
      childPrice: previous.childPrice,
      message: "",
    };
  }
  const days = request.durationDays;
  return {
    title: "",
    startDate: "",
    durationDays: days,
    durationNights: Math.max(days - 1, 0),
    itinerary: Array.from({ length: days }, emptyItineraryDay),
    transportModes: request.transportModes,
    accommodationType: request.accommodationType ?? "",
    meetingPoint: "",
    meetingTime: "07:30",
    includedServices: toLines([""]),
    excludedServices: [],
    notes: "",
    adultPrice: undefined as unknown as number,
    childPrice: undefined as unknown as number,
    message: "",
  };
}

export function proposalValuesToRequest(values: ProposalValues): ProposalRequest {
  return {
    title: values.title.trim(),
    startDate: values.startDate,
    durationDays: values.durationDays,
    durationNights: values.durationNights,
    itinerary: values.itinerary.map((d) => ({
      title: d.title.trim(),
      description: d.description.trim(),
      breakfast: d.breakfast,
      lunch: d.lunch,
      dinner: d.dinner,
      accommodation: d.accommodation.trim() || null,
    })),
    transportModes: values.transportModes as ProposalRequest["transportModes"],
    accommodationType: values.accommodationType as ProposalRequest["accommodationType"],
    meetingPoint: values.meetingPoint.trim(),
    meetingTime: values.meetingTime,
    includedServices: values.includedServices.map((s) => s.value.trim()),
    excludedServices: values.excludedServices.map((s) => s.value.trim()),
    notes: values.notes.trim() || null,
    adultPrice: values.adultPrice,
    childPrice: values.childPrice,
    message: values.message.trim() || null,
  };
}

/** Khớp CustomRequestRules.deposit của Backend: 30% làm tròn lên hàng nghìn, không vượt tổng. */
export function depositFor(total: number): number {
  const raw = Math.ceil((total * CUSTOM_REQUEST_RULES.depositPercent) / 100);
  return Math.min(total, Math.ceil(raw / 1000) * 1000);
}
