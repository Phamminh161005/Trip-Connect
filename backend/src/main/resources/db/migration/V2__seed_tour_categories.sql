-- Danh mục loại hình tour chuẩn của hệ thống (trước đây seed trong DataSeeder).
-- ON CONFLICT DO NOTHING: DB cũ đã có sẵn các dòng này thì bỏ qua, không báo lỗi.
INSERT INTO tour_categories (name)
VALUES ('Nghỉ dưỡng'),
       ('Mạo hiểm'),
       ('Khám phá văn hóa'),
       ('Sinh thái'),
       ('Tâm linh - Hành hương'),
       ('Ẩm thực'),
       ('Biển đảo'),
       ('Núi rừng - Trekking'),
       ('Team building - MICE'),
       ('Gia đình'),
       ('Tuần trăng mật'),
       ('Cao cấp - Luxury'),
       ('Tiết kiệm - Backpacking'),
       ('Nhiếp ảnh'),
       ('Thể thao'),
       ('Nông thôn - Nông nghiệp'),
       ('Lễ hội'),
       ('Giáo dục - Học tập'),
       ('Chăm sóc sức khỏe - Wellness'),
       ('Du thuyền')
ON CONFLICT (name) DO NOTHING;
