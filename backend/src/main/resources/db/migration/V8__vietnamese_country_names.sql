-- =====================================================================
-- V8: Dịch tên các quốc gia hay đi du lịch sang tiếng Việt (bảng locations được seed từ API tên tiếng Anh).
-- Quốc gia có tên giống nhau (Singapore, Malaysia, Canada...) giữ nguyên.
-- DB mới tinh (chưa có dữ liệu lúc chạy migration): DataSeeder dịch luôn khi seed bằng CountryNames.java
-- — danh sách hai nơi giống hệt nhau, sửa một nơi thì sửa cả nơi kia.
-- =====================================================================

UPDATE locations AS l
SET country = v.vi
FROM (VALUES
    ('Thailand', 'Thái Lan'),
    ('Japan', 'Nhật Bản'),
    ('South Korea', 'Hàn Quốc'),
    ('North Korea', 'Triều Tiên'),
    ('China', 'Trung Quốc'),
    ('Taiwan', 'Đài Loan'),
    ('Hong Kong', 'Hồng Kông'),
    ('Macau', 'Ma Cao'),
    ('Cambodia', 'Campuchia'),
    ('Laos', 'Lào'),
    ('Timor-Leste', 'Đông Timor'),
    ('India', 'Ấn Độ'),
    ('Mongolia', 'Mông Cổ'),
    ('Turkey', 'Thổ Nhĩ Kỳ'),
    ('United Arab Emirates', 'Các Tiểu vương quốc Ả Rập Thống nhất'),
    ('Saudi Arabia', 'Ả Rập Xê Út'),
    ('Egypt', 'Ai Cập'),
    ('Morocco', 'Ma-rốc'),
    ('South Africa', 'Nam Phi'),
    ('France', 'Pháp'),
    ('Germany', 'Đức'),
    ('Italy', 'Ý'),
    ('Spain', 'Tây Ban Nha'),
    ('Portugal', 'Bồ Đào Nha'),
    ('United Kingdom', 'Anh'),
    ('Netherlands', 'Hà Lan'),
    ('Belgium', 'Bỉ'),
    ('Switzerland', 'Thụy Sĩ'),
    ('Austria', 'Áo'),
    ('Czech Republic', 'Séc'),
    ('Poland', 'Ba Lan'),
    ('Greece', 'Hy Lạp'),
    ('Norway', 'Na Uy'),
    ('Sweden', 'Thụy Điển'),
    ('Finland', 'Phần Lan'),
    ('Denmark', 'Đan Mạch'),
    ('Russia', 'Nga'),
    ('Ukraine', 'Ukraina'),
    ('United States', 'Hoa Kỳ'),
    ('Australia', 'Úc'),
    ('Vatican City State (Holy See)', 'Vatican')
) AS v(en, vi)
WHERE l.country = v.en
  AND l.province IS NULL;
