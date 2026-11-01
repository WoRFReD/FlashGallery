@echo off
setlocal EnableDelayedExpansion
chcp 65001 > nul
title Copia Rapida a DIGI Storage (Z:) - FlashChip

:: Extraer nombre de la carpeta actual como nombre del proyecto
for %%I in ("%~dp0.") do set "PROJECT_NAME=%%~nxI"

echo ===================================================
echo   Iniciando copia rápida a Z:\DIGIstorage\APPs_Atigravity\!PROJECT_NAME!
echo   by WoRFReD - Canal: FlashChip
echo ===================================================
echo.

set "ORIGEN=%~dp0."
set "DESTINO=Z:\DIGIstorage\APPs_Atigravity\!PROJECT_NAME!"

:: 1. Verificar que RaiDrive ha montado la unidad Z:
if not exist "Z:" (
    echo [ERROR] La unidad Z: no está disponible o RaiDrive no está ejecutándose.
    pause
    exit /b 1
)

:: 2. Pre-crear la estructura en RaiDrive si no existe
if not exist "%DESTINO%" (
    echo Creando directorio de destino en RaiDrive...
    mkdir "%DESTINO%" 2>nul
)

:: 3. Robocopy optimizado para RaiDrive / WebDAV con exclusiones limpias
echo Sincronizando fuentes y APK oficial...
robocopy "%ORIGEN%" "%DESTINO%" /E /COPY:D /NODCOPY /MT:16 /FFT /XO /R:0 /W:0 /NP /XD .gradle build app\build .idea .cxx captures /XF local.properties *.iml

echo.
echo ===================================================
echo   Proceso finalizado. Copia limpia en Z:.
echo ===================================================
pause
