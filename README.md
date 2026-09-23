# Number Bot — Android

บอทสำหรับเกมกดตัวเลข 1 → 50 โดยใช้ Accessibility Gesture + MediaProjection + ML Kit OCR

## วิธี Build

1. เปิดโฟลเดอร์ `NumberBot` ด้วย Android Studio
2. รอ Gradle Sync
3. Build > Build APK(s)
4. ติดตั้ง APK ลง Android
5. เปิดแอป
6. กด `เปิด Accessibility` แล้วเปิด Number Bot
7. กลับเข้าแอป กด `START BOT`
8. อนุญาต "บันทึก/แชร์หน้าจอ"
9. เปิดเกมที่มีเลข 1–50

## หลักการทำงาน

- จับภาพหน้าจอด้วย MediaProjection
- ML Kit OCR หาเลขบนหน้าจอ
- หาเฉพาะเลขเป้าหมาย เช่น 1, 2, 3 ... 50 แบบ exact match
- ใช้ AccessibilityService dispatchGesture แตะตรงกลางของเลข
- ทำซ้ำจนถึง 50

## หมายเหตุ

ความเร็วจริงขึ้นกับ FPS ของเกม, ความเร็ว OCR, CPU/GPU และการวาดหน้าจอ
โปรเจกต์นี้เน้นความเร็ว แต่ไม่รับประกันเวลาหรือคะแนนของเกมใดเกมหนึ่ง
