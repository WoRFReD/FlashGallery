# 📋 HISTORIAL Y CONTROL DE VERSIONES
> **Proyecto:** FlashGallery  
> **Autor:** WoRFReD • **Canal:** FlashChip (https://youtube.com/@flashchiped)  
> **Versión Actual:** 1.19 • **Código:** 119 • **Fecha de Inicio:** 14/10/2026  
> **Ubicación:** C:\APPs_Atigravity\FlashGallery_APK  

---

## 📌 Guía de Clasificación de Cambios
Cada nueva versión debe documentar sus modificaciones usando estos identificadores:
* 🟢 **[NUEVA_FUNCION]**: Incorporación de nuevas pantallas, herramientas o capacidades.
* 🟡 **[INTERFAZ]**: Cambios visuales, temas, colores, animaciones, tipografías o layouts.
* 🔴 **[CORRECCION]**: Solución de bugs, crashes, cierres inesperados o fallos de compilación.
* 🔵 **[OPTIMIZACION]**: Reducción de tiempos de carga, memoria, peso de APK o rendimiento.
* 🟣 **[SEGURIDAD]**: Permisos de Android, firmas, certificados o compatibilidad.

---

## 🚀 Versión 1.19 (Código: 119) — 02/11/2026
### *Visor predeterminado para PDFs y documentos externos, y solución definitiva al mover fotos*

* 🔴 **[CORRECCION]**: Solución definitiva al mover imágenes o vídeos a otra carpeta: se preserva rigurosamente la fecha original de captura y modificación (dateTaken y lastModified), actualizando el registro de MediaStore en su lugar o sincronizándolo sin eliminar el elemento. En la pestaña 'Línea de tiempo' la foto permanece visible en su posición cronológica exacta y en 'De tus apps' pasa a la nueva carpeta.
* 🟢 **[NUEVA_FUNCION]**: Integración en el menú 'Abrir con...' de Android: FlashGallery ahora aparece como opción para abrir archivos PDF, documentos de Office (Word, Excel, PowerPoint), texto y código desde apps externas como WhatsApp, Telegram, navegadores y gestores de archivos.
* 🟡 **[INTERFAZ]**: Apertura instantánea en visor integrado: cuando otra aplicación envía un PDF o documento, FlashGallery lo visualiza directamente en su visor nativo correspondiente (PdfViewerDialog, OfficeDocViewerDialog) mediante streaming seguro por ContentResolver y clonado a caché si es necesario.

---

## 🚀 Versión 1.18 (Código: 118) — 17/10/2026
### *Firma Oficial de Desarrollador FlashChip (Keystore Maestro) y adaptabilidad con fuentes grandes*

* 🟡 **[INTERFAZ]**: Adaptabilidad completa para fuentes grandes (accesibilidad): pestañas superiores convertidas en ScrollableTabRow y botones de modo ('Línea de tiempo' / 'De tus apps') con desplazamiento horizontal suave y protección contra salto de línea (maxLines = 1), eliminando cualquier corte de palabras como 'Document-os'.
* 🟣 **[SEGURIDAD]**: Certificado Maestro Oficial de Desarrollador (flashchip-master.jks): par de claves RSA 4096-bit con validez de 30 años (hasta 2056) emitido a nombre de WoRFReD / FlashChip para acumular reputación y confianza ante Google Play Protect.
* 📝 **[DOCUMENTACION]**: Guía Oficial de Firma para Futuros Proyectos (GUIA_FIRMA_FLASHCHIP.md): manual detallado con credenciales, huellas SHA-256/SHA-1 y ejemplos de configuración en Gradle (Kotlin DSL y Groovy) para reutilizar esta misma firma en cualquier nueva app.
* 🛠️ **[HERRAMIENTA]**: Asistente automatizado de verificación (verificar_play_protect.bat): script para calcular hashes de la APK, copiar la huella SHA-256 al portapapeles y enlazar al formulario oficial de lista blanca de Google Play Protect.

---

## 🚀 Versión 1.17 (Código: 117) — 17/10/2026
### *Zoom por pellizco ultrafluido, desplazamiento libre (pan) y presentacion oficial en GitHub*

* 🔴 **[CORRECCION]**: Reescritura completa del motor tactil de zoom por pellizco (pinch-to-zoom): eliminado el corte abrupto y los tirones en el visor; ahora la ampliacion sigue fielmente los dedos en tiempo real de 1x a 5x con precision continua.
* 🔵 **[OPTIMIZACION]**: Desplazamiento panoramico (pan) suave con limites de contorno para fotos ampliadas: navegar por detalles de la imagen con 1 o 2 dedos sin trabas ni perdida de foco tactil.
* 🔵 **[OPTIMIZACION]**: Doble toque inteligente: amplia de forma centrada hacia el punto pulsado a 2.5x y vuelve con fluidez a 1x con el siguiente doble toque.
* 🔵 **[OPTIMIZACION]**: Mapeo de coordenadas OCR en fotos ampliadas: ahora el long-press para copiar claves y contraseñas (ej. routers) funciona con exactitud milimetrica incluso si el usuario ha hecho zoom previo sobre la pegatina.
* 📝 **[DOCUMENTACION]**: Presentacion oficial bilingüe en GitHub (README.md): documentacion estructurada profesional con insignias dinamicas, arquitectura tecnica, caracteristicas clave, historial de versiones y enlaces oficiales del canal FlashChip de WoRFReD.

---

## 🚀 Versión 1.16 (Código: 116) — 17/10/2026
### *Reconocimiento de texto OCR / Live Text con pulsación prolongada en fotos*

* 🟢 **[NUEVA_FUNCION]**: Reconocimiento de texto en fotos por pulsación prolongada: al dejar pulsado sobre cualquier texto o caracteres dentro de una foto (pegatinas de router WiFi, números de serie, contraseñas, recibos, etc.), la app localiza con precisión el texto bajo el dedo y abre el panel de selección instantáneo.
* 🟢 **[NUEVA_FUNCION]**: Motor OCR Google ML Kit Text Recognition integrado (100% en dispositivo y sin conexión a internet): detecta bloques de texto, líneas y palabras completas en milisegundos con alta fidelidad.
* 🟡 **[INTERFAZ]**: Panel interactivo de selección de texto (OcrSelectionSheet): muestra la clave o texto pulsado en tipografía monoespaciada para evitar confusión entre caracteres (0/O, 1/l/I), con botón 'Copiar clave' en un toque y botón 'Compartir'.
* 🟡 **[INTERFAZ]**: Desglose completo de líneas detectadas: lista individualizada de todos los textos encontrados en la pegatina con botón de copia rápida por cada línea y botones globales para copiar o compartir todo.
* 🟡 **[INTERFAZ]**: Botón de escaneo OCR directo en la barra superior del visor de fotos para escanear a petición sin necesidad de pulsar sobre la pantalla.

---

## 🚀 Versión 1.15 (Código: 115) — 17/10/2026
### *Visor nativo de Office (Word, Excel, PowerPoint) y soporte universal de documentos*

* 🟢 **[NUEVA_FUNCION]**: Visor nativo de documentos de Office (OfficeDocViewerDialog): permite abrir y visualizar directamente sin necesidad de apps externas instaladas archivos de Microsoft Word (.docx, .doc, .odt, .rtf), Excel (.xlsx, .ods, .csv, .xls) y PowerPoint (.pptx, .odp, .ppt).
* 🟢 **[NUEVA_FUNCION]**: Motor de análisis ligero de Office (OfficeDocumentParser): descomprime y analiza estructuras XML nativas (WordprocessingML, SpreadsheetML, PresentationML) extrayendo títulos, párrafos formateados, viñetas, tablas y diapositivas de manera instantánea y con 0 MB de librerías pesadas.
* 🟡 **[INTERFAZ]**: Visualizador de Word con fichas estadísticas (párrafos, palabras aprox., tablas), párrafos con tipografía de lectura clara y tablas integradas.
* 🟡 **[INTERFAZ]**: Visualizador de Excel con selector de hojas (pestañas), cuadrícula bidireccional 2D interactiva con cabeceras fijadas y celdas alternadas.
* 🟡 **[INTERFAZ]**: Visualizador de PowerPoint con tarjetas de diapositivas numeradas, títulos destacados y lista de puntos clave.
* 🟡 **[INTERFAZ]**: Buscador interno de texto con resaltado de coincidencias, botón de copia completa al portapapeles y botón para compartir el documento.
* 🔴 **[CORRECCION]**: Apertura externa robusta en DocRepository: mapa completo de tipos MIME oficiales para toda la suite de Office, delegación de permisos de lectura por FileProvider y notificación guiada si el dispositivo no tiene ninguna app de Office instalada.

---

## 🚀 Versión 1.14 (Código: 114) — 17/10/2026
### *Botón Compartir en selección múltiple, solución recorte de miniaturas y visor de código blindado*

* 🟢 **[NUEVA_FUNCION]**: Botón 'Compartir' en la barra de selección múltiple: permite enviar una o múltiples fotos y vídeos a cualquier aplicación (WhatsApp, Telegram, Drive, etc.) mediante Android ShareSheet con permisos de lectura garantizados.
* 🔴 **[CORRECCION]**: Corrección del recorte de la fila superior: ajuste del margen superior (contentPadding = 76.dp) para que la primera fila de fotos y los iconos circulares de selección ('v') queden completamente visibles y holgados bajo la barra del mes fijada.
* 🔴 **[CORRECCION]**: Solución del cierre inesperado (crash) al abrir dumps de XML y archivos extensos en el visor de código: nuevo preprocesador automático (processRawLines) que formatea y desglosa archivos minificados sin saltos de línea (como picker_dump.xml) en bloques seguros de 400 caracteres.
* 🔵 **[OPTIMIZACION]**: Reestructuración del scroll horizontal en el visor Notepad++ a nivel de contenedor padre, evitando colisiones de ScrollState en Jetpack Compose y activando ajuste de línea (Word Wrap) por defecto para lectura cómoda en móviles.

---

## 🚀 Versión 1.12 (Código: 112) — 17/10/2026
### *Publicación de Release OTA en GitHub y prueba de actualización remota*

* 🟢 **[NUEVA_FUNCION]**: Versión 1.12 publicada oficialmente como release en GitHub con el binario FlashGallery.apk adjunto.
* 🔵 **[OPTIMIZACION]**: Verificación del flujo OTA en vivo: detección de salto de versión desde v1.11 a v1.12, descarga progresiva e instalación desatendida.
* 🔹 **[HERRAMIENTAS]**: Automatización del push a origin main y creación remota de release con GitHub CLI.

---

## 🚀 Versión 1.11 (Código: 111) — 17/10/2026
### *Actualizaciones OTA desde GitHub, diálogo de actualización y publicación automatizada*

* 🟢 **[NUEVA_FUNCION]**: Sistema de comprobación de actualizaciones OTA desde GitHub Releases: comprueba automáticamente o a petición si existe una nueva versión comparando Semantic Versioning (vX.YY).
* 🟡 **[INTERFAZ]**: Diálogo de actualización OTA (OtaUpdateDialog) estilo Cyberpunk: muestra notas de versión (changelog), tamaño, botón de descarga con barra de progreso porcentual y botón para instalar.
* 🟢 **[NUEVA_FUNCION]**: Descarga interna de APK en caché segura e instalación directa mediante FileProvider e Intent de PackageInstaller de Android.
* 🟡 **[INTERFAZ]**: Botón interactivo en el diálogo "Acerca de" (AboutDialog) para buscar actualizaciones OTA manualmente con respuesta instantánea.
* 🔹 **[HERRAMIENTAS]**: Script autónomo actualizar_y_probar.bat mejorado: integra GitHub CLI (gh) y MinGit portables en SDK_Gradle, autodetecta sesión de GitHub o inicia login guiado, gestiona repositorio remoto origin, compila, prueba por ADB y publica releases oficiales en GitHub con FlashGallery.apk adjunto.

---

## 🚀 Versión 1.10 (Código: 110) — 17/10/2026
### *Visor nativo de PDF y Visor de código estilo Notepad++ con coloreado de sintaxis*

* 🟢 **[NUEVA_FUNCION]**: Visor nativo de documentos PDF (PdfViewerDialog): renderizado de páginas de alta resolución mediante PdfRenderer de Android, paginador con slider, gestos táctiles de zoom y desplazamiento (pan) y botón para compartir o abrir en visor externo.
* 🟢 **[NUEVA_FUNCION]**: Visor de código y texto estilo Notepad++ (CodeTextViewerDialog): visualizador profesional para archivos XML, JSON, HTML, TXT, LOG, INI, scripts y código fuente.
* 🟡 **[INTERFAZ]**: Motor de coloreado de sintaxis (SyntaxHighlighter): etiquetas en FlashCyan, atributos en celeste/ámbar, cadenas en coral/naranja, comentarios de bloque y línea en verde Notepad++ (itálica), y palabras clave en morado.
* 🟡 **[INTERFAZ]**: Columna lateral de números de línea (Gutter) estilo Notepad++ sincronizada con el scroll vertical de alto rendimiento (LazyColumn).
* 🟢 **[NUEVA_FUNCION]**: Buscador integrado en archivos de texto: localizador de palabras clave con contador de coincidencias y botones arriba/abajo para navegar entre líneas coincidentes.
* 🟢 **[NUEVA_FUNCION]**: Conmutador de ajuste de línea (Word Wrap) para alternar entre desplazamiento horizontal sin cortes y salto de línea automático.
* 🟢 **[NUEVA_FUNCION]**: Botón de copia rápida de todo el documento al portapapeles y botón para abrir en editor externo si se desea modificar.

---

## 🚀 Versión 1.09 (Código: 109) — 17/10/2026
### *Eliminación de cabecera duplicada y separador de meses minimalista*

* 🔴 **[CORRECCION]**: Eliminado el encabezado duplicado al inicio: el primer mes ya no dibuja una segunda barra debajo de la barra sticky fija.
* 🟡 **[INTERFAZ]**: Rediseño del separador de meses dentro del scroll a formato minimalista ("solo letras"): línea sutil, tipografía limpia ("Septiembre 2026 • 24 elem.") y enlace discreto de texto para seleccionar.

---

## 🚀 Versión 1.08 (Código: 108) — 17/10/2026
### *Selector interactivo de salto rápido a Mes y Año*

* 🟢 **[NUEVA_FUNCION]**: Al pulsar la cabecera fija del mes en la parte superior ("Octubre 2026 ▾"), se abre un diálogo interactivo para saltar de fecha en el tiempo.
* 🟡 **[INTERFAZ]**: Selector de años mediante chips horizontales filtrando solo los años que contienen fotos reales.
* 🟡 **[INTERFAZ]**: Cuadrícula de meses disponibles para el año seleccionado, indicando cantidad de fotos/vídeos y resaltando el mes actual.
* 🟢 **[NUEVA_FUNCION]**: Botón de acceso rápido "Ir al principio (Más recientes)" y salto instantáneo (scrollToItem) sin tirones.

---

## 🚀 Versión 1.07 (Código: 107) — 16/10/2026
### *Modo Selección Múltiple y ajuste de sensibilidad de deslizamiento*

* 🟢 **[NUEVA_FUNCION]**: Modo selección por pulsación larga (long press) en cualquier foto o vídeo para seleccionar varios elementos.
* 🟢 **[NUEVA_FUNCION]**: Barra contextual de acciones: contador de seleccionados, seleccionar todo, mover a otra carpeta y eliminación masiva con confirmación.
* 🔵 **[OPTIMIZACION]**: Ajuste de sensibilidad del visor táctil (snapPositionalThreshold = 0.15f) para cambiar de foto con un deslizamiento de dedo más corto y suave.

---

## 🚀 Versión 1.06 (Código: 106) — 16/10/2026
### *Desbloqueo de gestos en visor, hoja EXIF detallada y gestión de botón Atrás*

* 🔴 **[CORRECCION]**: Desbloqueo de gestos de deslizamiento en HorizontalPager: desactivación de detectTransformGestures a zoom 1x para que no intercepte el cambio de foto.
* 🔴 **[CORRECCION]**: Corrección del botón 'i' de información EXIF: integrada directamente en el visor para evitar conflictos de ventana (WindowManager) en Android.
* 🟢 **[NUEVA_FUNCION]**: Ficha técnica detallada EXIF: muestra resolución, cámara, apertura (f-number), velocidad de obturación, ISO, distancia focal, fecha exacta, tamaño y GPS.
* 🔴 **[CORRECCION]**: Control total del botón físico/gestual Atrás del móvil (BackHandler) para retroceder vistas o cerrar visores sin minimizar la aplicación.

---

## 🚀 Versión 1.05 (Código: 105) — 16/10/2026
### *Navegación gestual táctil pura y limpieza de interfaz*

* 🟡 **[INTERFAZ]**: Eliminación de flechas físicas en los bordes del visor a pantalla completa.
* 🟢 **[NUEVA_FUNCION]**: Control 100% gestual deslizando el dedo (swipe) hacia la izquierda o derecha para pasar de foto.
* 🟡 **[INTERFAZ]**: Eliminación del pie de página redundante ("by WoRFReD"), centralizándolo con estilo en la barra superior.

---

## 🚀 Versión 1.04 (Código: 104) — 16/10/2026
### *Visor a pantalla completa con paginador horizontal*

* 🟢 **[NUEVA_FUNCION]**: Implementación de visor a pantalla completa (MediaViewerDialog) para fotos y reproducción de vídeos.
* 🟢 **[NUEVA_FUNCION]**: Integración de HorizontalPager de Jetpack Compose para navegar entre fotos anteriores y siguientes en el visor.
* 🟡 **[INTERFAZ]**: Cabecera fijada (Sticky Header) de mes en la línea de tiempo que permanece anclada al hacer scroll.

---

## 🚀 Versión 1.03 (Código: 103) — 16/10/2026
### *Carga instantánea de fotos y línea de tiempo por meses*

* 🔵 **[OPTIMIZACION]**: Sistema de carga progresiva ultrarrápida: el primer lote de miniaturas (60 fotos) se muestra en menos de 30 milisegundos sin congelar la app.
* 🟡 **[INTERFAZ]**: Sustitución de la burbuja flotante de fecha por separadores temporales fijos por meses (ej. "Octubre 2026", "Septiembre 2026").
* 🔵 **[OPTIMIZACION]**: Eliminación de indicadores circulares de carga bloqueantes en la pestaña de fotos para experiencia instantánea.

---

## 🚀 Versión 1.02 (Código: 102) — 15/10/2026
### *Permisos de almacenamiento en Android 11+ para APKs y Documentos*

* 🔴 **[CORRECCION]**: Solución al problema de pestañas de APKs y Documentos vacías por restricciones de permisos en Android 11 o superior.
* 🟢 **[NUEVA_FUNCION]**: Añadida solicitud de permiso especial MANAGE_EXTERNAL_STORAGE (Acceso a todos los archivos) con comprobación dinámica y redirección automática a Ajustes si no está concedido.
* 🟢 **[NUEVA_FUNCION]**: Escaneo recursivo optimizado de almacenamiento para instalación de APKs y lectura de PDF, Word, Excel, etc.

---

## 🚀 Versión 1.01 (Código: 101) — 14/10/2026
### *Creación inicial, compilación y estructura base*

* 🔵 **[OPTIMIZACION]**: Migración de ruta de disco Z: a C: para acelerar los tiempos de compilación de Gradle.
* 🔴 **[CORRECCION]**: Corrección de errores Kotlin/Compose: MutableSetOf, Box import, derivedStateFlow y sobrecarga de funciones Text.
* 🟢 **[NUEVA_FUNCION]**: Implementación de 3 pestañas principales: Fotos y Vídeos, APKs del dispositivo y Documentos.
* 🟡 **[INTERFAZ]**: Tema Cyberpunk oscuro (BgDark, SurfaceDark, FlashCyan, FlashAmber, TextPrimary).
* 🟢 **[NUEVA_FUNCION]**: Identificación y agrupación de fotos por aplicación de origen (WhatsApp, Cámara, Telegram, etc.).

---

