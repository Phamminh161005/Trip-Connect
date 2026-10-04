import { Badge } from "@/components/ui/badge";
import { DEPARTURE_STATUS, TOUR_STATUS, departureDisplayStatus } from "@/lib/tour/labels";
import { cn } from "@/lib/utils";
import type { TourDeparture, TourStatus } from "@/types/tour";

export function TourStatusBadge({ status, className }: { status: TourStatus; className?: string }) {
  return <Badge className={cn("border-0", TOUR_STATUS[status].className, className)}>{TOUR_STATUS[status].label}</Badge>;
}

export function DepartureStatusBadge({ departure }: { departure: TourDeparture }) {
  const status = DEPARTURE_STATUS[departureDisplayStatus(departure)];
  return <Badge className={cn("border-0", status.className)}>{status.label}</Badge>;
}
