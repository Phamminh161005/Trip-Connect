"use client";

import { useState, type ReactNode } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { GoogleOAuthProvider } from "@react-oauth/google";
import { AuthProvider } from "@/lib/auth/AuthProvider";
import { RealtimeProvider } from "@/lib/realtime/RealtimeProvider";
import { Toaster } from "@/components/ui/sonner";

const GOOGLE_CLIENT_ID = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID;

export function Providers({ children }: { children: ReactNode }) {
  // Mỗi phiên trình duyệt một QueryClient
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: { staleTime: 60_000, retry: 1, refetchOnWindowFocus: false },
        },
      }),
  );

  const content = (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <RealtimeProvider>{children}</RealtimeProvider>
        {/* Giao diện hiện chỉ có chế độ sáng -> cố định theme toast để không bị tối theo cài đặt máy */}
        <Toaster theme="light" position="top-center" richColors closeButton />
      </AuthProvider>
    </QueryClientProvider>
  );

  // Chưa cấu hình Google Client ID -> web vẫn chạy bình thường, chỉ ẩn nút đăng nhập Google
  return GOOGLE_CLIENT_ID ? <GoogleOAuthProvider clientId={GOOGLE_CLIENT_ID}>{content}</GoogleOAuthProvider> : content;
}
