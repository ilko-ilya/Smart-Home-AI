import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: 'standalone', // <-- вот эта строчка нужна для Докера
};

export default nextConfig;
