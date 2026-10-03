import type { Metadata } from "next";
import { AdminDashboard } from "./AdminDashboard";

export const metadata: Metadata = { title: "Tổng quan quản trị" };

export default function AdminHomePage() {
  return <AdminDashboard />;
}
