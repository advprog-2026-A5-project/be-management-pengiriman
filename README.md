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