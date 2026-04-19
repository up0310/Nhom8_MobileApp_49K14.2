# Ứng dụng Nhắc Nhở Uống Nước - Giải thích chi tiết folder và file

Tài liệu này chỉ tập trung vào 1 việc: mô tả rõ từng folder/file trong dự án đang làm chức năng gì, để bạn hoặc thành viên mới có thể đọc code nhanh và đúng hướng.

## 1. Cấu trúc tổng quan

Dự án là Android app viết bằng Java + XML, dùng Gradle để build, Firebase để xác thực và đồng bộ dữ liệu.

Các khối chính:
- Cấu hình build cấp project.
- Module app chứa source code Android.
- Tài nguyên giao diện (layout, strings, drawable, menu, raw).
- Tài liệu mô tả hệ thống.

## 2. Nhóm file ở thư mục gốc

### build.gradle
- Chức năng: cấu hình Gradle cấp project.
- Ý nghĩa: khai báo plugin chung, repository, và thiết lập dùng cho toàn workspace.

### settings.gradle
- Chức năng: khai báo module thuộc project.
- Hiện tại: module chính là app.

### gradle.properties
- Chức năng: chứa tham số cho Gradle (tối ưu build, cờ hệ thống).

### gradlew, gradlew.bat, gradle/wrapper
- Chức năng: Gradle Wrapper.
- Ý nghĩa: giúp máy mới chạy đúng phiên bản Gradle mà không cần cài thủ công.

### local.properties
- Chức năng: trỏ đường dẫn Android SDK trên máy local.
- Lưu ý: file này mang tính máy cá nhân.

### google-services.json (ở root)
- Chức năng: file cấu hình Firebase bạn đang mở.
- Lưu ý: app Android thực tế dùng file trong folder app, vì vậy file root nên đồng bộ hoặc chỉ giữ làm bản tham chiếu.

### TAI_LIEU_HE_THONG.md
- Chức năng: tài liệu kỹ thuật tổng quan hệ thống (luồng, kiến trúc, giới hạn hiện tại, hướng cải tiến).

### README.md
- Chức năng: tài liệu onboarding nhanh theo góc nhìn folder/file (chính là file này).

## 3. Module app và ý nghĩa từng phần

### app/build.gradle
- Chức năng: cấu hình build cấp module app.
- Chứa: minSdk, targetSdk, dependencies, Firebase BOM, plugin google-services.

### app/google-services.json
- Chức năng: file Firebase chính mà app dùng khi build/chạy.
- Ảnh hưởng: project Firebase, app id, api key, realtime database endpoint.

### app/proguard-rules.pro
- Chức năng: rule cho shrink/obfuscation khi build release (nếu bật minify).

### app/build/
- Chức năng: output tạm trong quá trình build.
- Ví dụ: generated, intermediates, outputs APK.
- Lưu ý: đây không phải source code chính, có thể sinh lại.

## 4. Source code Java (nghiệp vụ chính)

Tất cả class Java nằm trong:
- app/src/main/java/com/example/nhacnhouongnuoc/

### MainActivity.java
- Vai trò: trung tâm ứng dụng.
- Chức năng chính:
1. Điều hướng 4 tab: Today, Stats, Explore, Settings.
2. Ghi nhận lượng nước theo ngày, cập nhật progress.
3. Đồng bộ local và Firebase (profile, daily, logs, reminders).
4. Quản lý UI settings: tài khoản, giao diện sáng/tối, ngôn ngữ.
5. Quản lý danh sách reminder ở màn hình settings.
6. Tải và hiển thị bài viết Explore từ RSS.

### AuthActivity.java
- Vai trò: màn hình đăng nhập.
- Chức năng:
1. Nhận email/password.
2. Gọi FirebaseAuth đăng nhập.
3. Điều hướng sang MainActivity khi thành công.

### RegisterActivity.java
- Vai trò: màn hình đăng ký tài khoản mới.
- Chức năng: tạo user bằng FirebaseAuth với email/password.

### ForgotPasswordActivity.java
- Vai trò: quên mật khẩu theo luồng OTP.
- Chức năng:
1. Gửi OTP qua EmailJS.
2. Verify OTP theo thời hạn.
3. Gọi reset password email qua FirebaseAuth.

### ArticleWebViewActivity.java
- Vai trò: hiển thị bài viết Explore.
- Chức năng: mở URL trong WebView, hỗ trợ điều hướng back trong web.

### ReminderScheduler.java
- Vai trò: tầng logic đặt lịch nhắc.
- Chức năng:
1. Lưu/đọc danh sách reminder từ SharedPreferences (JSON).
2. Tạo/cập nhật/xóa reminder.
3. Tính thời điểm kích hoạt kế tiếp theo repeat mask.
4. Đặt lịch bằng AlarmManager.
5. Reschedule toàn bộ reminder khi cần.

### ReminderReceiver.java
- Vai trò: BroadcastReceiver nhận sự kiện đến giờ nhắc.
- Chức năng:
1. Nếu reminder kiểu notification: hiện thông báo + âm thanh ngắn.
2. Nếu kiểu alarm: kích hoạt service phát chuông.
3. Tạo/đảm bảo notification channel.
4. Sau khi fire, đặt lại lịch lần tiếp theo.

### ReminderBootReceiver.java
- Vai trò: receiver chạy sau khi reboot hoặc app update.
- Chức năng: gọi reschedule để khôi phục toàn bộ reminder đã lưu.

### AlarmPlaybackService.java
- Vai trò: foreground service phát âm báo thức.
- Chức năng: chạy âm thanh alarm ổn định ở background, hỗ trợ hành động dừng.

### AlarmAlertActivity.java
- Vai trò: màn hình full-screen khi alarm reo.
- Chức năng: cho người dùng thao tác tắt chuông nhanh.

## 5. AndroidManifest và quyền hệ thống

### app/src/main/AndroidManifest.xml
- Chức năng: khai báo toàn bộ thành phần Android app.
- Bao gồm:
1. Quyền mạng, notification, exact alarm, boot completed, foreground service.
2. Activity, Service, Receiver của ứng dụng.
3. Launcher activity là MainActivity.

## 6. Tài nguyên giao diện (res)

### app/src/main/res/layout/
- Chức năng: XML giao diện.
- Nhóm chính:
1. activity_*.xml: giao diện cấp màn hình (Auth, Register, Forgot, Main, WebView, Alarm).
2. fragment_*.xml: giao diện từng tab trong MainActivity (home, stats, explore, settings).
3. dialog_*.xml: giao diện popup (set goal, reminder schedule, reminder slot, log water, profile...).

### app/src/main/res/menu/
- Chức năng: định nghĩa menu điều hướng đáy.
- File quan trọng:
1. bottom_nav_menu.xml: khai báo 4 tab và icon tương ứng.

### app/src/main/res/values/
- Chức năng: tài nguyên text/theme chung.
- File quan trọng:
1. strings.xml: chuỗi tiếng Việt chính.
2. colors.xml, themes.xml: màu và theme giao diện.
3. arrays (nếu có): dữ liệu spinner/list.

### app/src/main/res/values-en/
- Chức năng: bản dịch tiếng Anh cho chuỗi giao diện.

### app/src/main/res/values-night/
- Chức năng: biến thể tài nguyên cho dark mode.

### app/src/main/res/drawable/
- Chức năng: shape, background, icon vector dùng trong UI.

### app/src/main/res/raw/
- Chức năng: âm thanh báo nhắc (notification/alarm).

### app/src/main/res/xml/
- Chức năng: cấu hình backup/data extraction rules.

## 7. Khu vực test

### app/src/test/java/
- Chức năng: unit test chạy trên JVM local.

### app/src/androidTest/java/
- Chức năng: instrumentation test chạy trên thiết bị hoặc emulator.

## 8. Luồng dữ liệu theo lớp file

Luồng chính khi app chạy:
1. MainActivity khởi tạo Firebase, theme/language, và điều hướng tab.
2. Người dùng thao tác ghi nước hoặc chỉnh settings.
3. Dữ liệu local lưu trong SharedPreferences.
4. Nếu đã đăng nhập, MainActivity đồng bộ lên Firebase Realtime Database.
5. ReminderScheduler đặt lịch hệ thống.
6. Đến giờ, ReminderReceiver xử lý fire notification/alarm.

## 9. Đọc code theo thứ tự để hiểu nhanh

Đề xuất đọc theo thứ tự này:
1. app/src/main/AndroidManifest.xml
2. app/src/main/java/com/example/nhacnhouongnuoc/MainActivity.java
3. app/src/main/java/com/example/nhacnhouongnuoc/AuthActivity.java
4. app/src/main/java/com/example/nhacnhouongnuoc/RegisterActivity.java
5. app/src/main/java/com/example/nhacnhouongnuoc/ForgotPasswordActivity.java
6. app/src/main/java/com/example/nhacnhouongnuoc/ReminderScheduler.java
7. app/src/main/java/com/example/nhacnhouongnuoc/ReminderReceiver.java
8. app/src/main/java/com/example/nhacnhouongnuoc/AlarmPlaybackService.java
9. app/src/main/java/com/example/nhacnhouongnuoc/ReminderBootReceiver.java
10. app/src/main/res/layout/fragment_home.xml
11. app/src/main/res/layout/fragment_stats.xml
12. app/src/main/res/layout/fragment_explore.xml
13. app/src/main/res/layout/fragment_settings.xml

## 10. Ghi chú quan trọng

1. Dữ liệu build tạm trong app/build không phải nơi sửa logic nghiệp vụ.
2. Khi đổi Firebase project, cần kiểm tra cả app/google-services.json và URL DB trong MainActivity.
3. Luồng OTP qua EmailJS hiện đang gọi từ client, nên cân nhắc đưa sang backend để an toàn hơn.

## 11. Phân chia nhiệm vụ cho 5 người

Mục này là bản phân công theo module để học và triển khai song song.

### Người 1: Explore (Bài viết + Công cụ)
1. Phụ trách tab Explore gồm articles và health tools.
2. Làm rõ luồng gọi RSS, parse dữ liệu, bind card, mở webview.
3. Tài liệu hóa công thức các tool BMI, BMR, TDEE, body fat, calories.
4. File chính cần đọc/làm:
- app/src/main/java/com/example/nhacnhouongnuoc/MainActivity.java
- app/src/main/java/com/example/nhacnhouongnuoc/ArticleWebViewActivity.java
- app/src/main/res/layout/fragment_explore.xml
- app/src/main/res/layout/activity_article_webview.xml
5. Đầu ra bắt buộc:
- Sơ đồ luồng Explore từ API đến UI.
- Bảng giải thích từng tool và input/output.

### Người 2: Home + Đăng nhập/Đăng xuất Firebase
1. Phụ trách luồng vào app và trạng thái tài khoản.
2. Quản lý login, register, logout, cập nhật UI theo trạng thái user.
3. Kiểm thử các case sai mật khẩu, chưa đăng nhập, chuyển màn hình.
4. File chính cần đọc/làm:
- app/src/main/java/com/example/nhacnhouongnuoc/MainActivity.java
- app/src/main/java/com/example/nhacnhouongnuoc/AuthActivity.java
- app/src/main/java/com/example/nhacnhouongnuoc/RegisterActivity.java
- app/src/main/res/layout/activity_auth.xml
- app/src/main/res/layout/activity_register.xml
- app/src/main/res/layout/fragment_home.xml
5. Đầu ra bắt buộc:
- Tài liệu luồng login/register/logout.
- Checklist test account flow.

### Người 3: Quên mật khẩu OTP + Ngôn ngữ + Profile cloud
1. Phụ trách luồng forgot password 3 bước: gửi OTP, xác minh OTP, reset password.
2. Phụ trách chuyển ngôn ngữ vi/en và lưu language vào profile trên Firebase.
3. Kiểm tra timeout OTP, nhập sai OTP, email mismatch.
4. File chính cần đọc/làm:
- app/src/main/java/com/example/nhacnhouongnuoc/ForgotPasswordActivity.java
- app/src/main/java/com/example/nhacnhouongnuoc/MainActivity.java
- app/src/main/res/layout/activity_forgot_password.xml
- app/src/main/res/layout/fragment_settings.xml
- app/src/main/res/values/strings.xml
- app/src/main/res/values-en/strings.xml
5. Đầu ra bắt buộc:
- Sequence flow OTP.
- Bảng mapping trường language local và cloud.

### Người 4: Lịch sử + Thống kê + Streak + Theme sáng/tối
1. Phụ trách tab Stats: chart 7 ngày, recent logs, full history dialog.
2. Phụ trách logic streak và achievement.
3. Phụ trách dark mode và xác minh áp theme đúng.
4. File chính cần đọc/làm:
- app/src/main/java/com/example/nhacnhouongnuoc/MainActivity.java
- app/src/main/res/layout/fragment_stats.xml
- app/src/main/res/layout/dialog_log_water.xml
- app/src/main/res/values/strings.xml
5. Đầu ra bắt buộc:
- Tài liệu công thức streak/achievement.
- Kết quả test chart, history, theme.

### Người 5: Settings Reminder + Notification/Alarm Scheduler
1. Phụ trách tạo/sửa/xóa lịch uống nước trong Settings.
2. Phụ trách scheduler, receiver, full-screen alarm, reschedule sau reboot.
3. Xác minh quyền notification/exact alarm và hoạt động thực tế theo giờ.
4. File chính cần đọc/làm:
- app/src/main/java/com/example/nhacnhouongnuoc/MainActivity.java
- app/src/main/java/com/example/nhacnhouongnuoc/ReminderScheduler.java
- app/src/main/java/com/example/nhacnhouongnuoc/ReminderReceiver.java
- app/src/main/java/com/example/nhacnhouongnuoc/ReminderBootReceiver.java
- app/src/main/java/com/example/nhacnhouongnuoc/AlarmPlaybackService.java
- app/src/main/java/com/example/nhacnhouongnuoc/AlarmAlertActivity.java
- app/src/main/res/layout/fragment_settings.xml
- app/src/main/res/layout/dialog_reminder_schedule.xml
- app/src/main/res/layout/dialog_reminder_slot.xml
- app/src/main/AndroidManifest.xml
5. Đầu ra bắt buộc:
- Sơ đồ end-to-end reminder.
- Checklist test nhắc giờ thường, lặp theo ngày, reboot máy.

### Cách phối hợp 5 người
1. Người 2 và Người 3 chốt account flow trước để có user test ổn định.
2. Người 5 làm reminder song song, cung cấp kịch bản test cho Người 4.
3. Người 4 dùng dữ liệu log thật để kiểm tra stats/streak.
4. Người 1 hoàn thiện Explore, không phụ thuộc mạnh vào các module khác.
5. Cuối vòng, cả nhóm chạy regression test toàn app theo checklist chung.

---