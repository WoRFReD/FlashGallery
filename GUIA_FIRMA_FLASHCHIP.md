# 🔐 GUÍA MAESTRA DE FIRMA OFICIAL - FLASHCHIP by WoRFReD

Este documento contiene la información oficial, credenciales y las instrucciones paso a paso para utilizar tu **Firma Criptográfica Oficial de Desarrollador** en **FlashGallery** y en **cualquier nuevo proyecto Android** que crees en el futuro.

---

## 📌 ¿Por qué es tan importante este archivo?

En Android, el archivo `.jks` (Java KeyStore) es tu **DNI digital como desarrollador**.
Cuando firmas una aplicación con esta clave:
1. **Identidad única:** Google Play Protect, Android y los dispositivos reconocen que todas las APKs provienen del mismo autor (**WoRFReD - FlashChip**).
2. **Reputación acumulada:** Cada vez que usuarios instalan tus aplicaciones firmadas con esta clave, Google Play Protect acumula confianza y reduce o elimina las advertencias de seguridad en futuros lanzamientos y proyectos.
3. **Actualizaciones seguras:** Solo quien posea este archivo puede actualizar la app en los teléfonos de los usuarios sin obligarles a desinstalar.

> [!CAUTION]
> **Haz una copia de seguridad segura de `flashchip-master.jks`:**
> Guarda este archivo en una memoria USB, en Google Drive o en tu almacenamiento seguro. Si lo pierdes, no podrás emitir actualizaciones de tus aplicaciones bajo la misma firma.

---

## 🔑 Credenciales Oficiales de la Clave

| Parámetro | Valor Oficial |
| :--- | :--- |
| **Archivo Keystore** | `flashchip-master.jks` |
| **Alias de la Clave** | `flashchip` |
| **Contraseña del Almacén (Store Password)** | `flashchippass` |
| **Contraseña de la Clave (Key Password)** | `flashchippass` |
| **Algoritmo de cifrado** | `RSA 4096 bits` (Grado militar de alta seguridad) |
| **Algoritmo de firma** | `SHA384withRSA` / `SHA-256` |
| **Validez** | **30 años** (Válido hasta Octubre de **2056**) |
| **Titular (DName)** | `CN=WoRFReD, OU=FlashChip, O=FlashChip, L=Madrid, ST=Madrid, C=ES` |

### 🛡️ Huellas Digitales del Certificado (Fingerprints)
* **SHA-256:**
  ```text
  CB:F2:50:E0:92:11:E1:8C:A8:68:44:41:56:4B:C3:3A:19:BA:B0:DE:14:1F:87:B1:E0:99:93:A5:7C:56:7B:1D
  ```
* **SHA-1:**
  ```text
  59:77:70:BB:A2:94:AC:64:76:9C:6A:A9:45:74:63:B9:5A:F7:A5:E1
  ```

---

## 🚀 Cómo usar esta clave en un NUEVO proyecto de Android

Cuando crees una nueva aplicación (o un nuevo proyecto en Android Studio o Antigravity):

### Paso 1: Copiar el archivo
Copia el archivo `flashchip-master.jks` a la carpeta raíz de tu nuevo proyecto.

### Paso 2: Configurar Gradle
Abre el archivo `app/build.gradle.kts` (o `app/build.gradle`) y añade tu configuración de firma:

#### Opción A: Si tu proyecto usa Kotlin DSL (`build.gradle.kts`):
```kotlin
android {
    ...
    signingConfigs {
        create("release") {
            storeFile = rootProject.file("flashchip-master.jks")
            storePassword = "flashchippass"
            keyAlias = "flashchip"
            keyPassword = "flashchippass"
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

#### Opción B: Si tu proyecto usa Groovy (`build.gradle`):
```groovy
android {
    ...
    signingConfigs {
        release {
            storeFile file("../flashchip-master.jks")
            storePassword "flashchippass"
            keyAlias "flashchip"
            keyPassword "flashchippass"
            v1SigningEnabled true
            v2SigningEnabled true
        }
    }

    buildTypes {
        release {
            signingConfig signingConfigs.release
        }
    }
}
```

### Paso 3: Firmar una APK manualmente por consola (Opcional)
Si compilas una APK sin firmar y quieres firmarla directamente desde la terminal con `apksigner`:
```bat
apksigner sign --ks flashchip-master.jks --ks-key-alias flashchip --ks-pass pass:flashchippass --key-pass pass:flashchippass MiNuevaApp.apk
```

---

## 🛡️ Cómo verificar la app en Google Play Protect para que sea reconocida

Para eliminar el aviso *"Esta app puede ser dañina"* en los teléfonos de tus seguidores y familiares, Google dispone de un formulario oficial de apelación directa para desarrolladores:

### 1. Formulario oficial de Google:
👉 **[Formulario de Apelaciones de Google Play Protect](https://support.google.com/googleplay/android-developer/contact/protectappeals)**

### 2. Datos a rellenar:
* **Nombre del desarrollador:** `WoRFReD (FlashChip)`
* **Canal / Web:** `https://youtube.com/@flashchiped`
* **Nombre del paquete (Package Name):** `com.flashchip.flashgallery` (o el de tu nueva app)
* **URL de descarga directa de la APK:** 
  `https://github.com/WoRFReD/FlashGallery/releases/latest/download/FlashGallery.apk`
* **Huella SHA-256 del certificado:**
  `CB:F2:50:E0:92:11:E1:8C:A8:68:44:41:56:4B:C3:3A:19:BA:B0:DE:14:1F:87:B1:E0:99:93:A5:7C:56:7B:1D`
* **Comentarios / Justificación:**
  *"Esta aplicación es una galería multimedia y visor de documentos de código abierto, desarrollada para la comunidad del canal FlashChip. La app no contiene malware, no recopila datos de usuario y todas sus funciones (incluido el OCR) se ejecutan 100% de forma local y offline en el dispositivo. Solicitamos la verificación y adición a la lista blanca de Play Protect."*

En un plazo de 24 a 48 horas tras enviarlo, los servidores de Google actualizan la base de datos de Play Protect y el aviso desaparece para todos los teléfonos.
