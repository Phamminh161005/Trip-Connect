import Image from "next/image";
import Link from "next/link";
import { Button } from "@/components/ui/button";

// TODO: ô tìm kiếm + danh sách tour nổi bật
export default function HomePage() {
  return (
    <section className="relative isolate overflow-hidden">
      <Image
        src="/images/destinations/ha-long.jpg"
        alt="Vịnh Hạ Long"
        fill
        priority
        sizes="100vw"
        className="-z-10 object-cover"
      />
      <div className="absolute inset-0 -z-10 bg-linear-to-r from-black/65 via-black/35 to-transparent" />
      <div className="mx-auto flex min-h-[70svh] max-w-7xl flex-col justify-center gap-6 px-4 py-20 sm:px-6 lg:px-10">
        <h1 className="max-w-2xl text-4xl font-bold tracking-tight text-balance text-white sm:text-5xl">
          Khám phá Việt Nam và thế giới cùng TripConnect
        </h1>
        <p className="max-w-xl text-lg text-white/85">
          Đặt tour ghép đoàn hoặc thiết kế tour riêng theo nhu cầu với các đối tác lữ hành uy tín.
        </p>
        <div className="flex flex-wrap gap-3">
          <Button asChild size="lg" className="h-12 rounded-xl px-6 text-base font-semibold">
            <Link href="/register">Bắt đầu ngay</Link>
          </Button>
          <Button asChild size="lg" variant="secondary" className="h-12 rounded-xl px-6 text-base font-semibold">
            <Link href="/register?type=agent">Trở thành đối tác</Link>
          </Button>
        </div>
      </div>
    </section>
  );
}
