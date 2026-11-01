# 📐 REGLAS MAESTRAS DE DESARROLLO DE APKS (FLASHCHIP)
> **Autor:** WoRFReD • **Canal:** FlashChip (https://youtube.com/@flashchiped)  
> **Documento de especificación obligatorio para Antigravity / Asistentes IA de programación.**

Cada vez que se solicite crear, modificar o mantener una aplicación Android (APK) para este entorno, **Antigravity DEBE seguir rigurosamente las siguientes directivas sin excepción**.

---

## 1. 🗂️ UBICACIÓN Y ESTRUCTURA DE TRABAJO

1. **Desarrollo en disco local `C:\`:**  
   Todo proyecto debe crearse y compilarse en la ruta local:  
   `C:\APPs_Atigravity\<Nombre_Proyecto>`  
   *(Esto es obligatorio para que Gradle compile en pocos segundos y no se ralentice por la latencia de disco de red/nube).*
2. **Copia de seguridad limpia en almacenamiento `Z:\` (Solo fuentes y APK final):**  
   Al respaldar en `Z:\DIGIstorage\APPs_Atigravity\<Nombre_Proyecto>`, **NUNCA** se deben transferir los gigabytes de archivos temporales de compilación.  
   * **SÍ se copia:**
     * Todo el código fuente (`app/src/`: Kotlin, XML, recursos, manifests).
     * Archivos de configuración (`build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `proguard-rules.pro`).
     * Gradle Wrapper (`gradle/`, `gradlew`, `gradlew.bat`).
     * Documentación y scripts (`INFORME_VERSIONES.md`, `README.md`, `actualizar_y_probar.bat`, `verificar_play_protect.bat`).
     * Certificado de firma (`flashchip-master.jks`).
     * **El APK oficial compilado final:** `<Nombre_Proyecto>.apk`.
   * **NUNCA se copia (exclusión obligatoria):**
     * ❌ `.gradle/` (cachés y daemons locales).
     * ❌ `build/` y `app/build/` (archivos intermedios, dexBuilder, temporales).
     * ❌ `.idea/` y `*.iml` (archivos internos de Android Studio).
     * ❌ `local.properties` (rutas locales de SDK exclusivas de la máquina local).
     * ❌ `.cxx/` y `captures/`.
   * **Comando Robocopy optimizado para DIGI Storage / RaiDrive (WebDAV):**
     * `/FFT` -> Tolerancia de fecha (2 segundos). Crucial para WebDAV; si el tamaño es igual, omite la subida innecesaria.
     * `/XO` -> Excluye archivos más antiguos o idénticos en destino.
     * `/MT:16` -> Transfiere 16 archivos en paralelo para máxima velocidad en red.
     * `/COPY:D` -> Solo datos (evita errores con atributos ACL de Windows no soportados por WebDAV).
     * `/NODCOPY` -> Ignora atributos de carpetas.
     * `/R:0 /W:0` -> Sin reintentos infinitos si un archivo está en uso.
     * `/NP` -> Desactiva el porcentaje individual en consola para máxima fluidez.
     * `/XD .gradle build app\build .idea .cxx captures` -> Excluye directorios temporales gigantes.
     * `/XF local.properties *.iml` -> Excluye configuraciones locales de máquina.
     ```cmd
     robocopy "C:\APPs_Atigravity\<Nombre_Proyecto>" "Z:\DIGIstorage\APPs_Atigravity\<Nombre_Proyecto>" /E /COPY:D /NODCOPY /MT:16 /FFT /XO /R:0 /W:0 /NP /XD .gradle build app\build .idea .cxx captures /XF local.properties *.iml
     ```
3. **Herramientas Compartidas (`SDK_Gradle`):**  
   Los binarios portables compartidos (GitHub CLI `.gh_bin`, MinGit, JDK portable, Gradle Wrappers) residen en:  
   `C:\APPs_Atigravity\SDK_Gradle` y `Z:\DIGIstorage\APPs_Atigravity\SDK_Gradle`.

---

## 2. 🔢 SISTEMA DE VERSIONES INCREMENTAL (REGLA 1.00+)

1. **Versión Inicial:**  
   Todo proyecto nuevo comienza **SIEMPRE** en la versión **1.00**:  
   * `versionCode = 100`  
   * `versionName = "1.00"`
2. **Incrementos Obligatorios por Modificación:**  
   Cada vez que se añada una función, pantalla, ajuste o corrección de error, se debe incrementar de **0.01 en 0.01**:  
   * Versión 2: `versionCode = 101`, `versionName = "1.01"`  
   * Versión 3: `versionCode = 102`, `versionName = "1.02"`  
   * Versión 4: `versionCode = 103`, `versionName = "1.03"`  
   *(y así sucesivamente).*
3. **Registro Formal del Historial:**  
   En cada incremento se debe actualizar:
   * El archivo [`INFORME_VERSIONES.md`](#) del proyecto con la nueva versión, código y lista clasificada de cambios.
   * La tabla del Changelog en el [`README.md`](#).
   * La cabecera de los archivos Kotlin clave (`// Version X.XX - <Nombre_App>`).

---

## 3. 🔐 FIRMA OFICIAL RELEASE (KEYSTORE MAESTRO FLASHCHIP)

1. **Nunca entregar versiones DEBUG:**  
   Todas las compilaciones deben ser en modo **Release Oficial Firmada** (`gradlew assembleRelease`).
2. **Almacén de Claves Maestro:**  
   Se utiliza el certificado maestro unificado de FlashChip:
   * Archivo: `flashchip-master.jks` (ubicado en la raíz de cada proyecto o en `Z:\DIGIstorage\APPs_Atigravity\Reglas_APKs\flashchip-master.jks`).
   * Alias: `flashchip`
   * Clave del almacén (storePassword): `flashchippass`
   * Clave del alias (keyPassword): `flashchippass`
   * Huella SHA-256 oficial: `CB:F2:50:E0:92:11:E1:8C:A8:68:44:41:56:4B:C3:3A:19:BA:B0:DE:14:1F:87:B1:E0:99:93:A5:7C:56:7B:1D`
3. **Firma Dual V1 + V2 (Obligatoria para Huawei / EMUI):**  
   En `app/build.gradle.kts` se debe configurar siempre:
   ```kotlin
   signingConfigs {
       create("release") {
           storeFile = rootProject.file("flashchip-master.jks")
           storePassword = "flashchippass"
           keyAlias = "flashchip"
           keyPassword = "flashchippass"
           enableV1Signing = true
           enableV2Signing = true // OBLIGATORIO: Huawei rechaza APKs sin bloque binario V2
       }
   }
   ```

---

## 4. 📱 REGLAS DE COMPATIBILIDAD (HUAWEI VS SAMSUNG & APKS GRANDES)

1. **Firma APK V2 en Huawei:**  
   Huawei EMUI y HarmonyOS exigen estrictamente el esquema APK V2. Si una app carece de firma V2, fallará con `INSTALL_PARSE_FAILED_NO_CERTIFICATES`.
2. **Ratio 3x de Almacenamiento Interno para `dex2oat`:**  
   Para procesar APKs grandes (desde 100 MB hasta 1 GB+), Android necesita en `/data` al menos **entre 2.5 y 3 veces el tamaño del APK libre**. Justo al final de la instalación, el compilador nativo ART (`dex2oat`) descomprime recursos y compila archivos `.odex`/`.vdex`. Si falta espacio, aborta con `INSTALL_FAILED_INSUFFICIENT_STORAGE`.
3. **Compatibilidad de Arquitecturas CPU (ABIs 32/64 bits):**  
   * Muchas tablets Huawei (ej. MediaPad T3, T5, MatePad T8/T10 con Kirin) ejecutan Android en **32 bits (`armeabi-v7a`)**.  
   * Si el APK solo incluye librerías de **64 bits (`arm64-v8a`)**, la instalación fallará con `INSTALL_FAILED_NO_MATCHING_ABIS`. Siempre que haya binarios nativos `.so`, incluir soporte dual `armeabi-v7a` y `arm64-v8a`.
4. **Conflictos de Firma Previa:**  
   Si una app ya existe instalada con una firma distinta, Android aborta con `INSTALL_FAILED_UPDATE_INCOMPATIBLE`. Debe desinstalarse la versión previa antes de instalar.

---

## 5. 🛠️ STACK TECNOLÓGICO Y ESTILO VISUAL

1. **Lenguaje:** Kotlin 1.9+ con Coroutines y Flow.
2. **Framework UI:** Jetpack Compose + Material 3.
3. **Estética FlashChip (Cyberpunk / Dark Theme):**  
   * Fondo: `#0A0E17` (BgDark)
   * Superficie de Tarjetas: `#131B2A` (SurfaceDark)
   * Borde / Contorno: `#24334B` (CardBorder)
   * Acentos de Color:
     * Cyan Neón: `#00E5FF` (FlashCyan - Color primario)
     * Ámbar / Oro: `#FFB300` (FlashAmber - Alertas / Secundario)
     * Verde Neón: `#00E676` (FlashGreen - Éxito / Operación OK)
     * Rojo Neón: `#FF5252` (FlashRed - Error / Advertencia crítica)
     * Texto Principal: `#F0F6FC`
     * Texto Secundario: `#8B949E`
4. **SDK Target:**  
   * `minSdk = 26` (Android 8.0 Oreo - compatibilidad con tablets antiguas y modernas).
   * `compileSdk = 34`, `targetSdk = 34`.

---

## 6. 🔄 ACTUALIZACIONES OTA AUTOMÁTICAS DESDE GITHUB

Toda aplicación debe incluir un módulo `OtaRepository` que:
1. Consulte la API pública de GitHub:  
   `https://api.github.com/repos/WoRFReD/<Nombre_Proyecto>/releases/latest`
2. Compare semánticamente el tag remoto (ej. `v1.02`) con la versión instalada (`1.01`).
3. Si existe una versión superior, muestre un diálogo integrado con los cambios y descargue la nueva APK con indicador visual de progreso (`%` y `MB`).
4. Solicite la instalación segura mediante `FileProvider`.
5. Incluya un botón de "Buscar actualizaciones" en el diálogo "Acerca de" de la app.

---

## 7. 🤖 ARCHIVOS Y SCRIPTS OBLIGATORIOS EN CADA PROYECTO

Cada nuevo proyecto debe contar indispensablemente con estos archivos en su raíz (cuyas plantillas maestras residen en `Z:\DIGIstorage\APPs_Atigravity\Reglas_APKs`):

1. **`actualizar_y_probar.bat`:**  
   Script automatizado que:
   * Auto-detecta JDK 17, Android SDK 34, Git y GitHub CLI (`gh`).
   * Extrae la versión actual directamente de `app/build.gradle.kts`.
   * Compila `assembleRelease` y copia el resultado como `<Nombre_Proyecto>.apk`.
   * Detecta dispositivos conectados por ADB e instala/lanza la app automáticamente.
   * Hace commit y push de los cambios en Git (`main`).
   * Publica la Release oficial en GitHub adjuntando el archivo `.apk` mediante `gh release create`.
2. **`INFORME_VERSIONES.md`:**  
   Historial formal de versiones en Markdown con clasificación de cambios por iconos y etiquetas:
   * 🟢 **`[NUEVA_FUNCION]`**
   * 🟡 **`[INTERFAZ]`**
   * 🔴 **`[CORRECCION]`**
   * 🔵 **`[OPTIMIZACION]`**
   * 🟣 **`[SEGURIDAD]`**
3. **`verificar_play_protect.bat`:**  
   Herramienta de utilidad para calcular el hash SHA-256 del APK y copiar la huella digital al portapapeles para apelaciones de falsos positivos en Play Protect.
4. **`README.md`:**  
   Documento de presentación bilingüe (Español/Inglés) con insignias de GitHub Release, Android version, Jetpack Compose y YouTube FlashChip, acompañado de la tabla completa del Changelog.
5. **`flashchip-master.jks`:**  
   Certificado de firma maestro en la raíz.
