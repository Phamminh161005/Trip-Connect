import { redirect } from "next/navigation";

// TODO: trang tổng quan (doanh thu, đơn đặt, yêu cầu tư vấn). Tạm chuyển tới Hồ sơ kinh doanh.
export default function AgentHomePage() {
  redirect("/agent/profile");
}
