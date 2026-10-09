import type { CustomRequestDraft } from "@/types/assistant";
import { CUSTOM_REQUEST_RULES as RULES } from "./labels";
import { localDateString } from "@/lib/tour/labels";

/** Thông tin điền sẵn cho form yêu cầu tour riêng, truyền qua query string (vd từ trợ lý AI). */
export interface CustomRequestPrefill {
  departureLocationId?: number;
  destinationIds?: number[];
  earliestStart?: string;
  latestStart?: string;
  durationDays?: number;
  adults?: number;
  children?: number;
  infants?: number;
  budgetMax?: number;
  notes?: string;
}

/** Link tới form với thông tin trợ lý đã thu thập. */
export function customRequestUrl(draft: CustomRequestDraft): string {
  const params = new URLSearchParams();
  const set = (key: string, value: string | number | null | undefined) => {
    if (value !== null && value !== undefined && value !== "") params.set(key, String(value));
  };
  set("from", draft.departureLocationId);
  if (draft.destinationIds.length > 0) params.set("to", draft.destinationIds.join(","));
  set("start", draft.earliestStart);
  set("end", draft.latestStart);
  set("days", draft.durationDays);
  set("adults", draft.adults);
  set("children", draft.children);
  set("infants", draft.infants);
  set("budget", draft.budgetMax);
  set("notes", draft.notes);
  const query = params.toString();
  return query ? `/custom-tour?${query}` : "/custom-tour";
}

/** Đọc lại từ query string; giá trị sai / quá giới hạn thì bỏ qua để khách tự nhập. */
export function readPrefill(params: URLSearchParams): CustomRequestPrefill {
  const int = (key: string, min: number, max: number) => {
    const raw = params.get(key);
    if (raw === null || !/^\d+$/.test(raw)) return undefined;
    const n = Number(raw);
    return n >= min && n <= max ? n : undefined;
  };
  const date = (key: string) => {
    const raw = params.get(key);
    return raw && /^\d{4}-\d{2}-\d{2}$/.test(raw) ? raw : undefined;
  };

  const ids = (params.get("to") ?? "")
    .split(",")
    .filter((s) => /^\d+$/.test(s))
    .map(Number)
    .slice(0, RULES.maxDestinations);

  // Ngày đã quá mốc "hôm nay + 14 ngày" thì bỏ (khách chọn lại), tránh mở form đã báo lỗi sẵn
  let earliestStart = date("start");
  let latestStart = date("end");
  if (earliestStart && earliestStart < localDateString(RULES.minLeadDays)) {
    earliestStart = undefined;
    latestStart = undefined;
  }
  if (!earliestStart || (latestStart && latestStart < earliestStart)) latestStart = undefined;
  if (earliestStart && latestStart) {
    const window = (new Date(latestStart).getTime() - new Date(earliestStart).getTime()) / 86_400_000;
    if (window > RULES.maxStartWindowDays) latestStart = undefined;
  }

  return {
    departureLocationId: int("from", 1, Number.MAX_SAFE_INTEGER),
    destinationIds: ids.length > 0 ? ids : undefined,
    earliestStart,
    latestStart,
    durationDays: int("days", 1, RULES.maxDurationDays),
    adults: int("adults", 1, RULES.maxTravellers),
    children: int("children", 0, RULES.maxTravellers),
    infants: int("infants", 0, RULES.maxTravellers),
    budgetMax: int("budget", 1, 1_000_000_000),
    notes: params.get("notes")?.slice(0, 2000) || undefined,
  };
}
