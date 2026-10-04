import { z } from "zod";
import { TOUR_RULES } from "@/lib/tour/labels";
import type { DepartureRequest, TourContentRequest, TourDetail } from "@/types/tour";

// Luật kiểm tra nội dung tour — giống TourContentRequest / DepartureRequest / TourContentWriter của Backend.
// Danh sách dòng chữ (điểm nổi bật, dịch vụ) lưu dạng { value } vì useFieldArray của react-hook-form cần object.

const line = (max: number, emptyMessage: string) =>
  z.object({ value: z.string().trim().min(1, emptyMessage).max(max, `Tối đa ${max} ký tự`) });

const itineraryDay = z.object({
  title: z.string().trim().min(1, "Tiêu đề ngày không được để trống").max(200, "Tối đa 200 ký tự"),
  description: z.string().trim().min(1, "Nội dung ngày không được để trống").max(5000, "Tối đa 5000 ký tự"),
  breakfast: z.boolean(),
  lunch: z.boolean(),
  dinner: z.boolean(),
  accommodation: z.string().trim().max(255, "Tối đa 255 ký tự"),
});

export const tourContentSchema = z
  .object({
    title: z.string().trim().min(10, "Tên tour từ 10 đến 150 ký tự").max(150, "Tên tour từ 10 đến 150 ký tự"),
    categoryIds: z
      .array(z.number())
      .min(1, "Chọn ít nhất 1 loại hình tour")
      .max(TOUR_RULES.maxCategories, `Chọn tối đa ${TOUR_RULES.maxCategories} loại hình`),
    departureLocationId: z.number({ error: "Vui lòng chọn nơi khởi hành" }),
    destinationIds: z
      .array(z.number())
      .min(1, "Chọn ít nhất 1 điểm đến")
      .max(TOUR_RULES.maxDestinations, `Chọn tối đa ${TOUR_RULES.maxDestinations} điểm đến`),
    durationDays: z
      .number({ error: "Vui lòng nhập số ngày" })
      .int("Số ngày phải là số nguyên")
      .min(1, "Tour ít nhất 1 ngày")
      .max(TOUR_RULES.maxDurationDays, `Tour tối đa ${TOUR_RULES.maxDurationDays} ngày`),
    durationNights: z.number({ error: "Vui lòng nhập số đêm" }).int().min(0, "Số đêm không hợp lệ"),
    highlights: z
      .array(line(150, "Điểm nổi bật không được để trống"))
      .min(TOUR_RULES.minHighlights, `Cần ít nhất ${TOUR_RULES.minHighlights} điểm nổi bật`)
      .max(TOUR_RULES.maxHighlights, `Tối đa ${TOUR_RULES.maxHighlights} điểm nổi bật`),
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
  })
  .refine((v) => v.durationNights === v.durationDays || v.durationNights === v.durationDays - 1, {
    path: ["durationNights"],
    message: "Số đêm bằng số ngày hoặc ít hơn 1 (vd 3 ngày 2 đêm)",
  })
  .refine((v) => v.itinerary.length === v.durationDays, {
    path: ["itinerary"],
    message: "Số ngày trong lịch trình phải bằng số ngày của tour",
  });

export type TourContentValues = z.infer<typeof tourContentSchema>;

export const emptyItineraryDay = (): TourContentValues["itinerary"][number] => ({
  title: "",
  description: "",
  breakfast: false,
  lunch: false,
  dinner: false,
  accommodation: "",
});

const toLines = (values: string[]) => values.map((value) => ({ value }));

export function tourToFormValues(tour?: TourDetail): TourContentValues {
  if (!tour) {
    return {
      title: "",
      categoryIds: [],
      departureLocationId: undefined as unknown as number,
      destinationIds: [],
      durationDays: 1,
      durationNights: 0,
      highlights: toLines(["", "", ""]),
      itinerary: [emptyItineraryDay()],
      transportModes: [],
      accommodationType: "",
      meetingPoint: "",
      meetingTime: "07:30",
      includedServices: toLines([""]),
      excludedServices: [],
      notes: "",
    };
  }
  return {
    title: tour.title,
    categoryIds: tour.categories.map((c) => c.id),
    departureLocationId: tour.departureLocation.id,
    destinationIds: tour.destinations.map((d) => d.id),
    durationDays: tour.durationDays,
    durationNights: tour.durationNights,
    highlights: toLines(tour.highlights),
    itinerary: tour.itinerary.map((d) => ({
      title: d.title,
      description: d.description,
      breakfast: d.breakfast,
      lunch: d.lunch,
      dinner: d.dinner,
      accommodation: d.accommodation ?? "",
    })),
    transportModes: tour.transportModes,
    accommodationType: tour.accommodationType,
    meetingPoint: tour.meetingPoint,
    meetingTime: tour.meetingTime,
    includedServices: toLines(tour.includedServices),
    excludedServices: toLines(tour.excludedServices),
    notes: tour.notes ?? "",
  };
}

export function formValuesToRequest(values: TourContentValues): TourContentRequest {
  return {
    title: values.title.trim(),
    categoryIds: values.categoryIds,
    departureLocationId: values.departureLocationId,
    destinationIds: values.destinationIds,
    durationDays: values.durationDays,
    durationNights: values.durationNights,
    highlights: values.highlights.map((h) => h.value.trim()),
    itinerary: values.itinerary.map((d) => ({
      title: d.title.trim(),
      description: d.description.trim(),
      breakfast: d.breakfast,
      lunch: d.lunch,
      dinner: d.dinner,
      accommodation: d.accommodation.trim() || null,
    })),
    transportModes: values.transportModes as TourContentRequest["transportModes"],
    accommodationType: values.accommodationType as TourContentRequest["accommodationType"],
    meetingPoint: values.meetingPoint.trim(),
    meetingTime: values.meetingTime,
    includedServices: values.includedServices.map((s) => s.value.trim()),
    excludedServices: values.excludedServices.map((s) => s.value.trim()),
    notes: values.notes.trim() || null,
  };
}

/**
 * Tên trường lỗi của Backend -> đường dẫn trong form.
 * "highlights[0]" -> "highlights.0.value", "itinerary[1].title" -> "itinerary.1.title"
 */
export function backendFieldToFormPath(field: string): string {
  const path = field.replace(/\[(\d+)]/g, ".$1");
  return /^(highlights|includedServices|excludedServices)\.\d+$/.test(path) ? `${path}.value` : path;
}

export const departureSchema = z
  .object({
    startDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, "Vui lòng chọn ngày khởi hành"),
    capacity: z
      .number({ error: "Vui lòng nhập số chỗ" })
      .int()
      .min(1, "Số chỗ ít nhất là 1")
      .max(TOUR_RULES.maxCapacity, `Số chỗ tối đa là ${TOUR_RULES.maxCapacity}`),
    adultPrice: z
      .number({ error: "Vui lòng nhập giá người lớn" })
      .min(TOUR_RULES.minAdultPrice, "Giá người lớn tối thiểu 10.000đ")
      .max(TOUR_RULES.maxPrice, "Giá quá lớn"),
    childPrice: z.number({ error: "Vui lòng nhập giá trẻ em" }).min(0).max(TOUR_RULES.maxPrice, "Giá quá lớn"),
  })
  .refine((v) => v.childPrice <= v.adultPrice, {
    path: ["childPrice"],
    message: "Giá trẻ em không được cao hơn giá người lớn",
  });

export type DepartureValues = DepartureRequest;
