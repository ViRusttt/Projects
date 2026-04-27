# NearNote — Location Reminder App

แอป Android สำหรับแจ้งเตือนตามสถานที่ต่างๆ เมื่อเข้าใกล้สถานที่ที่ตั้งไว้ จะแจ้งเตือนพร้อมโน้ตที่เขียนไว้

## MVP

1.สร้าง Location Reminder  
ค้นหาสถานที่หรือปักหมุดบนแผนที่  
กำหนดรัศมีการแจ้งเตือน  
เขียนโน้ต/รายการสิ่งที่ต้องทำ  
ตั้งชื่อสถานที  

2.แสดงรายการ Reminders
รายการ Reminders ทั้งหมด แสดงระยะห่างปัจจุบันจากแต่ละสถานที่  
เปิด/ปิดการแจ้งเตือนได้แบบ toggle  
แก้ไขและลบ reminder ได้  

3.การแจ้งเตือน
Push notification เมื่อเข้าใกล้สถานที่  
แสดงชื่อสถานที่ + โน้ตในการแจ้งเตือน  
กด notification เพื่อดูรายละเอียดเต็ม  

4.UI Reminder Detail  
แสดงแผนที่พร้อมตำแหน่ง  
แสดงโน้ตแบบ checklist (ติ๊กได้)  
ปุ่ม "Snooze" (แจ้งซ้ำอีกครั้งในครั้งต่อไปที่ผ่าน)  




## Tech Stack

- Kotlin + Jetpack Compose + Material 3
- Room Database
- Google Geofencing API 
- WorkManager

## หมายเหตุ
- ทดสอบบน Android 8.0 (API 26) ขึ้นไป
