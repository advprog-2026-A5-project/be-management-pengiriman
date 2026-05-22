# Profiling dan Analisis Improvement Pengiriman
## Kenapa Profiling Ini Dipakai

Profiling dipilih karena service ini dipenuhi endpoint read-heavy yang dipanggil lintas role, misalnya `MANDOR`, `SUPIR`, dan `ADMIN`. Pada jenis workload seperti ini, bottleneck biasanya tidak terlihat dari rata-rata response time saja, tetapi dari tail latency seperti `P95` dan `P99`.

Metode ini juga penting karena beberapa endpoint bergantung pada service lain dan query data yang berulang. Dengan JMeter, pola akses bisa disimulasikan secara konsisten, lalu hasilnya dibaca dengan metrik yang langsung menunjukkan dampak ke user experience.

Alasan metrik yang dipakai:

- `P95` dan `P99` dipakai untuk melihat lonjakan latency pada request terburuk.
- `Apdex` dipakai untuk menilai apakah response time masih aman untuk pengguna.
- `Error rate` dipakai untuk memastikan optimisasi tidak memperbaiki latency tapi merusak stabilitas.
## Hasil Sebelum ditingkatkan

### 1. Profil keseluruhan before

![Profil Before](docs/ProfileBefore.png)

### 2. P95 before

![P95 Before](docs/p95Before.png)

### 3. Apdex before

![Apdex Before](docs/ApdexBefore.png)

### Analisis

Hasil sebelum ditingkatkan performa menunjukkan ada endpoint yang memiliki latency tinggi di ekor distribusi, terutama endpoint read yang bergantung pada data lintas service. Di kondisi ini, rata-rata response time saja tidak cukup karena beberapa request masih melonjak jauh di atas request lain.

Endpoint yang paling perlu diperhatikan adalah endpoint yang melakukan berhubungan ke service lain atau query data yang berulang. Jika latency P95 tinggi, user akan merasakan aplikasi lambat walaupun rata-rata terlihat masih biasa saja.

## Hasil setelah ditingkatkan

### 1. Profil keseluruhan after

![Profil After](docs/ProfileAfter.png)

### 2. P95 after

![P95 After](docs/p95After.png)

### 3. Apdex after

![Apdex After](docs/ApdexAfter.png)

### Analisis

Hasil after dipakai untuk memverifikasi bahwa perubahan yang dilakukan memang menurunkan tail latency dan memperbaiki pengalaman pengguna. Fokus evaluasinya tetap sama: apakah `P95` turun, `Apdex` naik, dan apakah error rate tetap terkendali.

Improvement yang paling masuk akal untuk workload seperti ini adalah:

- caching data yang sering dibaca ulang,
- memperpendek timeout ke dependency agar request tidak menunggu terlalu lama,
- mengurangi repeated lookup ke service lain,
- dan menyiapkan index yang sesuai untuk query history dan filter data.

Di implementasi service ini, arah optimisasi yang relevan sudah terlihat di konfigurasi cache dan timeout pada [AppConfig.java](src/main/java/id/ac/ui/cs/advprog/bemanagementpengiriman/config/AppConfig.java). Cache dipakai untuk data yang sering diakses berulang, sedangkan timeout yang wajar akan mencegah request tersangkut terlalu lama ketika dependency lambat.

## Kesimpulan Improvement

Profiling ini valid untuk service pengiriman karena beban utamanya adalah request baca yang berulang dan sensitif terhadap latency tail. Dengan membandingkan before dan after lewat `P95`, `P99`, dan `Apdex`, kita bisa melihat apakah optimisasi benar-benar berdampak pada pengalaman pengguna.
# MySawit Architecture Documentation



# README Modul 9

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


# 2. Architectural Risk Analysis

## 2.1 Why Risk Storming Was Applied

Risk Storming dipakai karena arsitektur MySawit yang sekarang masih cukup aman untuk skala kecil. Namun, jika MySawit benar-benar digunakan oleh banyak unit kebun, maka masalah yang awalnya kecil bisa menjadi lebih besar.

Beberapa risiko sebenarnya sudah mulai dimitigasi. Contohnya pada modul kebun, validasi overlap tidak hanya mengandalkan pengecekan di level aplikasi, tetapi sudah diperkuat dengan DB-level locking. Ini membuat data kebun lebih aman ketika ada beberapa request yang mencoba membuat atau mengubah data kebun secara bersamaan.

Namun, walaupun satu risiko sudah ditangani, masih ada risiko lain yang perlu diperhatikan. Misalnya autentikasi dan otorisasi, single instance deployment, observability, pembayaran, wallet, backup database, dan komunikasi antarmodul. Dengan Risk Storming, kami mencoba melihat bagian mana yang masih bisa menjadi masalah jika sistem berkembang lebih besar.

## 2.2 Success Scenario

Misalnya MySawit berhasil digunakan oleh banyak kebun sawit milik BurhanSawit. Pengguna aktif tidak lagi hanya beberapa orang, tetapi bisa mencapai ratusan atau ribuan pengguna yang terdiri dari Admin Utama, Mandor, Buruh Sawit, dan Supir Truk.

Pada kondisi itu, aktivitas sistem akan meningkat. Admin sering membuat dan mengubah data kebun, Mandor memvalidasi hasil panen, Supir Truk mengubah status pengiriman, dan sistem pembayaran perlu membuat payroll secara berkala. Database juga akan menyimpan banyak data historis seperti hasil panen, pengiriman, assignment, payroll, dan perubahan saldo wallet.
harus dilakukan dengan sistem yang harus lebih stabil, mudah dipantau, punya backup, dan tidak langsung mati jika satu server atau satu process mengalami error.

## 2.3 Risk Identification

| ID | Area | Risiko | Dampak Utama | Kemungkinan | Dampak | Skor | Mitigasi |
|---|---|---|---|---|---|---|---|
| R1 | Autentikasi dan otorisasi | Role atau identitas user terlalu dipercaya dari request tanpa validasi token yang kuat | Security | High | High | 9 | Gunakan autentikasi terpusat dengan JWT atau OAuth2, lalu validasi token di gateway atau service |
| R2 | Locking kebun | DB-level locking sudah mengurangi race condition overlap, tetapi masih perlu dipastikan tidak menjadi bottleneck saat traffic tinggi | Performance, maintainability | Low | Medium | 2 | Pantau performa query locking, gunakan index spasial yang tepat, dan dokumentasikan transaksi kritis |
| R3 | Deployment single instance | Jika backend hanya berjalan pada satu EC2 atau satu process, sistem bisa down saat server bermasalah | Availability | Medium | High | 6 | Gunakan reverse proxy atau load balancer dengan lebih dari satu instance |
| R4 | Observability belum konsisten | Logging, metrics, dan tracing belum seragam di semua service | Maintainability | High | Medium | 6 | Standarisasi log, metrics, health check, dan dashboard monitoring |
| R5 | Pembayaran dan wallet | Perhitungan payroll dan perubahan saldo harus sangat akurat, kalau tidak bisa menyebabkan data uang salah | Data integrity | Medium | High | 6 | Gunakan tipe data presisi seperti BigDecimal, transaksi yang jelas, idempotency, dan audit log |
| R6 | Modul belum lengkap | Beberapa modul seperti pengiriman, notifikasi, atau auth penuh belum terlihat matang sehingga integrasi bisa terlambat | Maintainability | High | Medium | 6 | Tentukan kontrak API dan event sejak awal, walaupun implementasi belum lengkap |
| R7 | Komunikasi antarmodul | Kalau semua modul saling panggil langsung, sistem akan makin sulit diubah dan rawan coupling | Modifiability | Medium | Medium | 4 | Gunakan event broker untuk proses asynchronous seperti assignment, payroll, dan notifikasi |
| R8 | Backup dan recovery | Belum terlihat strategi backup dan restore database yang jelas | Recoverability | Medium | High | 6 | Buat backup otomatis, aturan retensi, dan prosedur restore |
| R9 | Upload bukti panen | Jika foto hasil panen disimpan langsung di server aplikasi, storage bisa cepat penuh dan sulit dipindahkan | Scalability | Medium | Medium | 4 | Gunakan object storage untuk file bukti panen |
| R10 | Data assignment | Assignment Mandor, Buruh, dan Supir berpengaruh ke banyak modul, sehingga data yang tidak sinkron bisa mengganggu operasi | Consistency | Medium | High | 6 | Gunakan event assignment dan aturan ownership yang jelas antarservice |

## 2.4 Risk Consensus

Dari risiko yang ditemukan, risiko paling penting bukan hanya yang paling banyak terjadi, tetapi yang paling besar dampaknya ke sistem kalau benar-benar terjadi.

Risiko pertama yang paling penting adalah masalah autentikasi dan otorisasi. Karena MySawit punya beberapa role, yaitu Admin Utama, Mandor, Buruh Sawit, dan Supir Truk, kesalahan validasi role bisa membuat user mengakses fitur yang seharusnya tidak boleh diakses.

Risiko kedua adalah ketersediaan sistem. Kalau sistem hanya berjalan di satu server atau satu process, maka ketika server tersebut mati, semua pengguna tidak bisa menggunakan aplikasi. Ini tidak masalah saat demo, tetapi akan bermasalah jika sistem digunakan secara rutin.

Risiko ketiga adalah pembayaran dan wallet. Karena payroll berhubungan dengan saldo dan upah, kesalahan kecil seperti rounding, update ganda, atau transaksi yang tidak konsisten bisa menyebabkan data finansial salah.

Risiko keempat adalah integrasi antarmodul. MySawit memiliki banyak modul yang saling terhubung, seperti kebun, hasil panen, pengiriman, pembayaran, dan notifikasi. Jika komunikasi antarmodul tidak jelas, maka sistem akan sulit dikembangkan oleh banyak anggota kelompok.

Untuk validasi overlap kebun, risiko race condition sudah tidak menjadi risiko utama lagi karena sudah ada DB-level locking. Namun, bagian ini tetap perlu dipantau karena locking yang terlalu berat dapat mempengaruhi performa ketika jumlah request meningkat.

## 2.5 Risk Mitigation

## R1 Autentikasi dan Otorisasi

Perubahan arsitektur yang disarankan adalah menambahkan API Gateway dan autentikasi berbasis JWT atau OAuth2. Dengan ini, setiap request membawa token yang sudah ditandatangani, dan service tidak asal percaya pada role dari header biasa.

Keuntungannya, akses antar role jadi lebih aman dan konsisten. Admin, Mandor, Buruh, dan Supir bisa dibatasi sesuai hak akses masing-masing. Kekurangannya, implementasi menjadi lebih kompleks karena perlu mengatur token, masa berlaku token, refresh token, dan validasi di setiap service.

## R2 Locking Kebun dan Validasi Overlap

Pada versi sebelumnya, validasi overlap kebun dianggap berisiko karena jika ada beberapa instance backend, pengecekan overlap di level aplikasi saja bisa gagal. Risiko ini sekarang sudah berkurang karena sistem sudah menerapkan DB-level locking.

Dengan DB-level locking, operasi yang sensitif terhadap konsistensi data kebun dapat dikontrol langsung di database. Jadi ketika ada request yang mencoba membuat atau mengubah kebun secara bersamaan, database dapat membantu menjaga agar proses validasi dan penyimpanan tidak saling bertabrakan.

Risiko yang tersisa bukan lagi correctness utama, tetapi lebih ke performa dan maintainability. Jika jumlah request bertambah besar, locking yang terlalu luas bisa membuat beberapa request harus menunggu lebih lama. Karena itu, mitigasi lanjutannya adalah memastikan query sudah memakai index yang tepat, terutama untuk data spasial, dan bagian transaksi yang memakai lock harus dibuat sesingkat mungkin.

## R3 Single Instance Deployment

Untuk kondisi awal, menjalankan backend di satu EC2 masih wajar. Namun, kalau project sukses, single instance menjadi risiko besar karena tidak ada cadangan jika server atau process mati.

Mitigasinya adalah menjalankan beberapa instance backend di belakang reverse proxy atau load balancer. Dengan ini, jika satu instance mati, request masih bisa diarahkan ke instance lain. Trade-off-nya adalah deployment menjadi lebih rumit dan membutuhkan konfigurasi tambahan seperti health check, environment variable yang konsisten, dan shared database yang aman.

## R4 Observability

Sistem yang terdiri dari banyak service akan sulit didebug kalau tidak punya observability yang baik. Masalahnya, error bisa terjadi di service kebun, pembayaran, pengiriman, atau event broker, tetapi penyebabnya tidak langsung kelihatan.

Mitigasinya adalah membuat standar logging, metrics, tracing, dan health check untuk semua service. Minimal, setiap service punya endpoint health check dan log yang jelas. Untuk versi lebih baik, bisa digunakan Prometheus, Grafana, dan OpenTelemetry. Trade-off-nya adalah ada tambahan konfigurasi dan resource untuk monitoring.

## R5 Pembayaran dan Wallet

Pembayaran dan wallet harus diperlakukan lebih hati-hati dibanding CRUD biasa. Kalau payroll sudah diterima, saldo user bertambah dan saldo Admin Utama berkurang. Jika operasi ini gagal di tengah atau diproses dua kali, data bisa menjadi tidak konsisten.

Mitigasinya adalah menggunakan tipe data presisi seperti BigDecimal, transaksi database yang jelas, idempotency key, dan audit log. Dengan begitu, sistem bisa mencegah pembayaran ganda dan tetap punya catatan perubahan saldo. Trade-off-nya adalah implementasi payment service menjadi lebih panjang dan perlu testing lebih serius.

## R6 Modul Belum Lengkap

Beberapa modul seperti pengiriman, notifikasi, auth penuh, dan hasil panen mungkin belum semuanya matang. Risiko dari kondisi ini adalah integrasi antar anggota kelompok menjadi tidak jelas, karena setiap modul bisa punya asumsi masing-masing.

Mitigasinya adalah membuat kontrak API dan event sejak awal. Misalnya, service kebun menerbitkan event saat Mandor ditugaskan, lalu notification service atau modul lain cukup consume event tersebut. Dengan ini, walaupun implementasi belum selesai, batas antar modul sudah lebih jelas.

## R7 Komunikasi Antarmodul

Jika semua modul saling memanggil langsung dan membaca database service lain, sistem akan sulit dikembangkan. Perubahan kecil di satu modul bisa merusak modul lain.

Mitigasinya adalah menggunakan komunikasi yang jelas, misalnya REST untuk request yang butuh jawaban langsung dan Kafka/event broker untuk proses asynchronous. Contohnya assignment Mandor atau Supir bisa menghasilkan event, lalu modul notifikasi atau pengiriman dapat bereaksi tanpa harus langsung bergantung ke database kebun.

## R8 Backup dan Recovery

Database menyimpan data penting seperti data kebun, assignment, pengiriman, payroll, dan wallet. Jika database rusak atau hilang tanpa backup, sistem akan sulit dipulihkan.

Mitigasinya adalah membuat backup otomatis dan prosedur restore. Untuk tahap awal, backup harian sudah cukup. Untuk tahap lebih serius, perlu restore drill agar tim tahu apakah backup benar-benar bisa digunakan.

## 2.6 Architecture Modification Justification

Dari hasil Risk Storming, arsitektur masa depan tidak hanya dibuat agar terlihat lebih besar, tetapi untuk menjawab risiko yang memang masih relevan dari arsitektur sekarang.

API Gateway ditambahkan untuk membantu validasi akses dan membuat request masuk lebih terkontrol. Load balancer dan multiple backend instance ditambahkan untuk mengurangi risiko downtime. PostgreSQL/PostGIS tetap digunakan karena cocok untuk data koordinat kebun, dan untuk bagian kebun sudah diperkuat dengan DB-level locking agar operasi overlap-sensitive lebih aman. Kafka digunakan untuk komunikasi asynchronous agar modul seperti kebun, pengiriman, pembayaran, dan notifikasi tidak saling bergantung terlalu kuat.

Selain itu, Redis atau cache bisa digunakan untuk data yang sering dibaca, misalnya daftar kebun atau data assignment, tetapi tidak boleh menjadi sumber kebenaran utama. Object storage juga lebih cocok untuk bukti foto hasil panen daripada menyimpannya langsung di server aplikasi.


# 3. Future Architecture

## 3.1 Future Architecture Goals

Future architecture dibuat untuk menjawab risiko yang muncul dari current architecture. Tujuan utamanya adalah membuat MySawit lebih aman, lebih scalable, lebih mudah dipantau, dan lebih jelas batas antar modulnya.

Pada future architecture, sistem diarahkan untuk memiliki API Gateway, service yang lebih jelas berdasarkan domain, event broker untuk komunikasi asynchronous, object storage untuk file bukti hasil panen, observability stack, dan database yang lebih siap untuk backup serta recovery.

## 3.2 Future System Context Diagram

![Future System Context Diagram](docs/architecture/images/future-context.png)

Future context diagram menunjukkan bahwa MySawit tidak hanya menjadi aplikasi internal sederhana, tetapi menjadi platform operasional yang terhubung dengan identity provider, payment gateway, storage, dan monitoring. Sistem ini perlu menangani lebih banyak pengguna dan proses bisnis yang lebih kritis.

## 3.3 Future Container Diagram

![Future Container Diagram](docs/architecture/images/future-container.png)

Future container diagram menunjukkan bahwa setiap service memiliki tanggung jawab yang lebih jelas. Auth Service menangani login dan role. Kebun Service menangani data kebun dan assignment. Harvest Service menangani laporan hasil panen. Shipping Service menangani pengiriman. Payment Service menangani payroll dan wallet. Notification Service menangani pesan kepada user.

Kafka digunakan untuk event seperti assignment, harvest approved, shipment approved, payroll created, dan notification requested. Dengan cara ini, setiap service tidak harus langsung membaca database service lain.

## 3.4 How Future Architecture Mitigates Risks

Future architecture membantu mengurangi risiko dengan beberapa cara:

1. API Gateway dan Auth Service mengurangi risiko akses tidak sah.
2. Load balancer dan multiple backend instance mengurangi risiko downtime.
3. Kafka mengurangi coupling antarmodul.
4. Object storage membantu mengelola file bukti hasil panen dengan lebih scalable.
5. Monitoring dan logging membantu debugging ketika sistem mulai besar.
6. Backup database membantu recovery jika terjadi masalah data.
7. DB-level locking tetap digunakan pada modul kebun untuk menjaga konsistensi data spasial.

## 3.5 Future Architecture Trade-offs

Arsitektur future menjadi lebih kompleks. Jumlah service bertambah, deployment lebih sulit, dan observability menjadi lebih penting. Tim juga harus lebih disiplin dalam membuat kontrak API dan event agar service tidak saling bergantung secara sembarangan.

Namun, trade-off ini masuk akal karena MySawit punya banyak alur bisnis yang saling terhubung. Kalau semuanya tetap dibuat terlalu sederhana, sistem akan sulit berkembang ketika jumlah data, user, dan transaksi bertambah.




# 4. Individual Work: Pengiriman Module

## 4.1 Individual Container Diagram

![Individual Container Diagram](docs/architecture/images/individual_container.png)

## 4.2 Individual Component Diagram
![Individual Component Diagram](docs/architecture/images/individual-component.png)

## 4.3 Code Diagram 1 - Class Diagram

![Code Diagram 1](docs/architecture/images/code_diagram_1.png)

## 4.5 Code Diagram 2 - Lifecycle Sequence

![Code Diagram 2](docs/architecture/images/code_diagram_2.png)

