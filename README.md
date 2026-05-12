# MySawit Architecture Documentation

# 1. Current Architecture of MySawit

## 1.1 Current System Context Diagram

![Current System Context Diagram](docs/architecture/images/current-context.png)

Current context diagram menjelaskan posisi MySawit sebagai sistem utama yang berinteraksi dengan para pengguna dan external system. Admin Utama memiliki akses paling luas untuk mengelola user, kebun, assignment, payroll, dan proses operasional lain. Mandor berperan dalam supervisi hasil panen dan pengiriman. Buruh Sawit mencatat hasil panen. Supir Truk menjalankan pengiriman hasil panen ke pabrik.

## 1.2 Current Container Diagram

![Current Container Diagram](docs/architecture/images/current-container.png)

Current container diagram menunjukkan bahwa sistem sudah mulai dipisahkan berdasarkan domain. Modul kebun bertanggung jawab terhadap data kebun dan assignment. Modul pembayaran menangani payroll dan wallet. Modul pengiriman menangani proses pengangkutan hasil panen. PostgreSQL/PostGIS digunakan untuk menyimpan data kebun dan melakukan operasi spasial seperti validasi overlap kebun.

Kafka digunakan sebagai event broker supaya service dapat berkomunikasi secara asynchronous. Contohnya, ketika Mandor ditugaskan ke kebun, service kebun dapat menerbitkan event yang nantinya dapat digunakan oleh service lain seperti notification atau operation service.

## 1.3 Current Deployment Diagram

![Current Deployment Diagram](docs/architecture/images/current-deployment.png)

Current deployment diagram menunjukkan bahwa sistem masih berada pada tahap awal deployment. Backend Spring Boot dapat berjalan sebagai single Java process di server. Database PostgreSQL/PostGIS dan Kafka dapat dijalankan melalui Docker atau service terpisah.

Deployment seperti ini masih cukup untuk pengembangan awal. Namun, pendekatan single instance memiliki risiko availability karena jika satu server atau satu process bermasalah, sistem bisa ikut tidak dapat diakses.

## 1.4 Current Architecture Decisions and Trade-offs

Beberapa keputusan arsitektur saat ini adalah:

1. Menggunakan Spring Boot untuk backend service.
2. Menggunakan PostgreSQL/PostGIS untuk data kebun dan validasi spasial.
3. Menggunakan Kafka untuk komunikasi event-driven.
4. Memisahkan beberapa domain menjadi service berbeda seperti kebun, pembayaran, dan pengiriman.
5. Menggunakan DB-level locking pada validasi overlap kebun agar data kebun tetap konsisten saat ada request bersamaan.

Trade-off dari keputusan ini adalah sistem menjadi lebih siap untuk modularisasi, tetapi setup dan integrasi menjadi lebih kompleks dibanding satu monolithic application sederhana. Penggunaan Kafka juga membantu decoupling, tetapi membutuhkan konfigurasi tambahan dan pemahaman event flow yang lebih jelas.
