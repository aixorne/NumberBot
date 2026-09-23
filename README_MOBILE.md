# สร้าง APK ด้วยมือถืออย่างเดียว

วิธีที่ง่ายที่สุดคือ GitHub Actions ไม่ต้องติดตั้ง Android Studio บนมือถือ

1. สร้างบัญชี GitHub
2. สร้าง Repository ใหม่ เช่น `NumberBot`
3. แตก ZIP แล้วอัปโหลดไฟล์ทั้งหมดเข้า Repository
4. เปิดแท็บ Actions
5. เลือก workflow `Build Number Bot APK`
6. กด `Run workflow`
7. รอจนขึ้นเครื่องหมายเขียว
8. เข้า run ที่สำเร็จ → ส่วน Artifacts → ดาวน์โหลด `NumberBot-debug`
9. แตก ZIP จะได้ `app-debug.apk`
10. เปิด APK เพื่อติดตั้ง

ถ้า Actions ไม่เริ่มจากการ push:
Actions → Build Number Bot APK → Run workflow

หมายเหตุ:
- GitHub จะเป็นเครื่องที่ Build APK ให้
- มือถือใช้แค่เบราว์เซอร์/แอป GitHub ก็ได้
- APK เป็น debug APK สำหรับติดตั้งทดสอบ
