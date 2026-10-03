export interface LocationResponse {
  id: number;
  country: string;
  province: string | null;
}

export interface TourCategoryResponse {
  id: number;
  name: string;
}

/** Ngân hàng (chuẩn Napas / VietQR). bin = mã định danh ngân hàng 6 số. */
export interface BankResponse {
  bin: string;
  code: string;
  shortName: string;
  name: string;
  logoUrl: string | null;
}
