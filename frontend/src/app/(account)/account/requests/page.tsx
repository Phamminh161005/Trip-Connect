import { Suspense } from "react";
import type { Metadata } from "next";
import { RequireAuth } from "@/components/auth/RequireAuth";
import { MyRequestList } from "./MyRequestList";

export const metadata: Metadata = { title: "Yêu cầu tour riêng" };

export default function MyRequestsPage() {
  return (
    <Suspense>
      <RequireAuth roles={["CUSTOMER"]}>
        <MyRequestList />
      </RequireAuth>
    </Suspense>
  );
}
