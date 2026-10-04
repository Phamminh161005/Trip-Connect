"use client";

import { useState } from "react";
import Image from "next/image";
import { ChevronLeft, ChevronRight, Images } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog";
import type { TourImage } from "@/types/tour";

/** Ảnh bìa lớn + 4 ảnh nhỏ; bấm vào ảnh để xem cỡ lớn và lướt qua lại. */
export function TourGallery({ images, title }: { images: TourImage[]; title: string }) {
  const [openIndex, setOpenIndex] = useState<number | null>(null);

  if (images.length === 0) {
    return (
      <div className="flex aspect-[21/9] items-center justify-center rounded-2xl bg-muted text-muted-foreground">
        <Images className="size-8" />
      </div>
    );
  }

  const [cover, ...rest] = images;
  const thumbs = rest.slice(0, 4);
  const move = (step: number) => setOpenIndex((i) => (i === null ? i : (i + step + images.length) % images.length));

  return (
    <>
      <div className="grid gap-2 sm:grid-cols-4 sm:grid-rows-2">
        <button
          type="button"
          onClick={() => setOpenIndex(0)}
          className="relative aspect-[4/3] overflow-hidden rounded-2xl sm:col-span-2 sm:row-span-2 sm:aspect-auto sm:min-h-80"
        >
          <Image src={cover.url} alt={title} fill priority sizes="(min-width: 640px) 50vw, 100vw" className="object-cover" />
        </button>
        {thumbs.map((image, index) => (
          <button
            key={image.id}
            type="button"
            onClick={() => setOpenIndex(index + 1)}
            className="relative hidden aspect-[4/3] overflow-hidden rounded-2xl sm:block"
          >
            <Image src={image.url} alt={`${title} — ảnh ${index + 2}`} fill sizes="25vw" className="object-cover" />
            {index === thumbs.length - 1 && images.length > 5 && (
              <span className="absolute inset-0 flex items-center justify-center bg-black/45 font-semibold text-white">
                +{images.length - 5} ảnh
              </span>
            )}
          </button>
        ))}
      </div>
      {images.length > 1 && (
        <Button variant="outline" size="sm" className="mt-2 rounded-lg sm:hidden" onClick={() => setOpenIndex(0)}>
          <Images /> Xem {images.length} ảnh
        </Button>
      )}

      <Dialog open={openIndex !== null} onOpenChange={(open) => !open && setOpenIndex(null)}>
        <DialogContent className="max-w-5xl rounded-2xl p-2 sm:max-w-5xl">
          <DialogTitle className="sr-only">Ảnh tour {title}</DialogTitle>
          {openIndex !== null && (
            <div className="relative aspect-[3/2] w-full overflow-hidden rounded-xl bg-black">
              <Image src={images[openIndex].url} alt={`${title} — ảnh ${openIndex + 1}`} fill sizes="90vw" className="object-contain" />
              <span className="absolute bottom-2 left-1/2 -translate-x-1/2 rounded-full bg-black/60 px-3 py-1 text-xs text-white">
                {openIndex + 1} / {images.length}
              </span>
              {images.length > 1 && (
                <>
                  <Button
                    size="icon"
                    variant="secondary"
                    className="absolute top-1/2 left-2 -translate-y-1/2 rounded-full"
                    onClick={() => move(-1)}
                    aria-label="Ảnh trước"
                  >
                    <ChevronLeft />
                  </Button>
                  <Button
                    size="icon"
                    variant="secondary"
                    className="absolute top-1/2 right-2 -translate-y-1/2 rounded-full"
                    onClick={() => move(1)}
                    aria-label="Ảnh sau"
                  >
                    <ChevronRight />
                  </Button>
                </>
              )}
            </div>
          )}
        </DialogContent>
      </Dialog>
    </>
  );
}
