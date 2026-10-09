# TripConnect

TripConnect là hệ thống đặt tour du lịch trực tuyến. Doanh nghiệp vận hành nền tảng tự xây dựng và bán tour của mình,
đồng thời cho các công ty lữ hành khác (đối tác) đăng bán tour sau khi được xác minh hồ sơ.

Khách tìm, đặt và thanh toán tour qua VNPay, được hoàn tiền tự động theo chính sách hủy. Khi không có tour phù hợp, khách
gửi yêu cầu thiết kế tour riêng; hệ thống giao yêu cầu cho đối tác am hiểu điểm đến, đối tác gửi lịch trình kèm báo giá,
hai bên trao đổi và chỉnh sửa đến khi chốt rồi khách đặt cọc. Nền tảng giữ tiền thanh toán của khách và đối soát hoa
hồng với đối tác hằng tháng. Trợ lý AI tư vấn tour và trả lời câu hỏi dựa trên dữ liệu thật của hệ thống.

## Tính năng

| Vai trò | Chức năng chính |
|---|---|
| Khách vãng lai | Tìm kiếm tour không cần gõ dấu, lọc theo loại hình, ngày đi, giá, thời lượng; xem chi tiết và tour tương tự; hỏi trợ lý AI; đăng ký / đăng nhập (email + OTP hoặc Google) |
| Khách hàng | Đặt tour ghép đoàn và thanh toán VNPay; hủy đơn, hoàn tiền theo chính sách; gửi yêu cầu tour riêng, yêu cầu chỉnh sửa đề xuất, đặt cọc 30%; trò chuyện với đối tác; đánh giá chuyến đi; gợi ý tour theo sở thích |
| Đối tác | Hồ sơ doanh nghiệp và giấy tờ pháp lý (được duyệt trước khi bán); quản lý tour và lịch khởi hành; xem đơn đặt; nhận và đề xuất lịch trình cho yêu cầu tour riêng; trả lời đánh giá; xác nhận hoặc khiếu nại bảng đối soát hằng tháng |
| Quản trị viên | Duyệt hồ sơ đối tác, duyệt / đình chỉ tour, tạo tour của TripConnect; quản lý người dùng, đơn đặt, hoàn tiền thủ công; giao yêu cầu tour riêng; lập đối soát hoa hồng; quản lý danh mục địa điểm và loại hình tour |

Một số điểm kỹ thuật:

- **Chống bán trùng chỗ:** khóa bản ghi lịch khởi hành (`SELECT … FOR UPDATE`), giữ chỗ 15 phút cho đơn chưa thanh toán.
- **Thanh toán an toàn:** xác thực chữ ký HMAC-SHA512 của VNPay, ghi nhận kết quả đúng một lần từ IPN, trang trả về và tra cứu chủ động (querydr).
- **Thời gian thực:** WebSocket STOMP cho trò chuyện và thông báo, chỉ đẩy sự kiện sau khi giao dịch CSDL commit.
- **Trợ lý AI:** Gemini với gọi hàm (function calling) và RAG trên véc-tơ tour và kho kiến thức, trả lời dạng luồng (SSE).
- **Báo cáo:** Power BI đọc dữ liệu qua các view chỉ đọc trong schema `reporting`.

### Xác thực và phân quyền

- **Email + OTP:** mật khẩu băm BCrypt; OTP chỉ lưu bản băm, hiệu lực 5 phút, tối đa 5 lần nhập sai.
- **Google và token:** Backend tự xác minh Google ID token; access token (JWT) chỉ giữ trong bộ nhớ trang, refresh token
  nằm trong cookie `HttpOnly` qua route BFF của Next.js, CSDL chỉ lưu bản băm.
- **Khóa tài khoản:** sai mật khẩu 5 lần thì khóa 15 phút, mở khóa ngay được bằng OTP.
- **Xoay vòng refresh token:** token đã dùng mà bị dùng lại thì thu hồi mọi phiên của người dùng.
- **Phân quyền:** tách theo nhóm API `/api`, `/api/agent`, `/api/admin`; đối tác chỉ bán tour khi hồ sơ đã được duyệt.
- **Giới hạn tần suất** đăng ký, đăng nhập và gửi OTP theo IP và email.

## Công nghệ

| Tầng | Công nghệ |
|---|---|
| Backend | Java 17, Spring Boot 4 (Web MVC, Security, Data JPA, WebSocket, Mail, Scheduling), Flyway, JWT, Caffeine, Apache Tika |
| Frontend | Next.js 16 (React 19, App Router), TanStack Query, React Hook Form + Zod, Tailwind CSS, shadcn/ui |
| CSDL | PostgreSQL 16 (Docker) |
| Dịch vụ ngoài | VNPay (sandbox), Cloudinary, Google OAuth, Gmail SMTP, Gemini API, VietQR |
| Báo cáo | Power BI Desktop |

## Cấu trúc thư mục

```
Trip-Connect/
├── backend/                Spring Boot API (cổng 8080)
│   ├── src/main/java/…     controller, service, repository, entity, security, realtime, payment, ai, job…
│   ├── src/main/resources/
│   │   ├── db/migration/   Migration Flyway V1 … V23
│   │   └── knowledge/      Kho kiến thức cho trợ lý AI (Markdown)
│   └── reporting/          File Power BI (.pbix), script sinh dữ liệu mẫu, hướng dẫn dựng CSDL demo
├── frontend/               Next.js (cổng 3000)
│   └── src/                app/ (public, auth, account, agent, admin), components/, lib/, hooks/, types/
└── docker-compose.yml      PostgreSQL 16
```

## Cài đặt và chạy

### Yêu cầu

- Docker Desktop
- JDK 17
- Node.js 20 trở lên

### 1. Khởi động CSDL

```bash
docker compose up -d
```

### 2. Backend

Tạo tệp `backend/.env` với các biến sau (giá trị lấy từ tài khoản của bạn):

```env
JWT_SECRET=<chuỗi bí mật dài, tối thiểu 32 ký tự>
JWT_ACCESS_EXPIRATION_MS=900000
JWT_REFRESH_EXPIRATION_MS=604800000

GOOGLE_CLIENT_ID=<OAuth client ID>
GMAIL_USERNAME=<địa chỉ Gmail gửi thư>
GMAIL_APP_PASSWORD=<App Password của Gmail>
CLOUDINARY_URL=cloudinary://<api_key>:<api_secret>@<cloud_name>

VNPAY_TMN_CODE=<mã website sandbox>
VNPAY_HASH_SECRET=<chuỗi bí mật sandbox>
GEMINI_API_KEY=<khóa Gemini API>
FRONTEND_URL=http://localhost:3000
```

`VNPAY_*` và `GEMINI_API_KEY` có thể để trống: khi đó thanh toán và trợ lý AI không hoạt động, các chức năng khác vẫn chạy.

```bash
cd backend
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

Khi khởi động, Flyway tự áp dụng các migration còn thiếu; nếu có khóa Gemini, hệ thống tạo véc-tơ cho kho kiến thức và
các tour đang bán ở luồng nền.

### 3. Frontend

Tạo tệp `frontend/.env.local`:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_GOOGLE_CLIENT_ID=<OAuth client ID, giống backend>
REFRESH_TOKEN_MAX_AGE_SECONDS=604800
```

```bash
cd frontend
npm install
npm run dev
```

Mở <http://localhost:3000>.

### Ghi chú

- Tài khoản quản trị viên không đăng ký được qua giao diện; cần tạo sẵn trong CSDL (`role = 'ADMIN'`).
- Khi chạy ở máy cá nhân, VNPay không gọi được IPN về `localhost`; kết quả thanh toán vẫn được ghi nhận qua trang
  trả về và tra cứu chủ động.
- Thẻ thử nghiệm VNPay sandbox: ngân hàng NCB, số thẻ `9704198526191432198`, tên `NGUYEN VAN A`, ngày phát hành
  `07/15`, OTP `123456`.

## Kiểm thử

```bash
cd backend
./mvnw test
```

Các quy tắc nghiệp vụ ở tầng Service được kiểm thử đơn vị bằng JUnit 5, Mockito và AssertJ; thời gian được cố định
bằng `Clock` để kiểm tra các mốc hạn.

## Báo cáo Power BI

File `backend/reporting/BaoCaoTripConnect.pbix` gồm hai trang: **Tổng quan** (doanh thu, phần của TripConnect, tiền
trả đối tác, hoàn tiền, tỉ lệ hủy theo tháng) và **Xếp hạng đơn vị tổ chức tour**. Cách dựng CSDL demo và tạo tài khoản
chỉ đọc cho Power BI xem tại [backend/reporting/README.md](backend/reporting/README.md).
