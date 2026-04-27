# NearNote — Location Reminder App

แอป Android สำหรับแจ้งเตือนตามสถานที่ เมื่อเข้าใกล้สถานที่ที่ตั้งไว้ จะแจ้งเตือนพร้อมโน้ตที่เขียนไว้

## วิธีเปิดใน Android Studio

1. แตกไฟล์ zip
2. เปิด Android Studio → File → Open → เลือกโฟลเดอร์ `nearnote`
3. รอ Gradle sync เสร็จ (ครั้งแรกอาจนาน 2-5 นาที)
4. กด Run ▶

## Features

- ✅ เพิ่ม reminder ผูกกับสถานที่บนแผนที่
- ✅ แผนที่ฟรี OSMDroid ไม่ต้อง API key
- ✅ เลื่อนซ้ายเพื่อลบ reminder (swipe to delete)
- ✅ Checklist ติ๊กได้ในหน้า detail
- ✅ Push notification เมื่อเข้าใกล้สถานที่
- ✅ Toggle เปิด/ปิดแต่ละ reminder
- ✅ ผ่อนผัน 2 ชั่วโมง

## Tech Stack

- Kotlin + Jetpack Compose + Material 3
- Room Database
- Google Geofencing API (ฟรี ไม่ต้องบัตร)
- OSMDroid (แผนที่ฟรี)
- WorkManager

## หมายเหตุ

- ต้องอนุญาต Location permission ทั้ง "While using" และ "Always" เพื่อให้ geofence ทำงานได้
- ทดสอบบน Android 8.0 (API 26) ขึ้นไป
