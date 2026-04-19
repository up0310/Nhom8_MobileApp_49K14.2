# Tài liệu giải thích toàn bộ hệ thống

## 1) Tổng quan
Ứng dụng Nhắc nhở uống nước là app Android (Java, minSdk 24, targetSdk 36) giúp người dùng:
- theo dõi lượng nước đã uống trong ngày,
- đặt mục tiêu nước cá nhân,
- tạo nhiều lịch nhắc (notification hoặc alarm),
- xem thống kê và thành tích,
- đồng bộ dữ liệu qua Firebase Auth + Realtime Database,
- đọc bài viết và dùng các công cụ sức khỏe trong tab Explore.

Công nghệ chính:
- Android SDK + AndroidX (AppCompat, Material, ConstraintLayout)
- Firebase Auth (đăng nhập Email/Password)
- Firebase Realtime Database (lưu profile, nhắc, log, lịch sử)
- Firebase Analytics (ghi sự kiện sử dụng)

## 2) Cấu trúc tổng thể
Hệ thống theo hướng 1 Activity chính + nhiều màn hình bổ trợ:
- MainActivity: trung tâm UI/logic (Today, Stats, Explore, Settings)
- AuthActivity: đăng nhập
- RegisterActivity: đăng ký
- ForgotPasswordActivity: quên mật khẩu (OTP qua EmailJS + reset)
- ArticleWebViewActivity: mở bài viết trong WebView
- AlarmAlertActivity: full-screen cảnh báo khi alarm rung
- ReminderScheduler: quản lý lịch nhắc, lưu JSON, set/cancel alarm
- ReminderReceiver: nhận sự kiện đến giờ, bắn notification hoặc kích hoạt service alarm
- AlarmPlaybackService: foreground service phát chuông alarm
- ReminderBootReceiver: khôi phục lịch sau reboot/update app

## 3) Kiến trúc dữ liệu và lưu trữ
### 3.1 Lưu trữ local (SharedPreferences)
Toàn bộ dữ liệu local nằm trong pref app_settings, gồm:
- Cài đặt UI: dark mode, language
- Hồ sơ: cân nặng, chiều cao, tuổi, mức vận động, khí hậu
- Mục tiêu nước: mục tiêu đề xuất, min/max, mục tiêu ngày
- Log nước theo ngày: key dạng pref_intake_YYYYMMDD
- Lịch sử ghi nước: JSON array trong pref_log_history
- Danh sách reminder: JSON array trong pref_reminder_items
- Các key legacy reminder và key migration

### 3.2 Lưu trữ cloud (Firebase Realtime Database)
Base URL:
- xxx

Mẫu dữ liệu theo user:
- users/{uid}/profile
- users/{uid}/daily/{YYYYMMDD}
- users/{uid}/reminders/itemsJson
- users/{uid}/logs/historyJson

Xác thực dùng Firebase Authentication (Email/Password).

## 4) Điều hướng và UI
Bottom navigation có 4 tab:
- Today: tiến độ trong ngày, ghi nhanh 200/350/500ml, đặt mục tiêu
- Stats: chart 7 ngày, tổng nước, trung bình, achievement, log gần đây
- Explore: bài viết hydration + công cụ sức khỏe (BMI/BMR/TDEE/...)
- Settings: tài khoản, đồng bộ, giao diện, ngôn ngữ, reminder, profile

MainActivity không dùng Fragment class riêng mà inflate layout theo tab vào FrameLayout.

## 5) Luồng chức năng chính
### 5.1 Khởi động app
1. Áp dụng theme/ngôn ngữ đã lưu.
2. Khởi tạo Firebase Analytics/Auth/Database.
3. Nếu chưa đăng nhập thì mở AuthActivity.
4. Tạo notification channel, reschedule reminder từ local.
5. Mở tab cuối người dùng đã chọn.
6. Nếu lần đầu sử dụng thì hiện onboarding tính mục tiêu nước.

### 5.2 Ghi nước (Today)
1. Người dùng bấm quick-log hoặc nhập số ml.
2. App cộng vào intake của ngày hiện tại (pref_intake_YYYYMMDD).
3. Ghi thêm log vào pref_log_history.
4. Cập nhật progress UI (% và progress bars).
5. Nếu đã đăng nhập thì đẩy dữ liệu ngày hiện tại + logs lên Firebase.
6. Ghi event Analytics (water_logged).

### 5.3 Tính mục tiêu nước
Dữ liệu đầu vào:
- cân nặng, tuổi, mức vận động

Logic tính:
- Chọn hệ số ml/kg tùy activity/độ tuổi
- Tính min/max/target
- Làm tròn bước 50 ml
- Giới hạn trong khoảng an toàn (1200-7000 ml)

Kết quả được lưu local và đồng bộ cloud nếu đã đăng nhập.

### 5.4 Reminder (notification/alarm)
Mô hình reminder:
- mỗi reminder có id, giờ, phút, type, enabled, repeatMask, alarmSound
- repeatMask là bitmask 7 ngày (T2..CN)

Đặt lịch:
- ReminderScheduler tính lần kích hoạt tiếp theo theo giờ + repeat day
- dùng AlarmManager.setExactAndAllowWhileIdle
- Android 12+ có kiểm tra canScheduleExactAlarms()

Khi đến giờ:
- ReminderReceiver nhận broadcast
- Nếu type=notification: hiện notification + âm ngắn
- Nếu type=alarm: start AlarmPlaybackService (foreground) + full-screen intent (AlarmAlertActivity)
- Sau khi xử lý, reminder tự reschedule lần tiếp theo

### 5.5 Đồng bộ dữ liệu tài khoản
App có cơ chế sync 2 chiều local <-> cloud:
- Khi user thay đổi dữ liệu local quan trọng thì đẩy lên cloud
- Khi login/chuyển account/onResume thì tải cloud xuống local
- Nếu phát hiện user mới với data local và cloud cùng có dữ liệu thì ưu tiên push local (logic hiện tại)
- Khi logout thì xóa dữ liệu local theo account scope, hủy reminder, quay về Auth

Có auto-sync cooldown 15 giây để tránh gọi liên tục.

### 5.6 Explore
- Bài viết fallback có sẵn trong strings.
- App gọi RSS từ Google News query hydration.
- Parse XML RSS, lấy tối đa 3 bài, làm sạch HTML summary, ước lượng thời gian đọc.
- Bấm bài viết thì mở ArticleWebViewActivity.
- Ngoài bài viết, tab này có bộ calculator sức khỏe:
1. Body fat
2. BMI
3. BMR
4. TDEE
5. Daily calories
6. Calories burn
7. Healthy weight

## 6) Bảo mật và quyền
### 6.1 Permissions (Manifest)
- INTERNET
- POST_NOTIFICATIONS
- RECEIVE_BOOT_COMPLETED
- SCHEDULE_EXACT_ALARM
- FOREGROUND_SERVICE
- USE_FULL_SCREEN_INTENT

### 6.2 Lưu ý kỹ thuật bảo mật
ForgotPasswordActivity hiện có thông tin EmailJS endpoint/service/template/public key/private key hard-code trong app.
Đây là điểm nhạy cảm (dễ lộ secret khi reverse APK). Nên đưa phần gửi OTP qua backend an toàn thay vì gọi trực tiếp từ client.

## 7) Resource và đa ngôn ngữ
- Chuỗi giao diện chính ở res/values/strings.xml (VI)
- Bản dịch EN ở res/values-en/strings.xml
- Alarm sound và notification sound ở res/raw/*

Người dùng có thể đổi ngôn ngữ ngay trong app (vi/en).

## 8) Build và cấu hình
### 8.1 Thông số build
- AGP: 9.1.0
- Java compatibility: 11
- compileSdk/targetSdk: 36
- minSdk: 24

### 8.2 Dependencies chính
- androidx.appcompat, material, activity, constraintlayout
- Firebase BOM + analytics + auth + database

### 8.3 Entry point
- Launcher activity: MainActivity

## 9) Luồng sự kiện quan trọng
### 9.1 Reboot device
- ReminderBootReceiver nhận BOOT_COMPLETED hoặc MY_PACKAGE_REPLACED
- gọi ReminderScheduler.rescheduleFromStorage()
- đảm bảo channel notification tồn tại

### 9.2 Alarm full-screen
- Alarm đến giờ -> ReminderReceiver
- start AlarmPlaybackService foreground
- service hiển thị notification có fullScreenIntent mở AlarmAlertActivity
- user bấm Dừng chuông -> gửi ACTION_STOP_ALARM cho service

## 10) Giới hạn hiện tại và hướng cải tiến
- MainActivity đang gom nhiều logic UI + business; nên tách Fragment/ViewModel/Repository để dễ bảo trì.
- Có một số key/string legacy (âm thanh alarm cũ) được map để tương thích ngược.
- OTP flow hiện tại dựa vào EmailJS từ client; nên chuyển qua server.
- Đồng bộ local-cloud chưa có conflict resolution nâng cao (chưa merge theo timestamp từng trường).

## 11) Các file quan trọng để đọc nhanh
- app/src/main/java/com/example/nhacnhouongnuoc/MainActivity.java
- app/src/main/java/com/example/nhacnhouongnuoc/ReminderScheduler.java
- app/src/main/java/com/example/nhacnhouongnuoc/ReminderReceiver.java
- app/src/main/java/com/example/nhacnhouongnuoc/AlarmPlaybackService.java
- app/src/main/java/com/example/nhacnhouongnuoc/AuthActivity.java
- app/src/main/java/com/example/nhacnhouongnuoc/RegisterActivity.java
- app/src/main/java/com/example/nhacnhouongnuoc/ForgotPasswordActivity.java
- app/src/main/AndroidManifest.xml
- app/build.gradle

## 12) Hướng dẫn chạy dự án trên máy mới
### 12.1 Chuẩn bị môi trường
1. Cài Android Studio bản mới ổn định (khuyến nghị dùng bản có hỗ trợ AGP 9.x).
2. Cài Android SDK Platform 36 và Build-Tools tương ứng.
3. Cài JDK 17 (hoặc JDK mà Android Studio đang khuyến nghị cho AGP hiện tại).
4. Cài Git.
5. Có thiết bị thật bật USB debugging hoặc tạo Android Emulator (API 24+).

### 12.2 Lấy mã nguồn
1. Clone project về máy.
2. Mở thư mục gốc dự án bằng Android Studio.
3. Đợi Gradle Sync hoàn tất.

### 12.3 Cấu hình Firebase bắt buộc
1. Tạo (hoặc dùng sẵn) Firebase project.
2. Thêm Android app với package name: com.example.nhacnhouongnuoc.
3. Tải file google-services.json và đặt vào thư mục app/google-services.json.
4. Trong Firebase Console:
   - bật Authentication: Email/Password,
   - bật Realtime Database,
   - kiểm tra URL database trùng với FIREBASE_DB_URL đang dùng trong mã.

Nếu URL database khác, sửa hằng FIREBASE_DB_URL trong MainActivity cho đúng môi trường của bạn.

### 12.4 Build và chạy bằng Android Studio
1. Mở Android Studio, chọn Open và trỏ tới thư mục gốc dự án.
2. Chờ Android Studio index source + hoàn tất Gradle Sync (thanh trạng thái không còn chạy).
3. Nếu hiện cảnh báo JDK/Gradle:
   - vào File > Settings > Build, Execution, Deployment > Build Tools > Gradle,
   - chọn Gradle JDK là JDK 17,
   - bấm Sync Project with Gradle Files.
4. Kiểm tra file app/google-services.json đã có trong project (nếu thiếu thì Firebase sẽ lỗi khi chạy).
5. Chọn cấu hình chạy app ở góc trên (module app).
6. Chọn thiết bị chạy:
   - Thiết bị thật: bật Developer options và USB debugging, cắm cáp và chấp nhận RSA prompt.
   - Emulator: mở Device Manager, tạo máy ảo API 24+ rồi bấm Start.
7. Nhấn Run (Shift+F10) để build và cài app.
8. Sau khi app mở lên:
   - cấp quyền Notification khi được hỏi,
   - đăng ký hoặc đăng nhập tài khoản,
   - thử tạo 1 reminder để kiểm tra luồng thông báo/alarm.

Gợi ý khi chạy lần đầu:
- Nếu app không build được, thử Build > Clean Project rồi Build > Rebuild Project.
- Nếu Android Studio không nhận thiết bị thật, kiểm tra adb devices trong Terminal của Android Studio.
- Nếu đăng nhập lỗi cấu hình, kiểm tra lại Firebase Auth đã bật Email/Password.

### 12.5 Build và chạy bằng dòng lệnh (Windows)
Tại thư mục gốc dự án:

```powershell
.\gradlew.bat clean assembleDebug
adb install -r "app\build\outputs\apk\debug\app-debug.apk"
adb shell am start -n com.example.nhacnhouongnuoc/.MainActivity
```

### 12.6 Checklist kiểm tra nhanh sau khi chạy
1. Đăng nhập/đăng ký hoạt động.
2. Ghi nước hiển thị đúng ở tab Today.
3. Tab Stats có dữ liệu 7 ngày và log gần đây.
4. Tạo reminder notification và nhận được nhắc đúng giờ.
5. Tạo reminder alarm và nút Dừng chuông hoạt động.
6. Đổi ngôn ngữ vi/en và app cập nhật giao diện.
7. Đăng xuất rồi đăng nhập lại, dữ liệu đồng bộ đúng.

### 12.7 Lỗi hay gặp trên máy mới
1. Lỗi thiếu google-services.json: app không kết nối Firebase.
2. Lỗi Auth operation not allowed/configuration not found: chưa bật Email/Password trong Firebase Auth.
3. Không nhận thông báo: chưa cấp quyền POST_NOTIFICATIONS (Android 13+).
4. Alarm không chính xác: thiết bị chặn exact alarm hoặc chế độ tiết kiệm pin quá gắt.
5. Build lỗi JDK/Gradle: kiểm tra lại JDK mà Android Studio dùng và sync lại Gradle.

## 13) Mô tả đặc thù folder code
### 13.1 Thư mục gốc dự án
- build.gradle: cấu hình build cấp project (plugin chung, cấu hình dùng cho toàn workspace).
- settings.gradle: khai báo module của project (hiện tại có module app).
- gradle.properties: tham số cho Gradle (tối ưu build, cờ hệ thống).
- gradlew, gradlew.bat, gradle/wrapper: Gradle Wrapper để máy mới chạy đúng phiên bản Gradle mà không cần tự cài riêng.
- local.properties: đường dẫn SDK trên máy local (không nên commit giá trị cá nhân).
- build/: output build cấp project, không phải source code.

### 13.2 Module app
- app/build.gradle: cấu hình build của ứng dụng Android, dependency, minSdk/targetSdk, plugin Firebase.
- app/google-services.json: file cấu hình Firebase cho app Android theo package name.
- app/proguard-rules.pro: rule cho shrink/obfuscation (dùng khi build release có minify).
- app/build/: output build của module app (APK, intermediates, generated...), có thể xóa và build lại.

### 13.3 Source code chính
- app/src/main/java/com/example/nhacnhouongnuoc: toàn bộ mã Java nghiệp vụ.
   - MainActivity: điều hướng 4 tab, xử lý logic chính, sync dữ liệu.
   - Auth/Register/ForgotPassword: luồng xác thực tài khoản.
   - ReminderScheduler/Receiver/BootReceiver: hệ thống đặt lịch và nhận nhắc.
   - AlarmPlaybackService + AlarmAlertActivity: phát chuông nền + màn hình full-screen khi báo thức.
   - ArticleWebViewActivity: hiển thị bài viết qua WebView.

### 13.4 Tài nguyên giao diện và dữ liệu tĩnh
- app/src/main/res/layout: XML layout cho activity/fragment/item/dialog.
- app/src/main/res/values: chuỗi tiếng Việt, màu sắc, theme.
- app/src/main/res/values-en: chuỗi tiếng Anh (đa ngôn ngữ).
- app/src/main/res/values-night: biến thể giao diện tối.
- app/src/main/res/menu: menu điều hướng đáy (bottom nav).
- app/src/main/res/drawable: shape/icon/vector/background.
- app/src/main/res/raw: file âm thanh alarm/notification.
- app/src/main/res/xml: rule backup/data extraction.

### 13.5 Manifest và test
- app/src/main/AndroidManifest.xml: khai báo permission, activity, service, receiver, launcher.
- app/src/test/java: unit test chạy JVM local.
- app/src/androidTest/java: instrumentation test chạy trên thiết bị/emulator.

---
Tài liệu này mô tả hệ thống theo mã nguồn hiện tại trong repo. Nếu cần, có thể tạo thêm bản kiến trúc đề xuất (refactor plan theo module) ở tài liệu tiếp theo.
