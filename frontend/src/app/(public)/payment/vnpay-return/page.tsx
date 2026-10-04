import { Suspense } from "react";
import type { Metadata } from "next";
import { VnPayReturnView } from "./VnPayReturnView";

export const metadata: Metadata = { title: "Kết quả thanh toán", robots: { index: false } };

export default function VnPayReturnPage() {
  return (
    <Suspense>
      <VnPayReturnView />
    </Suspense>
  );
}
