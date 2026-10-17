# ⚡ FlashGallery v1.18

[![Release](https://img.shields.io/github/v/release/WoRFReD/FlashGallery?color=00e5ff&label=Release&logo=github)](https://github.com/WoRFReD/FlashGallery/releases/latest)
[![Android](https://img.shields.io/badge/Android-8.0%2B%20(API%2026%2B)-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9%2B-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Donate PayPal](https://img.shields.io/badge/Donate-PayPal-blue.svg?logo=paypal)](https://paypal.me/WoRFReD)
[![YouTube](https://img.shields.io/badge/YouTube-FlashChip-red?logo=youtube)](https://youtube.com/@flashchiped)

[Español](#español) | [English](#english)

---

<a name="español"></a>
## 🇪🇸 Español

**FlashGallery** es una galería multimedia y gestor de documentos nativo para Android de alto rendimiento, diseñada con **Jetpack Compose** bajo una estética futurista y minimalista estilo *Dark/Cyberpunk*. Ofrece visualización fluida de fotos y vídeos, reconocimiento OCR en el dispositivo, visores avanzados de documentos (PDF, código con resaltado de sintaxis, Office) y un sistema integrado de actualizaciones OTA directamente desde GitHub Releases.

---

### 🚀 Características Destacadas

* **📸 Visor Multimedia Ultrafluido:**
  * Deslizamiento horizontal instantáneo entre fotos y vídeos con respuesta táctil inmediata.
  * **Pinch-to-zoom continuo:** Zoom suave de 1x a 5x que sigue el movimiento de los dedos en tiempo real sin cortes ni tirones.
  * **Pan libre y centrado inteligente:** Desplazamiento fluido en imágenes ampliadas y doble toque para ampliar directamente sobre el punto deseado.
  * Reproducción nativa de vídeo integrada con **AndroidX Media3 ExoPlayer**.

* **🔍 Reconocimiento de Texto OCR / Live Text (100% Offline):**
  * **Pulsación Prolongada Inteligente:** Deja pulsado sobre cualquier texto dentro de una foto (la típica pegatina de router WiFi con la clave, números de serie, códigos, tickets) para seleccionarlo de inmediato.
  * Cálculo milimétrico de coordenadas sobre la imagen, incluso si está ampliada con zoom.
  * **Panel de Selección Interactiva:** Resalta la clave en tipografía monoespaciada legible, botón *"Copiar clave"* en un solo toque, lista interactiva de líneas detectadas y opción para compartir.
  * Impulsado por **Google ML Kit Text Recognition** en local, garantizando privacidad total y velocidad extrema sin necesidad de conexión.

* **📑 Visor de Documentos Universal:**
  * **Visor Nativo de PDF:** Renderizado de alta definición página a página con `PdfRenderer`, paginador con slider interactivo y zoom.
  * **Visor de Código estilo Notepad++:** Coloreado de sintaxis inteligente para XML, JSON, HTML, TXT, LOG, INI y scripts. Barra de números de línea (*gutter*), buscador en tiempo real y ajuste de línea (*word wrap*).
  * **Suite Office Integrada:** Abre y lee documentos de Microsoft Word (`.docx`, `.doc`), Excel (`.xlsx`, `.csv`) con cuadrícula de celdas bidireccional y PowerPoint (`.pptx`) con diapositivas estructuradas sin necesidad de instalar suites pesadas.

* **🔄 Actualizaciones OTA Automáticas:**
  * Comprobador inteligente que compara la versión instalada contra los lanzamientos de **GitHub Releases**.
  * Diálogo de actualización integrado con registro de cambios, descarga directa del instalador APK y ejecución segura de la instalación.

* **🗂️ Organización Inteligente y Selección Múltiple:**
  * Agrupación cronológica por meses con barra adhesiva (*sticky header*) y selector interactivo para saltar de forma inmediata a cualquier mes o año.
  * Modo de selección múltiple por pulsación larga para compartir, mover o eliminar lotes de archivos con confirmación de seguridad.
  * Pestañas especializadas para Fotos, Vídeos, Documentos y APKs.

* **ℹ️ Inspector EXIF Detallado:**
  * Información técnica completa: modelo de cámara, apertura, ISO, velocidad de obturación, distancia focal, resolución y geolocalización.

---

### ⚙️ Arquitectura Técnica

* **Lenguaje:** Kotlin 1.9+
* **Framework UI:** Jetpack Compose (Material 3 con personalización Cyberpunk neón)
* **Carga de Imágenes:** Coil 3 con decodificación asíncrona y caché de memoria/disco
* **Motor Multimedia:** AndroidX Media3 (ExoPlayer)
* **Motor OCR:** Google ML Kit Text Recognition (On-Device)
* **Renderizado PDF:** Android Native `PdfRenderer`
* **Compilación y Releases:** Gradle 8.4, ProGuard/R8, firmado automático y GitHub CLI (`gh`)

---

### 📜 Historial de Versiones (Changelog)

| Versión | Fecha | Resumen de Cambios |
| :---: | :---: | :--- |
| **v1.18** | 17/10/2026 | **Firma Oficial de Desarrollador (Keystore Maestro FlashChip) y accesibilidad con fuentes grandes:** Incorporación de ScrollableTabRow en pestañas superiores y scroll horizontal con `maxLines = 1` en selectores de modo para evitar cortes de texto con fuentes aumentadas. Firma oficial RSA 4096-bit con validez de 30 años a nombre de WoRFReD / FlashChip, guía oficial de firma y asistente de Play Protect. |
| **v1.17** | 17/10/2026 | **Zoom táctil ultrafluido:** Reescritura del motor de gestos multitáctiles (pinch-to-zoom continuo de 1x a 5x, desplazamiento panorámico con límites y doble toque con centrado al punto pulsado). Mapeo de coordenadas OCR en fotos ampliadas y presentación oficial en GitHub. |
| **v1.16** | 17/10/2026 | **OCR / Live Text en fotos:** Reconocimiento offline con Google ML Kit, pulsación prolongada sobre textos/pegatinas de router para copiar contraseñas, panel interactivo y botón de escaneo directo. |
| **v1.15** | 17/10/2026 | **Soporte Office Universal:** Visor nativo para Word (.docx, .doc), Excel (.xlsx, .csv con cuadrícula 2D) y PowerPoint (.pptx). |
| **v1.14** | 17/10/2026 | Corrección de scroll horizontal en Notepad++ y resolución de bloqueos al abrir volcados de texto largos. |
| **v1.13** | 17/10/2026 | Botón de compartir en selección múltiple y corrección de recorte visual de checkboxes en cuadrícula. |
| **v1.12** | 17/10/2026 | Compilación Release oficial firmada y resolución de bucle en detección OTA. |
| **v1.11** | 17/10/2026 | Sistema de actualizaciones OTA desde GitHub Releases y script de publicación desatendido con GitHub CLI. |
| **v1.10** | 17/10/2026 | Visor nativo de PDF y Visor de código estilo Notepad++ con coloreado de sintaxis y buscador. |
| **v1.09** | 17/10/2026 | Eliminación de cabecera duplicada y rediseño minimalista de separadores de mes. |
| **v1.08** | 17/10/2026 | Diálogo interactivo para salto rápido a cualquier Mes y Año. |
| **v1.07** | 16/10/2026 | Modo de selección múltiple y ajuste de sensibilidad de swipe. |
| **v1.06** | 16/10/2026 | Pestañas de Documentos y APKs con contadores en tiempo real. |
| **v1.05** | 16/10/2026 | Ficha técnica y visualizador de metadatos EXIF. |
| **v1.04** | 16/10/2026 | Visor a pantalla completa con navegación por swipe lateral y controles inmersivos. |
| **v1.03** | 16/10/2026 | Agrupación cronológica por meses con barra adhesiva dinámica. |
| **v1.02** | 16/10/2026 | Cuadrícula fluida con LazyVerticalGrid y caché con Coil. |
| **v1.01** | 16/10/2026 | Versión inicial con permisos Android 14 y arquitectura Jetpack Compose. |

---

<a name="english"></a>
## 🇬🇧 English

**FlashGallery** is a high-performance Android gallery and universal document manager built 100% natively with **Jetpack Compose** following a futuristic Dark/Cyberpunk aesthetic. It provides butter-smooth photo and video playback, on-device OCR text selection, advanced document viewers (PDF, syntax-highlighted code, Office), and an automated OTA updater powered directly by GitHub Releases.

---

### 🚀 Key Features

* **📸 Ultra-Smooth Media Viewer:**
  * Lightning-fast horizontal swipe between pictures and videos.
  * **Continuous Pinch-to-Zoom:** Natural, fluid 1x to 5x zoom that follows your fingers in real time without stutters or interruptions.
  * **Free Pan & Centered Double-Tap:** Effortless panning across zoomed images and smart double-tap to zoom into the exact tapped area.
  * Native video playback powered by **AndroidX Media3 ExoPlayer**.

* **🔍 On-Device OCR / Live Text Selection (100% Offline):**
  * **Smart Long-Press:** Long-press anywhere on a photo (such as the WiFi password sticker behind a router, serial numbers, labels, receipts) to immediately isolate and extract text.
  * High-precision coordinate mapping even when the photo is zoomed in.
  * **Interactive Selection Sheet:** Displays passwords in clear monospace font, a one-tap *"Copy Key"* button, detected line-by-line list, and sharing options.
  * Powered locally by **Google ML Kit Text Recognition** for complete privacy and instant response without internet access.

* **📑 Universal Document Viewer:**
  * **Native PDF Reader:** Crisp page-by-page rendering via Android's `PdfRenderer`, interactive page slider, and zoom gestures.
  * **Notepad++ Style Code Viewer:** Syntax highlighting for XML, JSON, HTML, TXT, LOG, INI, and source code. Includes line number gutter, keyword finder, and word wrap toggle.
  * **Built-in Office Viewer:** Read Microsoft Word (`.docx`, `.doc`), Excel (`.xlsx`, `.csv` with 2D spreadsheet grid), and PowerPoint (`.pptx`) presentations natively with zero third-party office apps needed.

* **🔄 Automated GitHub OTA Updates:**
  * Built-in update checker that checks against the latest **GitHub Releases**.
  * In-app updater dialog with release notes, download progress bar, and seamless APK package installation.

* **🗂️ Smart Organization & Batch Management:**
  * Chronological month grouping with sticky headers and a quick jump dialog to travel to any month or year.
  * Long-press multi-selection mode to share, relocate, or batch delete media safely.
  * Categorized tabs for Photos, Videos, Documents, and APK files.

* **ℹ️ Deep EXIF Inspector:**
  * Detailed camera metadata: device model, aperture, shutter speed, ISO, focal length, image resolution, and location tags.

---

### 📥 Download & Installation

You can download the ready-to-install **FlashGallery.apk** directly from the [Latest GitHub Release](https://github.com/WoRFReD/FlashGallery/releases/latest). Once installed, FlashGallery will notify you automatically when new updates become available.

---

## by WoRFReD
Canal: [FlashChip](https://youtube.com/@flashchiped)

### ☕ ¿Te ha sido útil este proyecto? / Support this project
Si esta aplicación te ha servido de ayuda y quieres apoyar el desarrollo de nuevas herramientas, aplicaciones y proyectos para el canal:

[![Donate with PayPal](https://img.shields.io/badge/Donate-PayPal-blue.svg?logo=paypal)](https://paypal.me/WoRFReD)
