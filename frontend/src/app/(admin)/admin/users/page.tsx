import type { Metadata } from "next";
import { UserList } from "./UserList";

export const metadata: Metadata = { title: "Quản lý người dùng" };

export default function AdminUsersPage() {
  return <UserList />;
}
