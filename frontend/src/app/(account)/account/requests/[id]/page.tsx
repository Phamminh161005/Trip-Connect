import { Suspense } from "react";
import type { Metadata } from "next";
import { RequireAuth } from "@/components/auth/RequireAuth";
import { parseIdParam } from "@/lib/route";
import { MyRequestView } from "./MyRequestView";

export const metadata: Metadata = { title: "Chi tiết yêu cầu tour riêng" };

export default async function MyRequestPage({ params }: { params: Promise<{ id: string }> }) {
  const id = parseIdParam((await params).id);
  return (
    <Suspense>
      <RequireAuth roles={["CUSTOMER"]}>
        <MyRequestView id={id} />
      </RequireAuth>
    </Suspense>
  );
}
