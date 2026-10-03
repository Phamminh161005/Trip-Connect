"use client";

import { GoogleLogin } from "@react-oauth/google";
import { toast } from "sonner";

const GOOGLE_ENABLED = Boolean(process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID);

export function isGoogleEnabled(): boolean {
  return GOOGLE_ENABLED;
}

/**
 * Nút đăng nhập Google (nút chính thức do Google vẽ — Google yêu cầu giữ nguyên giao diện nút).
 * Trả về idToken; Backend tự xác minh token với Google.
 */
export function GoogleSignInButton({
  onCredential,
  text = "continue_with",
}: {
  onCredential: (idToken: string) => void;
  /** "continue_with" = "Tiếp tục bằng Google" (trang Đăng nhập); "signup_with" = "Đăng ký bằng Google". */
  text?: "continue_with" | "signup_with";
}) {
  if (!GOOGLE_ENABLED) return null;

  return (
    <div className="flex justify-center">
      <GoogleLogin
        onSuccess={(response) => {
          if (response.credential) onCredential(response.credential);
          else toast.error("Không nhận được thông tin từ Google, vui lòng thử lại");
        }}
        onError={() => {
          toast.error("Đăng nhập Google thất bại, vui lòng thử lại");
        }}
        text={text}
        shape="pill"
        size="large"
        width="320"
      />
    </div>
  );
}
