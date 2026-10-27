# Meizu mblu 22 Pro — iOS 18 & HyperOS System UI Overlay APK

Ứng dụng APK tùy biến giao diện hệ thống (**System UI Overlay — Không cần Root, Không cần Shizuku**) thiết kế riêng cho **Meizu mblu 22 Pro (Màn hình 6.79" HD+ 120Hz, Android 15 / Flyme OS)**.

---

## Tính năng tích hợp (All-in-One)

1. **Trung tâm điều khiển (Custom Control Center chuẩn iOS 18 / HyperOS 2.0):**
   - Vuốt từ góc trên bên phải màn hình xuống để mở.
   - Cụm **Kết nối 2x2** (Wi-Fi, Dữ liệu 4G LTE, Bluetooth, Chế độ Máy bay).
   - **Trình phát nhạc (Media Player)** điều khiển trực tiếp Spotify, YouTube Music, ZingMP3...
   - **2 Thanh trượt dọc (Vertical Capsule Sliders)** chỉnh **Độ sáng màn hình** và **Âm lượng** theo thời gian thực.
   - **4 Nút tác vụ nhanh:** Đèn Pin (bật tắt phần cứng tức thì), Xoay màn hình, Chế độ Âm báo (Chuông/Rung/Im lặng), Khóa màn hình 1 chạm.
   - **4 Phím tiện ích đáy:** Chụp màn hình, Đổi Theme nhanh 1 chạm, Mở Thông báo gốc, Mở Cài đặt máy.

2. **Đảo thích ứng (Dynamic Island) & Thanh trạng thái iOS 18:**
   - Căn chỉnh sẵn tọa độ chuẩn cho cụm camera đục lỗ giữa màn hình của **Meizu mblu 22 Pro** (kèm thanh trượt tinh chỉnh độ rộng, độ cao, khoảng cách mép trên theo ý thích).
   - **Chạm nhẹ:** Bung rộng Dynamic Island để điều khiển nhạc, bật đèn pin, chụp màn hình hoặc mở nhanh Control Center.
   - **Vuốt xuống từ Dynamic Island:** Mở ngay Control Center.

3. **Thanh trượt âm lượng nổi (Custom Volume HUD) & Hiệu ứng Sạc MagSafe 18W:**
   - Tự động bắt phím cứng Tăng/Giảm âm lượng để hiển thị thanh trượt nổi bên cạnh phải màn hình kiểu iOS 18 / HyperOS.
   - Tự động hiển thị hiệu ứng vòng sạc **18W HyperCharge / MagSafe** khi cắm cáp sạc.

4. **5 Chủ đề iOS 18 / HyperOS + Bộ tạo Hình nền HD+ (720×1640) đồng bộ:**
   - `iOS 18 Ultramarine Blur`
   - `HyperOS 2.0 Titanium`
   - `iOS 18 Desert Titanium`
   - `HyperOS Frost Mint`
   - `iOS 18 Dark Pro Max`
   - Hỗ trợ đổi đồng bộ cả **Giao diện System UI** lẫn **Hình nền Màn hình chính + Màn hình khóa** chỉ với 1 chạm.

---

## Cách lấy file `.apk` tự động qua GitHub Actions (Không cần cài Android Studio)

Dự án đã cấu hình sẵn workflow tại [`.github/workflows/build-apk.yml`](.github/workflows/build-apk.yml).

### Cách 1: Chạy script tự động đẩy lên GitHub
Mở PowerShell tại thư mục `e:\website\mblu22pro-theme-overlay` và chạy:
```powershell
.\push-to-github-build-apk.ps1
```

### Cách 2: Đẩy thủ công lên GitHub
1. Tạo 1 repository mới trên GitHub (ví dụ: `mblu22pro-theme-overlay`).
2. Chạy các lệnh sau trong thư mục `e:\website\mblu22pro-theme-overlay`:
   ```powershell
   git init
   git branch -M main
   git add .
   git commit -m "Initial commit: Meizu mblu 22 Pro iOS 18 & HyperOS System UI APK"
   git remote add origin https://github.com/<TEN_GITHUB_CUA_BAN>/mblu22pro-theme-overlay.git
   git push -u origin main
   ```
3. Vào tab **Actions** trên kho GitHub của bạn -> Chọn workflow **Build Meizu mblu 22 Pro Theme APK** -> Chờ khoảng **1.5 – 2 phút** -> Kéo xuống mục **Artifacts** và bấm tải **`Meizu-mblu22Pro-Theme-APK.zip`** (bên trong có sẵn file `.apk` đã ký để cài ngay vào điện thoại).

---

## Hướng dẫn kích hoạt trên Meizu mblu 22 Pro (Android 15)

Do **Android 15** bảo mật các ứng dụng cài ngoài bằng tính năng *Cài đặt bị hạn chế (Restricted Settings)*, sau khi cài file `.apk`, bạn làm đúng 3 bước ngay trong màn hình chính của App:
1. Bấm nút **Bước 1 (Mở TT Ứng dụng)** -> Chạm dấu **`⋮`** ở góc trên bên phải màn hình Thông tin ứng dụng -> Chọn **"Cho phép cài đặt bị hạn chế" (Allow restricted settings)** và xác nhận vân tay/mật khẩu máy.
2. Quay lại App, bấm **Bước 2 (Bật ngay)** -> Chọn **`mblu 22 Pro iOS 18 / HyperOS System UI`** -> Gạt **Bật**.
3. Bấm **Bước 3 (Cấp quyền)** để cho phép thanh trượt chỉnh độ sáng màn hình, và **Bước 4** để giữ giao diện chạy 24/7 không bị hệ thống tắt ngầm.
