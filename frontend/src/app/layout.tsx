import type { Metadata } from "next";
import { Be_Vietnam_Pro, Geist_Mono } from "next/font/google";
import { Providers } from "./providers";
import "./globals.css";

// Font thiết kế riêng cho tiếng Việt (dấu hiển thị cân đối).
const beVietnamPro = Be_Vietnam_Pro({
  variable: "--font-sans",
  subsets: ["latin", "vietnamese"],
  weight: ["400", "500", "600", "700"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: {
    default: "TripConnect - Đặt tour du lịch trực tuyến",
    template: "%s | TripConnect",
  },
  description: "Khám phá và đặt tour du lịch trong nước và quốc tế cùng các đối tác uy tín.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="vi" className={`${beVietnamPro.variable} ${geistMono.variable} h-full antialiased`}>
      <body className="flex min-h-full flex-col">
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
