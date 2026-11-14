# Port CNC Simulator ke Android

## Dasar proyek

Proyek ini dibuat dari arsip **`cnc-simulator-main.zip` terbaru yang diberikan untuk pekerjaan ini**. Proyek Android lama `Cnc5-main` dan `CNC_SmartGen_Stage12-14_Full_Project` tidak dipakai sebagai fondasi dan tidak digabungkan.

## Strategi implementasi

Source terbaru ternyata sudah memiliki mode browser:

- `src/renderer/platform/browser.ts` menyediakan penyimpanan browser, daftar file terakhir, impor file, dan penyimpanan pustaka tool.
- `src/renderer/platform/index.tsx` otomatis memilih platform browser saat `window.api` Electron tidak tersedia.
- `src/core/` berisi parser, interpreter, Macro B, siklus, alarm, konfigurasi, serta logika simulasi.
- `src/renderer/` berisi antarmuka React, playback, worker simulasi, geometri, pemakanan material, pemeriksaan tabrakan, dan viewport Three.js.

Karena itu, cara paling setia untuk membawa simulator terbaru ke Android adalah **mengemas renderer browser yang sudah ada ke dalam Android WebView**. Ini mempertahankan mesin CNC TypeScript dan tampilan Three.js yang sudah ada. Tidak perlu menulis ulang interpreter CNC dari awal di Kotlin.

## Struktur proyek

```text
CNC_Simulator_Android/
├── src/                         # source CNC Simulator terbaru (TypeScript/React)
│   ├── core/                    # parser, interpreter, macro, siklus, alarm
│   ├── renderer/                # UI, playback, worker, geometry, Three.js
│   ├── main/                    # host Electron asli (dipertahankan untuk desktop)
│   ├── preload/                 # API Electron asli (dipertahankan untuk desktop)
│   └── server/                  # mode browser/server asli
├── configs/                     # konfigurasi mesin, kontrol, tool, alarm
├── examples/                    # contoh program
├── tests/                       # pengujian source asli
├── android/
│   ├── settings.gradle
│   ├── build.gradle
│   ├── gradle.properties
│   └── app/
│       └── src/main/
│           ├── AndroidManifest.xml
│           ├── java/com/cncsmartgen/simulator/MainActivity.java
│           └── assets/www/       # hasil build renderer; diisi workflow
├── .github/workflows/
│   ├── ci.yml                    # workflow upstream
│   ├── release.yml               # workflow desktop upstream
│   └── android-apk.yml           # build APK Android
└── ANDROID_PORT_README.md
```

## Integrasi Android yang dibuat

1. `MainActivity.java` membuka renderer melalui AndroidX `WebViewAssetLoader` pada origin HTTPS lokal `appassets.androidplatform.net`.
2. JavaScript, DOM storage, hardware acceleration, dan WebGL diaktifkan untuk menjalankan UI dan viewport 3D.
3. `WebChromeClient` meneruskan input file HTML ke pemilih dokumen Android.
4. `src/renderer/platform/dom.ts` tetap menggunakan download browser biasa di browser desktop, tetapi mengalihkan unduhan ke bridge Android bila berjalan di WebView.
5. Bridge Android menyimpan program/proyek ke `Downloads/CNC Simulator` pada Android 10+, dan ke folder unduhan privat aplikasi pada Android versi lebih lama.
6. GitHub Actions membangun renderer web dari source terbaru, menyalin hasilnya ke `android/app/src/main/assets/www`, lalu membangun APK.

## Build APK melalui GitHub Actions

1. Unggah seluruh isi folder proyek ini ke repository GitHub baru atau repository yang memang akan dipakai untuk source terbaru.
2. Buka tab **Actions**.
3. Jalankan workflow **Build CNC Simulator Android APK** atau push ke branch `main`/`master`.
4. Setelah workflow sukses, unduh artifact **`cnc-simulator-android-debug`**. Di dalamnya terdapat `app-debug.apk`.

Workflow menjalankan `npm ci`, `npm run check`, `npm run build:only`, lalu Gradle `assembleDebug`.

## Build manual

Persyaratan: Node.js 22.12+, JDK 17, Android SDK platform 36, dan Gradle 8.13.

Dari root proyek:

```bash
npm ci
npm run check
npm run build:only
mkdir -p android/app/src/main/assets/www
cp -R out/renderer/. android/app/src/main/assets/www/
gradle -p android --no-daemon assembleDebug
```

APK: `android/app/build/outputs/apk/debug/app-debug.apk`.

## Status dan batasan tahap ini

Ini adalah **fondasi port Android yang menggunakan source simulator terbaru**, bukan aplikasi APK yang sudah diuji pada perangkat fisik. Build APK belum dapat diverifikasi di lingkungan pembuatan ini karena instalasi dependency npm tidak selesai dalam batas waktu yang tersedia dan Android SDK/Gradle lokal tidak tersedia.

Sebelum dipakai untuk pembelajaran atau validasi program, lakukan pengujian pada perangkat Android untuk:

- WebGL/Three.js dan performa viewport 3D;
- pemuatan `simulation.worker` pada WebView;
- impor `.nc`, `.txt`, `.cncsim`, dan JSON tool library;
- penyimpanan dan pembukaan ulang proyek;
- playback, single block, feed hold, override, timeline, dan seek mundur;
- mesin bubut dan machining center, semua kontrol yang tersedia, Macro B, siklus, alarm, offset, serta collision check;
- ukuran program besar, memori, suhu, dan orientasi layar.

Antarmuka sumber asli dirancang untuk desktop/browser lebar. Port ini menambahkan tata letak satu kolom untuk layar sempit, ukuran target sentuh minimum, penyesuaian panel operator, viewport, tombol playback, dan layar awal. Editor kode serta beberapa panel padat tetap membutuhkan pengujian pada ukuran layar HP yang sebenarnya; pengujian sentuh dan WebGL di perangkat fisik belum dilakukan.

## Lisensi

Source upstream menyatakan lisensi **GPL-3.0-or-later**. File `LICENSE`, dokumentasi, atribusi, dan pemberitahuan hak cipta upstream dipertahankan. Distribusi source turunan maupun APK harus mematuhi ketentuan GPL tersebut. Port Android ini tidak mengklaim kepemilikan atas source upstream.
