import Image from "next/image";
import Link from "next/link";
import { TourSearchBar } from "@/components/search/TourSearchBar";
import { HomeSections } from "./HomeSections";

export default function HomePage() {
  return (
    <>
      {/* Kéo ảnh lên nằm dưới header trong suốt (header cao h-18) */}
      <section className="relative isolate -mt-18">
        <div className="absolute inset-0 -z-10 overflow-hidden">
          <Image src="/images/destinations/ha-long.jpg" alt="Vịnh Hạ Long" fill priority sizes="100vw" className="object-cover" />
          <div className="absolute inset-0 bg-linear-to-r from-black/70 via-black/40 to-black/10" />
          {/* Tối phần trên để chữ trắng của header luôn đọc rõ */}
          <div className="absolute inset-x-0 top-0 h-40 bg-linear-to-b from-black/50 to-transparent" />
        </div>
        <div className="mx-auto flex min-h-[calc(62svh+4.5rem)] max-w-7xl flex-col justify-center gap-6 px-4 pt-34 pb-16 sm:px-6 lg:px-10">
          <h1 className="max-w-2xl text-4xl font-bold tracking-tight text-balance text-white sm:text-5xl">
            Khám phá Việt Nam và thế giới cùng TripConnect
          </h1>
          <p className="max-w-xl text-lg text-white/85">
            Đặt tour ghép đoàn hoặc thiết kế tour riêng theo nhu cầu với các đối tác lữ hành uy tín.
          </p>
          <div className="max-w-4xl">
            <TourSearchBar variant="hero" />
          </div>
          <p className="text-sm text-white/80">
            Bạn là công ty lữ hành?{" "}
            <Link href="/register?type=agent" className="font-semibold text-white underline underline-offset-4">
              Trở thành đối tác
            </Link>
          </p>
        </div>
      </section>
      <HomeSections />
    </>
  );
}
