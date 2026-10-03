export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
}

// Khớp với response của MethodArgumentNotValidException trong GlobalExceptionHandler
export interface ValidationErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  fieldErrors: Record<string, string>;
}

/** Khớp với PageResponse của backend. page bắt đầu từ 0. */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
