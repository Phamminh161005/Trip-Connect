"use client";

import { useState } from "react";
import Image from "next/image";
import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog";
import type { ReviewImage } from "@/types/review";

/** Ảnh nhỏ của đánh giá; bấm để xem ảnh lớn. */
export function ReviewImages({ images }: { images: ReviewImage[] }) {
  const [open, setOpen] = useState<ReviewImage | null>(null);
  if (images.length === 0) return null;
  return (
    <>
      <div className="flex flex-wrap gap-2">
        {images.map((image) => (
          <button
            key={image.id}
            type="button"
            onClick={() => setOpen(image)}
            className="relative size-20 overflow-hidden rounded-lg bg-muted outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
            aria-label="Xem ảnh lớn"
          >
            <Image src={image.url} alt="" fill sizes="80px" className="object-cover transition-transform hover:scale-105" />
          </button>
        ))}
      </div>
      <Dialog open={open !== null} onOpenChange={(next) => !next && setOpen(null)}>
        <DialogContent className="max-w-3xl p-2 sm:max-w-3xl">
          <DialogTitle className="sr-only">Ảnh đánh giá</DialogTitle>
          {open && (
            <div className="relative aspect-4/3 w-full overflow-hidden rounded-lg">
              <Image src={open.url} alt="" fill sizes="(min-width: 768px) 768px, 100vw" className="object-contain" />
            </div>
          )}
        </DialogContent>
      </Dialog>
    </>
  );
}
