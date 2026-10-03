import type { BankResponse, LocationResponse, TourCategoryResponse } from "@/types/catalog";
import { apiRequest } from "./client";

export const getLocations = () => apiRequest<LocationResponse[]>("/api/locations", { auth: false });

export const getTourCategories = () =>
  apiRequest<TourCategoryResponse[]>("/api/tour-categories", { auth: false });

export const getBanks = () => apiRequest<BankResponse[]>("/api/banks", { auth: false });

/** Tỉnh/Thành của Việt Nam (bỏ các địa điểm cấp quốc gia / nước ngoài). */
export const isVietnamProvince = (location: LocationResponse) =>
  location.country === "Việt Nam" && location.province !== null;
