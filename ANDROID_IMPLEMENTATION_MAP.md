# Peta implementasi source terbaru ke Android

| Source upstream | Peran dalam Android | Perlakuan |
|---|---|---|
| `src/core/program/parse.ts` | Parser program CNC | Dipakai sebagai source asli dalam bundle web |
| `src/core/interp/` | Interpreter G/M-code, axis, arcs, canned cycles, diagnostics, alarms | Dipakai sebagai source asli dalam bundle web |
| `src/core/macro/expr.ts` | Evaluasi ekspresi Macro B | Dipakai sebagai source asli dalam bundle web |
| `src/core/interp/variables.ts`, `flow.ts` | Variabel dan alur IF/WHILE/GOTO | Dipakai sebagai source asli dalam bundle web |
| `src/core/removal.ts` | Model pemakanan benda kerja | Dipakai sebagai source asli dalam bundle web |
| `src/renderer/simulation.ts` | Orkestrasi simulasi | Dipakai sebagai source asli dalam bundle web |
| `src/renderer/simulation.worker.ts` | Worker simulasi | Dibundel oleh Vite; wajib uji di WebView |
| `src/renderer/geometry/` | Geometri stock, toolpath, profile, measurement | Dipakai sebagai source asli dalam bundle web |
| `src/renderer/collisions.ts` | Deteksi tabrakan | Dipakai sebagai source asli dalam bundle web |
| `src/renderer/components/Viewport3D.tsx` | Viewport Three.js | Dirender di WebView dengan hardware acceleration |
| `src/renderer/components/Player.tsx` dan `usePlayback.ts` | Kontrol playback | UI asli; perlu uji sentuh |
| `src/renderer/platform/browser.ts` | Abstraksi platform non-Electron | Digunakan langsung |
| `src/renderer/platform/dom.ts` | Pemilih file dan download | Download disambungkan ke native Android bridge |
| `configs/` | Machine/control/tool/alarm JSON | Dipertahankan dan dibundel seperti sumber asli |

## Kenapa bukan rewrite Kotlin sekarang?

Menulis ulang parser, interpreter, Macro B, cycle, geometry, removal engine, collision engine, dan renderer sekaligus akan membuat dua mesin simulasi yang mudah berbeda hasilnya. Source terbaru sudah memisahkan browser platform dari Electron. WebView memungkinkan reuse mesin CNC dan UI yang sama dengan perubahan Android seminimal mungkin. Kotlin/Java hanya menjadi host Android dan jembatan fitur sistem operasi.
