"use client";

import { BedDouble, Bus, CalendarDays, MapPin, NotebookPen, Tags, Users, Wallet } from "lucide-react";
import type { ReactNode } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { travellersText } from "@/lib/booking/labels";
import { budgetText, destinationsText, placeName, startWindowText } from "@/lib/customRequest/labels";
import { ACCOMMODATION_LABELS, TRANSPORT_LABELS } from "@/lib/tour/labels";
import type { CustomRequestDetail } from "@/types/customRequest";

/** Nội dung yêu cầu của khách — dùng chung cho khách, Agent, Admin. */
export function RequestInfo({ request: r }: { request: CustomRequestDetail }) {
  return (
    <Card className="rounded-2xl">
      <CardHeader>
        <CardTitle className="text-lg">Nội dung yêu cầu</CardTitle>
      </CardHeader>
      <CardContent className="grid gap-4 sm:grid-cols-2">
        <Item icon={<MapPin />} label="Hành trình">
          {placeName(r.departureLocation)} → {destinationsText(r.destinations)}
        </Item>
        <Item icon={<CalendarDays />} label="Thời gian">
          {r.durationDays} ngày, khởi hành trong khoảng {startWindowText(r.earliestStart, r.latestStart)}
        </Item>
        <Item icon={<Users />} label="Số khách">
          {travellersText(r.adults, r.children, r.infants)}
        </Item>
        <Item icon={<Wallet />} label="Ngân sách">
          {budgetText(r.budgetMin, r.budgetMax)}
        </Item>
        <Item icon={<Tags />} label="Loại hình yêu thích">
          {r.categories.length > 0 ? r.categories.map((c) => c.name).join(", ") : "Không yêu cầu"}
        </Item>
        <Item icon={<Bus />} label="Phương tiện">
          {r.transportModes.length > 0 ? r.transportModes.map((m) => TRANSPORT_LABELS[m]).join(", ") : "Không yêu cầu"}
        </Item>
        <Item icon={<BedDouble />} label="Lưu trú">
          {r.accommodationType ? ACCOMMODATION_LABELS[r.accommodationType] : "Không yêu cầu"}
        </Item>
        {r.notes && (
          <div className="sm:col-span-2">
            <Item icon={<NotebookPen />} label="Ghi chú của khách">
              <span className="whitespace-pre-line">{r.notes}</span>
            </Item>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

function Item({ icon, label, children }: { icon: ReactNode; label: string; children: ReactNode }) {
  return (
    <div className="flex gap-3">
      <span className="mt-0.5 text-primary [&_svg]:size-4">{icon}</span>
      <div className="min-w-0">
        <p className="text-xs text-muted-foreground">{label}</p>
        <p className="text-sm">{children}</p>
      </div>
    </div>
  );
}
