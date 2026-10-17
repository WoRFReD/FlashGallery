@echo off
setlocal EnableDelayedExpansion

echo ====================================================
echo   VERIFICADOR GOOGLE PLAY PROTECT - FLASHCHIP
echo   by WoRFReD - Canal: FlashChip
echo ====================================================
echo.

set "CERT_SHA256=CB:F2:50:E0:92:11:E1:8C:A8:68:44:41:56:4B:C3:3A:19:BA:B0:DE:14:1F:87:B1:E0:99:93:A5:7C:56:7B:1D"
set "PACKAGE=com.flashchip.flashgallery"
set "APK_URL=https://github.com/WoRFReD/FlashGallery/releases/latest/download/FlashGallery.apk"

echo [1/3] Datos de identidad del Desarrollador:
echo ----------------------------------------------------
echo Titular:        WoRFReD (Canal FlashChip)
echo Paquete:        !PACKAGE!
echo Enlace APK:     !APK_URL!
echo Huella SHA-256: !CERT_SHA256!
echo ----------------------------------------------------
echo.

if exist "FlashGallery.apk" (
    echo [2/3] Calculando Hash SHA-256 de FlashGallery.apk...
    for /f "skip=1 tokens=* delims=" %%A in ('certutil -hashfile FlashGallery.apk SHA256 ^| findstr /v "CertUtil"') do (
        set "APK_HASH=%%A"
        set "APK_HASH=!APK_HASH: =!"
    )
    echo Hash APK: !APK_HASH!
    echo.
) else (
    echo [2/3] FlashGallery.apk no encontrada. (Compila con actualizar_y_probar.bat primero).
    echo.
)

echo [3/3] Copiando la huella digital SHA-256 al portapapeles...
echo !CERT_SHA256!| clip
echo [OK] Huella copiada al portapapeles.
echo.

echo ====================================================
echo INSTRUCCIONES PARA GOOGLE PLAY PROTECT:
echo 1. Se abrira el formulario oficial de apelaciones de Google.
echo 2. Pega la huella SHA-256 (ya la tienes en el portapapeles).
echo 3. Introduce el enlace de descarga de la APK:
echo    !APK_URL!
echo 4. En comentarios indica que es una galeria de codigo abierto
echo    sin malware y 100%% offline.
echo ====================================================
echo.
pause

start https://support.google.com/googleplay/android-developer/contact/protectappeals
exit /b 0
