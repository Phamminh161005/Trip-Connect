-- =====================================================================
-- V5: Địa chỉ trụ sở + tài khoản ngân hàng CÓ CẤU TRÚC (thay cho ô chữ tự do)
--  - Địa chỉ: Tỉnh/Thành (chọn từ bảng locations) + địa chỉ chi tiết (cột address cũ)
--  - Ngân hàng: mã BIN (chọn từ bảng banks) + số tài khoản + tên chủ tài khoản
--    Tách riêng để sau này tạo được mã QR chuyển khoản VietQR khi quyết toán.
-- Dữ liệu cột bank_account cũ (ô chữ tự do) KHÔNG tách tự động được -> xóa; Agent nhập lại.
-- =====================================================================

-- ----- Danh mục ngân hàng (chuẩn Napas / VietQR) -----
-- bin: mã định danh ngân hàng 6 số trong hệ thống chuyển khoản liên ngân hàng (vd 970436 = Vietcombank)
CREATE TABLE banks (
    bin                varchar(8)   PRIMARY KEY,
    code               varchar(20)  NOT NULL,
    short_name         varchar(100) NOT NULL,
    name               varchar(255) NOT NULL,
    logo_url           varchar(500),
    transfer_supported boolean      NOT NULL DEFAULT true,
    active             boolean      NOT NULL DEFAULT true
);

-- Snapshot 65 ngân hàng từ https://api.vietqr.io/v2/banks (2026-09-28)
INSERT INTO banks (bin, code, short_name, name, logo_url, transfer_supported)
VALUES
    ('970425', 'ABB', 'ABBANK', 'Ngân hàng TMCP An Bình', 'https://cdn.vietqr.io/img/ABB.png', true),
    ('970416', 'ACB', 'ACB', 'Ngân hàng TMCP Á Châu', 'https://cdn.vietqr.io/img/ACB.png', true),
    ('970405', 'VBA', 'Agribank', 'Ngân hàng Nông nghiệp và Phát triển Nông thôn Việt Nam', 'https://cdn.vietqr.io/img/VBA.png', true),
    ('970409', 'BAB', 'BacABank', 'Ngân hàng TMCP Bắc Á', 'https://cdn.vietqr.io/img/BAB.png', true),
    ('970438', 'BVB', 'BaoVietBank', 'Ngân hàng TMCP Bảo Việt', 'https://cdn.vietqr.io/img/BVB.png', true),
    ('970418', 'BIDV', 'BIDV', 'Ngân hàng TMCP Đầu tư và Phát triển Việt Nam', 'https://cdn.vietqr.io/img/BIDV.png', true),
    ('546034', 'CAKE', 'CAKE', 'TMCP Việt Nam Thịnh Vượng - Ngân hàng số CAKE by VPBank', 'https://cdn.vietqr.io/img/CAKE.png', true),
    ('970444', 'CBB', 'CBBank', 'Ngân hàng Thương mại TNHH MTV Xây dựng Việt Nam', 'https://cdn.vietqr.io/img/CBB.png', false),
    ('422589', 'CIMB', 'CIMB', 'Ngân hàng TNHH MTV CIMB Việt Nam', 'https://cdn.vietqr.io/img/CIMB.png', true),
    ('533948', 'CITIBANK', 'Citibank', 'Ngân hàng Citibank, N.A. - Chi nhánh Hà Nội', 'https://cdn.vietqr.io/img/CITIBANK.png', false),
    ('970446', 'COOPBANK', 'COOPBANK', 'Ngân hàng Hợp tác xã Việt Nam', 'https://cdn.vietqr.io/img/COOPBANK.png', true),
    ('796500', 'DBS', 'DBSBank', 'DBS Bank Ltd - Chi nhánh Thành phố Hồ Chí Minh', 'https://cdn.vietqr.io/img/DBS.png', false),
    ('970431', 'EIB', 'Eximbank', 'Ngân hàng TMCP Xuất Nhập khẩu Việt Nam', 'https://cdn.vietqr.io/img/EIB.png', true),
    ('970408', 'GPB', 'GPBank', 'Ngân hàng Thương mại TNHH MTV Dầu Khí Toàn Cầu', 'https://cdn.vietqr.io/img/GPB.png', false),
    ('970437', 'HDB', 'HDBank', 'Ngân hàng TMCP Phát triển Thành phố Hồ Chí Minh', 'https://cdn.vietqr.io/img/HDB.png', true),
    ('970442', 'HLBVN', 'HongLeong', 'Ngân hàng TNHH MTV Hong Leong Việt Nam', 'https://cdn.vietqr.io/img/HLBVN.png', false),
    ('458761', 'HSBC', 'HSBC', 'Ngân hàng TNHH MTV HSBC (Việt Nam)', 'https://cdn.vietqr.io/img/HSBC.png', false),
    ('970456', 'IBK - HCM', 'IBKHCM', 'Ngân hàng Công nghiệp Hàn Quốc - Chi nhánh TP. Hồ Chí Minh', 'https://cdn.vietqr.io/img/IBK.png', false),
    ('970455', 'IBK - HN', 'IBKHN', 'Ngân hàng Công nghiệp Hàn Quốc - Chi nhánh Hà Nội', 'https://cdn.vietqr.io/img/IBK.png', false),
    ('970434', 'IVB', 'IndovinaBank', 'Ngân hàng TNHH Indovina', 'https://cdn.vietqr.io/img/IVB.png', false),
    ('668888', 'KBank', 'KBank', 'Ngân hàng Đại chúng TNHH Kasikornbank', 'https://cdn.vietqr.io/img/KBANK.png', true),
    ('970466', 'KEBHANAHCM', 'KEBHanaHCM', 'Ngân hàng KEB Hana – Chi nhánh Thành phố Hồ Chí Minh', 'https://cdn.vietqr.io/img/KEBHANAHCM.png', false),
    ('970467', 'KEBHANAHN', 'KEBHANAHN', 'Ngân hàng KEB Hana – Chi nhánh Hà Nội', 'https://cdn.vietqr.io/img/KEBHANAHN.png', false),
    ('970452', 'KLB', 'KienLongBank', 'Ngân hàng TMCP Kiên Long', 'https://cdn.vietqr.io/img/KLB.png', true),
    ('970463', 'KBHCM', 'KookminHCM', 'Ngân hàng Kookmin - Chi nhánh Thành phố Hồ Chí Minh', 'https://cdn.vietqr.io/img/KBHCM.png', false),
    ('970462', 'KBHN', 'KookminHN', 'Ngân hàng Kookmin - Chi nhánh Hà Nội', 'https://cdn.vietqr.io/img/KBHN.png', false),
    ('970449', 'LPB', 'LPBank', 'Ngân hàng TMCP Lộc Phát Việt Nam', 'https://cdn.vietqr.io/img/LPB.png', true),
    ('977777', 'MAFC', 'MAFC', 'Công ty Tài chính TNHH MTV Mirae Asset (Việt Nam) ', 'https://cdn.vietqr.io/img/MAFC.png', false),
    ('970422', 'MB', 'MBBank', 'Ngân hàng TMCP Quân đội', 'https://cdn.vietqr.io/img/MB.png', true),
    ('970414', 'MBV', 'MBV', 'Ngân hàng TNHH MTV Việt Nam Hiện Đại', 'https://cdn.vietqr.io/img/MBV.png', true),
    ('971025', 'momo', 'MoMo', 'CTCP Dịch Vụ Di Động Trực Tuyến', 'https://cdn.vietqr.io/img/momo.png', true),
    ('970426', 'MSB', 'MSB', 'Ngân hàng TMCP Hàng Hải Việt Nam', 'https://cdn.vietqr.io/img/MSB.png', true),
    ('970428', 'NAB', 'NamABank', 'Ngân hàng TMCP Nam Á', 'https://cdn.vietqr.io/img/NAB.png', true),
    ('970419', 'NCB', 'NCB', 'Ngân hàng TMCP Quốc Dân', 'https://cdn.vietqr.io/img/NCB.png', true),
    ('801011', 'NHB HN', 'Nonghyup', 'Ngân hàng Nonghyup - Chi nhánh Hà Nội', 'https://cdn.vietqr.io/img/NHB.png', false),
    ('970448', 'OCB', 'OCB', 'Ngân hàng TMCP Phương Đông', 'https://cdn.vietqr.io/img/OCB.png', true),
    ('970430', 'PGB', 'PGBank', 'Ngân hàng TMCP Thịnh vượng và Phát triển', 'https://cdn.vietqr.io/img/PGB.png', true),
    ('970439', 'PBVN', 'PublicBank', 'Ngân hàng TNHH MTV Public Việt Nam', 'https://cdn.vietqr.io/img/PBVN.png', false),
    ('970412', 'PVCB', 'PVcomBank', 'Ngân hàng TMCP Đại Chúng Việt Nam', 'https://cdn.vietqr.io/img/PVCB.png', true),
    ('971133', 'PVDB', 'PVcomBank Pay', 'Ngân hàng TMCP Đại Chúng Việt Nam Ngân hàng số', 'https://cdn.vietqr.io/img/PVCB.png', true),
    ('970403', 'STB', 'Sacombank', 'Ngân hàng TMCP Sài Gòn Thương Tín', 'https://cdn.vietqr.io/img/STB.png', true),
    ('970400', 'SGICB', 'SaigonBank', 'Ngân hàng TMCP Sài Gòn Công Thương', 'https://cdn.vietqr.io/img/SGICB.png', true),
    ('970429', 'SCB', 'SCB', 'Ngân hàng TMCP Sài Gòn', 'https://cdn.vietqr.io/img/SCB.png', true),
    ('970440', 'SEAB', 'SeABank', 'Ngân hàng TMCP Đông Nam Á', 'https://cdn.vietqr.io/img/SEAB.png', true),
    ('970443', 'SHB', 'SHB', 'Ngân hàng TMCP Sài Gòn - Hà Nội', 'https://cdn.vietqr.io/img/SHB.png', true),
    ('970424', 'SHBVN', 'ShinhanBank', 'Ngân hàng TNHH MTV Shinhan Việt Nam', 'https://cdn.vietqr.io/img/SHBVN.png', true),
    ('970410', 'SCVN', 'StandardChartered', 'Ngân hàng TNHH MTV Standard Chartered Bank Việt Nam', 'https://cdn.vietqr.io/img/SCVN.png', false),
    ('970407', 'TCB', 'Techcombank', 'Ngân hàng TMCP Kỹ thương Việt Nam', 'https://cdn.vietqr.io/img/TCB.png', true),
    ('963388', 'TIMO', 'Timo', 'Ngân hàng số Timo by Ban Viet Bank (Timo by Ban Viet Bank)', 'https://vietqr.net/portal-service/resources/icons/TIMO.png', true),
    ('970423', 'TPB', 'TPBank', 'Ngân hàng TMCP Tiên Phong', 'https://cdn.vietqr.io/img/TPB.png', true),
    ('546035', 'Ubank', 'Ubank', 'TMCP Việt Nam Thịnh Vượng - Ngân hàng số Ubank by VPBank', 'https://cdn.vietqr.io/img/UBANK.png', true),
    ('970458', 'UOB', 'UnitedOverseas', 'Ngân hàng United Overseas - Chi nhánh TP. Hồ Chí Minh', 'https://cdn.vietqr.io/img/UOB.png', false),
    ('999888', 'VBSP', 'VBSP', 'Ngân hàng Chính sách Xã hội', 'https://cdn.vietqr.io/img/VBSP.png', false),
    ('970441', 'VIB', 'VIB', 'Ngân hàng TMCP Quốc tế Việt Nam', 'https://cdn.vietqr.io/img/VIB.png', true),
    ('970427', 'VAB', 'VietABank', 'Ngân hàng TMCP Việt Á', 'https://cdn.vietqr.io/img/VAB.png', true),
    ('970433', 'VIETBANK', 'VietBank', 'Ngân hàng TMCP Việt Nam Thương Tín', 'https://cdn.vietqr.io/img/VIETBANK.png', true),
    ('970454', 'VCCB', 'VietCapitalBank', 'Ngân hàng TMCP Bản Việt', 'https://cdn.vietqr.io/img/VCCB.png', true),
    ('970436', 'VCB', 'Vietcombank', 'Ngân hàng TMCP Ngoại Thương Việt Nam', 'https://cdn.vietqr.io/img/VCB.png', true),
    ('970415', 'ICB', 'VietinBank', 'Ngân hàng TMCP Công thương Việt Nam', 'https://cdn.vietqr.io/img/ICB.png', true),
    ('971005', 'VTLMONEY', 'ViettelMoney', 'Tổng Công ty Dịch vụ số Viettel - Chi nhánh tập đoàn công nghiệp viễn thông Quân Đội', 'https://cdn.vietqr.io/img/VIETTELMONEY.png', false),
    ('970406', 'Vikki', 'Vikki', 'Ngân hàng TNHH MTV Số Vikki', 'https://cdn.vietqr.io/img/Vikki.png', false),
    ('971011', 'VNPTMONEY', 'VNPTMoney', 'VNPT Money', 'https://cdn.vietqr.io/img/VNPTMONEY.png', false),
    ('970432', 'VPB', 'VPBank', 'Ngân hàng TMCP Việt Nam Thịnh Vượng', 'https://cdn.vietqr.io/img/VPB.png', true),
    ('970421', 'VRB', 'VRB', 'Ngân hàng Liên doanh Việt - Nga', 'https://cdn.vietqr.io/img/VRB.png', false),
    ('970457', 'WVN', 'Woori', 'Ngân hàng TNHH MTV Woori Việt Nam', 'https://cdn.vietqr.io/img/WVN.png', true);

-- ----- agent_profiles -----
ALTER TABLE agent_profiles ADD COLUMN address_province_id bigint REFERENCES locations (id);
ALTER TABLE agent_profiles ADD COLUMN bank_bin            varchar(8) REFERENCES banks (bin);
ALTER TABLE agent_profiles ADD COLUMN bank_account_number varchar(19);
ALTER TABLE agent_profiles ADD COLUMN bank_account_holder varchar(255);
ALTER TABLE agent_profiles DROP COLUMN bank_account;

-- ----- agent_profile_change_requests (NULL = không đổi) -----
ALTER TABLE agent_profile_change_requests ADD COLUMN address_province_id bigint REFERENCES locations (id);
ALTER TABLE agent_profile_change_requests ADD COLUMN bank_bin            varchar(8) REFERENCES banks (bin);
ALTER TABLE agent_profile_change_requests ADD COLUMN bank_account_number varchar(19);
ALTER TABLE agent_profile_change_requests ADD COLUMN bank_account_holder varchar(255);
ALTER TABLE agent_profile_change_requests DROP COLUMN bank_account;
