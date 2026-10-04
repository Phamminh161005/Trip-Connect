"use client";

import { Suspense, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { usePathname, useSearchParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { ArrowRight, ChevronDown, MapPin } from "lucide-react";
import { locationLabel } from "@/components/auth/register/LocationMultiSelect";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { getPopularDestinations } from "@/lib/api/search";
import { cn } from "@/lib/utils";

export const NAV_LINKS = [
  { href: "/tours", label: "Tất cả tour", international: null },
  { href: "/tours?international=false", label: "Trong nước", international: "false" },
  { href: "/tours?international=true", label: "Quốc tế", international: "true" },
] as const;

const itemClass = (inverted: boolean, active: boolean) =>
  cn(
    "relative flex items-center gap-1 rounded-full px-3.5 py-2 text-sm font-semibold transition-colors outline-none focus-visible:ring-3 focus-visible:ring-ring/50",
    inverted ? "text-white hover:bg-white/15" : "hover:bg-accent",
    // Gạch dưới cho mục đang xem
    active &&
      cn(
        "after:absolute after:inset-x-3.5 after:-bottom-0.5 after:h-0.5 after:rounded-full",
        inverted ? "after:bg-white" : "after:bg-primary",
      ),
  );

/** Menu giữa header (màn hình rộng): nhóm tour + điểm đến nổi bật. */
export function MainNav({ inverted }: { inverted: boolean }) {
  return (
    <nav aria-label="Điều hướng chính" className="hidden items-center gap-1 md:flex">
      {/* useSearchParams cần Suspense để trang tĩnh vẫn dựng sẵn được */}
      <Suspense fallback={<NavLinks inverted={inverted} activeKey={null} />}>
        <NavLinksWithActive inverted={inverted} />
      </Suspense>
      <DestinationsMenu inverted={inverted} />
    </nav>
  );
}

function NavLinksWithActive({ inverted }: { inverted: boolean }) {
  const pathname = usePathname();
  const params = useSearchParams();
  const activeKey = pathname === "/tours" ? (params.get("destinationId") ? "destination" : (params.get("international") ?? "all")) : null;
  return <NavLinks inverted={inverted} activeKey={activeKey} />;
}

function NavLinks({ inverted, activeKey }: { inverted: boolean; activeKey: string | null }) {
  return NAV_LINKS.map((link) => (
    <Link
      key={link.href}
      href={link.href}
      aria-current={activeKey === (link.international ?? "all") ? "page" : undefined}
      className={itemClass(inverted, activeKey === (link.international ?? "all"))}
    >
      {link.label}
    </Link>
  ));
}

function DestinationsMenu({ inverted }: { inverted: boolean }) {
  // Dùng chung bộ nhớ đệm với mục "Điểm đến nổi bật" ở trang chủ
  const query = useQuery({ queryKey: ["popular-destinations"], queryFn: () => getPopularDestinations(8) });
  const [open, setOpen] = useState(false);
  if (query.isError || (query.data && query.data.length === 0)) return null;
  const close = () => setOpen(false);

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger className={cn(itemClass(inverted, false), "data-[state=open]:bg-accent", inverted && "data-[state=open]:bg-white/15")}>
        Điểm đến <ChevronDown className="size-4 transition-transform in-data-[state=open]:rotate-180" />
      </PopoverTrigger>
      <PopoverContent align="center" sideOffset={10} className="w-[min(92vw,560px)] rounded-2xl p-4">
        <p className="mb-3 text-sm font-semibold">Điểm đến nổi bật</p>
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
          {query.isPending
            ? Array.from({ length: 8 }, (_, i) => <div key={i} className="aspect-4/3 animate-pulse rounded-xl bg-muted" />)
            : query.data.map((d) => (
                <Link
                  key={d.location.id}
                  href={`/tours?destinationId=${d.location.id}`}
                  onClick={close}
                  className="group relative aspect-4/3 overflow-hidden rounded-xl bg-muted outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
                >
                  {d.coverImageUrl && (
                    <Image
                      src={d.coverImageUrl}
                      alt=""
                      fill
                      sizes="140px"
                      className="object-cover transition-transform duration-300 group-hover:scale-105"
                    />
                  )}
                  <div className="absolute inset-0 bg-linear-to-t from-black/75 via-black/10 to-transparent" />
                  <div className="absolute right-2 bottom-1.5 left-2 text-white">
                    <p className="flex items-center gap-0.5 truncate text-xs font-semibold">
                      <MapPin className="size-3 shrink-0" /> {locationLabel(d.location)}
                    </p>
                    <p className="text-[11px] text-white/80">{d.tourCount} tour</p>
                  </div>
                </Link>
              ))}
        </div>
        <Link
          href="/tours"
          onClick={close}
          className="mt-3 flex w-fit items-center gap-1 text-sm font-semibold text-primary hover:underline"
        >
          Xem tất cả tour <ArrowRight className="size-4" />
        </Link>
      </PopoverContent>
    </Popover>
  );
}
