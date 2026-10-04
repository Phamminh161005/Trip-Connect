import { Suspense } from "react";
import type { Metadata } from "next";
import { RequireAuth } from "@/components/auth/RequireAuth";
import { parseIdParam } from "@/lib/route";
import { BookingForm } from "./BookingForm";

export const metadata: Metadata = { title: "Đặt tour" };

/** Trang đặt tour — cần đăng nhập (chưa đăng nhập sẽ được đưa tới trang đăng nhập rồi quay lại đây). */
export default async function BookTourPage({ params }: PageProps<"/tours/[id]/book">) {
  const id = parseIdParam((await params).id);
  return (
    <Suspense>
      <RequireAuth roles={["CUSTOMER", "AGENT"]}>
        <BookingForm tourId={id} />
      </RequireAuth>
    </Suspense>
  );
}
