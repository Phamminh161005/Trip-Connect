import type { Metadata } from "next";
import { AdminTourList } from "./AdminTourList";

export const metadata: Metadata = { title: "Quản lý tour" };

export default function AdminToursPage() {
  return <AdminTourList />;
}
