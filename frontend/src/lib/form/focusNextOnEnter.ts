import type { KeyboardEvent } from "react";

const FIELDS = [
  'input:not([type="hidden"]):not([type="checkbox"]):not([type="radio"]):not([type="file"])',
  "textarea",
  'button[role="combobox"]',
  'button[aria-haspopup="dialog"]',
].join(",");

const SKIP_TYPES = new Set(["checkbox", "radio", "file", "submit", "button", "reset"]);

/**
 * Gắn vào onKeyDown của <form>: Enter trong một ô nhập -> chuyển sang ô kế tiếp.
 * Ở ô cuối cùng thì để mặc định (gửi form). Textarea giữ Enter để xuống dòng.
 */
export function focusNextOnEnter(event: KeyboardEvent<HTMLFormElement>) {
  if (event.key !== "Enter" || event.defaultPrevented || event.nativeEvent.isComposing) return;
  if (event.shiftKey || event.ctrlKey || event.altKey || event.metaKey) return;

  const target = event.target;
  if (!(target instanceof HTMLInputElement) || SKIP_TYPES.has(target.type)) return;

  const fields = Array.from(event.currentTarget.querySelectorAll<HTMLElement>(FIELDS)).filter(
    (el) => !(el as HTMLInputElement).disabled && !el.closest("[hidden]") && el.getClientRects().length > 0,
  );
  // Ô tìm kiếm trong popup (portal) không nằm trong form -> index = -1, bỏ qua
  const index = fields.indexOf(target);
  if (index === -1 || index === fields.length - 1) return;

  event.preventDefault();
  fields[index + 1].focus();
}
