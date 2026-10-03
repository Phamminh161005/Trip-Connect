import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  reactCompiler: true,
  images: {
    // Ảnh tour lưu trên Cloudinary (thư mục tripconnect/) — cho phép next/image tối ưu kích thước
    remotePatterns: [{ protocol: "https", hostname: "res.cloudinary.com", pathname: "/*/image/upload/**" }],
  },
};

export default nextConfig;
