@echo off
setlocal EnableDelayedExpansion

echo ====================================================
echo   COMPILANDO, INSTALANDO Y PUBLICANDO FLASHGALLERY
echo   by WoRFReD - Canal: FlashChip
echo ====================================================

:: ------------------------------------------------------
:: 0. UBICACION DE HERRAMIENTAS COMPARTIDAS (SDK_Gradle)
:: ------------------------------------------------------
pushd "%~dp0.."
set "SHARED_TOOLS=%CD%\SDK_Gradle"
popd
if not exist "%SHARED_TOOLS%" mkdir "%SHARED_TOOLS%"

:: ------------------------------------------------------
:: 1. COMPROBACION DE HERRAMIENTAS: GIT Y GITHUB CLI (gh)
:: ------------------------------------------------------
echo.
echo [INFO] Verificando herramientas de control de versiones...

:: Deteccion de Git
where git >nul 2>&1
if %errorlevel% neq 0 (
    if exist "%SHARED_TOOLS%\.git_bin\cmd\git.exe" set "PATH=%SHARED_TOOLS%\.git_bin\cmd;!PATH!"
    if exist "C:\Program Files\Git\cmd\git.exe" set "PATH=C:\Program Files\Git\cmd;!PATH!"
    if exist "C:\Program Files\Git\bin\git.exe" set "PATH=C:\Program Files\Git\bin;!PATH!"
    if exist "%LOCALAPPDATA%\Programs\Git\cmd\git.exe" set "PATH=%LOCALAPPDATA%\Programs\Git\cmd;!PATH!"
    where git >nul 2>&1
    if !errorlevel! neq 0 (
        echo [INFO] Descargando MinGit portable en SDK_Gradle...
        if not exist "%SHARED_TOOLS%\.git_bin" mkdir "%SHARED_TOOLS%\.git_bin"
        set "GIT_ZIP=%SHARED_TOOLS%\.git_bin\mingit.zip"
        set "GIT_URL=https://github.com/git-for-windows/git/releases/download/v2.44.0.windows.1/MinGit-2.44.0-64-bit.zip"
        curl -L -s -o "!GIT_ZIP!" "!GIT_URL!" || powershell -Command "$ProgressPreference = 'SilentlyContinue'; Invoke-WebRequest -Uri '!GIT_URL!' -OutFile '!GIT_ZIP!'"
        if exist "!GIT_ZIP!" (
            powershell -Command "Expand-Archive -Path '!GIT_ZIP!' -DestinationPath '%SHARED_TOOLS%\.git_bin' -Force"
            del /f /q "!GIT_ZIP!" >nul 2>&1
            if exist "%SHARED_TOOLS%\.git_bin\cmd\git.exe" set "PATH=%SHARED_TOOLS%\.git_bin\cmd;!PATH!"
        )
        where git >nul 2>&1
        if !errorlevel! neq 0 (
            echo.
            echo [ERROR] Git no esta instalado o no se encuentra en el PATH del sistema.
            echo [INFO] Puedes instalar Git rapidamente ejecutando:
            echo        winget install --id Git.Git -e --source winget
            echo.
            pause
            exit /b 1
        )
    )
)
echo [OK] Git localizado en el sistema.

:: Deteccion de GitHub CLI (gh)
where gh >nul 2>&1
if %errorlevel% neq 0 (
    if exist "%SHARED_TOOLS%\.gh_bin\bin\gh.exe" set "PATH=%SHARED_TOOLS%\.gh_bin\bin;!PATH!"
    if exist "C:\Program Files\GitHub CLI\gh.exe" set "PATH=C:\Program Files\GitHub CLI;!PATH!"
    if exist "%LOCALAPPDATA%\Programs\GitHub CLI\bin\gh.exe" set "PATH=%LOCALAPPDATA%\Programs\GitHub CLI\bin;!PATH!"
    where gh >nul 2>&1
    if !errorlevel! neq 0 (
        echo [INFO] Descargando GitHub CLI portable en SDK_Gradle...
        if not exist "%SHARED_TOOLS%\.gh_bin" mkdir "%SHARED_TOOLS%\.gh_bin"
        set "GH_ZIP=%SHARED_TOOLS%\.gh_bin\gh.zip"
        set "GH_URL=https://github.com/cli/cli/releases/download/v2.58.0/gh_2.58.0_windows_amd64.zip"
        curl -L -s -o "!GH_ZIP!" "!GH_URL!" || powershell -Command "$ProgressPreference = 'SilentlyContinue'; Invoke-WebRequest -Uri '!GH_URL!' -OutFile '!GH_ZIP!'"
        if exist "!GH_ZIP!" (
            powershell -Command "Expand-Archive -Path '!GH_ZIP!' -DestinationPath '%SHARED_TOOLS%\.gh_temp' -Force"
            if exist "%SHARED_TOOLS%\.gh_temp\gh_2.58.0_windows_amd64\bin\gh.exe" (
                if not exist "%SHARED_TOOLS%\.gh_bin\bin" mkdir "%SHARED_TOOLS%\.gh_bin\bin"
                copy /y "%SHARED_TOOLS%\.gh_temp\gh_2.58.0_windows_amd64\bin\gh.exe" "%SHARED_TOOLS%\.gh_bin\bin\gh.exe" >nul
                rmdir /s /q "%SHARED_TOOLS%\.gh_temp" >nul 2>&1
            )
            del /f /q "!GH_ZIP!" >nul 2>&1
            if exist "%SHARED_TOOLS%\.gh_bin\bin\gh.exe" set "PATH=%SHARED_TOOLS%\.gh_bin\bin;!PATH!"
        )
        where gh >nul 2>&1
        if !errorlevel! neq 0 (
            echo.
            echo [ERROR] GitHub CLI [gh] no esta instalado en este equipo.
            echo [INFO] Puedes instalarlo ejecutando en una consola:
            echo        winget install --id GitHub.cli
            echo.
            pause
            exit /b 1
        )
    )
)
echo [OK] GitHub CLI [gh] localizado en el sistema.

:: Comprobacion de sesion activa en GitHub CLI
gh auth status >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo ====================================================
    echo [GITHUB] No hay sesion iniciada en este equipo.
    echo [GITHUB] Iniciando login seguro en GitHub CLI...
    echo ====================================================
    echo Por favor, sigue las instrucciones en pantalla para autorizarte:
    echo.
    gh auth login
    gh auth status >nul 2>&1
    if !errorlevel! neq 0 (
        echo.
        echo [ERROR] No se pudo verificar la sesion de GitHub.
        pause
        exit /b 1
    )
    echo [OK] Sesion iniciada correctamente en GitHub.
) else (
    echo [OK] Sesion activa verificada en GitHub.
)

:: ------------------------------------------------------
:: 1. GESTION DE REPOSITORIO LOCAL Y REMOTO (.git / origin)
:: ------------------------------------------------------
echo.
echo [INFO] Verificando estado del repositorio Git...

if not exist ".git" (
    echo [INFO] No se encontro la carpeta .git. Inicializando repositorio local...
    git init
    git branch -M main
    git config user.name >nul 2>&1
    if !errorlevel! neq 0 (
        for /f "tokens=*" %%U in ('gh api user --jq .login 2^>nul') do set "GH_USER=%%U"
        if not defined GH_USER set "GH_USER=WoRFReD"
        git config user.name "!GH_USER!"
        git config user.email "!GH_USER!@users.noreply.github.com"
    )
    git add .
    git commit -m "Initial commit - FlashGallery"
    echo [OK] Repositorio Git local inicializado con la rama principal 'main'.
)

git remote get-url origin >nul 2>&1
if %errorlevel% neq 0 (
    echo [INFO] No existe el repositorio remoto 'origin'.
    echo [INFO] Creando repositorio publico en GitHub con gh...
    gh repo create FlashGallery --public --source=. --remote=origin --push
    if !errorlevel! neq 0 (
        echo [AVISO] gh repo create no pudo crearlo directamente -o ya existe en tu cuenta-.
        for /f "tokens=*" %%U in ('gh api user --jq .login 2^>nul') do set "GH_USER=%%U"
        if defined GH_USER (
            echo [INFO] Vinculando a origin: https://github.com/!GH_USER!/FlashGallery.git
            git remote add origin "https://github.com/!GH_USER!/FlashGallery.git"
            git push -u origin main
        )
    )
    echo [OK] Repositorio remoto 'origin' configurado correctamente.
) else (
    echo [OK] Repositorio remoto 'origin' verificado.
)

:: ------------------------------------------------------
:: 3. AUTO-DETECCION O DESCARGA AUTOMATICA DE JDK 17
:: ------------------------------------------------------
set "NEED_JDK=1"
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\javac.exe" (
        "%JAVA_HOME%\bin\java.exe" -version 2>&1 | findstr /i "17\." >nul 2>&1
        if !errorlevel! equ 0 set "NEED_JDK=0"
    )
)
if "!NEED_JDK!"=="1" (
    echo.
    echo [INFO] Buscando JDK 17 compatible para Gradle...
    
    set "DETECTED_JDK="
    if exist "%SHARED_TOOLS%\.jdk" (
        for /d %%D in ("%SHARED_TOOLS%\.jdk\*17*") do set "DETECTED_JDK=%%D"
        if not defined DETECTED_JDK (
            for /d %%D in ("%SHARED_TOOLS%\.jdk\jdk*") do set "DETECTED_JDK=%%D"
        )
    )
    if not defined DETECTED_JDK if exist "%USERPROFILE%\.jdks" (
        for /d %%D in ("%USERPROFILE%\.jdks\*17*") do set "DETECTED_JDK=%%D"
        if not defined DETECTED_JDK (
            for /d %%D in ("%USERPROFILE%\.jdks\*") do set "DETECTED_JDK=%%D"
        )
    )
    if not defined DETECTED_JDK (
        for /d %%D in ("C:\Program Files\Java\*17*") do set "DETECTED_JDK=%%D"
    )
    if not defined DETECTED_JDK if exist "C:\Program Files\Android\Android Studio\jbr" set "DETECTED_JDK=C:\Program Files\Android\Android Studio\jbr"
    if not defined DETECTED_JDK if exist "C:\Program Files\Android\Android Studio\jre" set "DETECTED_JDK=C:\Program Files\Android\Android Studio\jre"
    if not defined DETECTED_JDK (
        for /d %%D in ("%LOCALAPPDATA%\Android\Sdk\jbr*") do set "DETECTED_JDK=%%D"
    )

    if defined DETECTED_JDK (
        set "JAVA_HOME=!DETECTED_JDK!"
        set "PATH=!JAVA_HOME!\bin;!PATH!"
        echo [OK] JDK asignado automaticamente: !JAVA_HOME!
    ) else (
        echo.
        echo [INFO] No se encontro ninguna instalacion local de JDK.
        echo [INFO] Descargando OpenJDK 17 Portable en la carpeta compartida SDK_Gradle...
        
        if not exist "%SHARED_TOOLS%\.jdk" mkdir "%SHARED_TOOLS%\.jdk"
        
        set "JDK_ZIP=%SHARED_TOOLS%\.jdk\openjdk17.zip"
        set "JDK_URL=https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.10%%2B7/OpenJDK17U-jdk_x64_windows_hotspot_17.0.10_7.zip"

        curl -L -s -o "!JDK_ZIP!" "!JDK_URL!" || powershell -Command "$ProgressPreference = 'SilentlyContinue'; Invoke-WebRequest -Uri '!JDK_URL!' -OutFile '!JDK_ZIP!'"
        
        if exist "!JDK_ZIP!" (
            echo [INFO] Descomprimiendo JDK en la carpeta compartida SDK_Gradle...
            powershell -Command "Expand-Archive -Path '!JDK_ZIP!' -DestinationPath '%SHARED_TOOLS%\.jdk' -Force"
            del /f /q "!JDK_ZIP!" >nul 2>&1
            
            for /d %%D in ("%SHARED_TOOLS%\.jdk\jdk*") do set "DETECTED_JDK=%%D"
            if defined DETECTED_JDK (
                set "JAVA_HOME=!DETECTED_JDK!"
                set "PATH=!JAVA_HOME!\bin;!PATH!"
                echo [OK] OpenJDK 17 descargado y configurado correctamente en SDK_Gradle.
            ) else (
                echo [ERROR] No se pudo extraer OpenJDK.
                pause
                exit /b 1
            )
        ) else (
            echo [ERROR] Fallo la descarga de OpenJDK. Comprueba tu conexion a Internet.
            pause
            exit /b 1
        )
    )
)

:: ------------------------------------------------------
:: 4. AUTO-DETECCION O DESCARGA AUTOMATICA DEL SDK ANDROID
:: ------------------------------------------------------
echo.
echo [INFO] Verificando ubicacion del SDK de Android...

set "DETECTED_SDK="
if exist "%SHARED_TOOLS%\.sdk\platforms\android-34" set "DETECTED_SDK=%SHARED_TOOLS%\.sdk"
if not defined DETECTED_SDK if defined ANDROID_HOME if exist "%ANDROID_HOME%\platforms\android-34" set "DETECTED_SDK=%ANDROID_HOME%"
if not defined DETECTED_SDK if defined ANDROID_SDK_ROOT if exist "%ANDROID_SDK_ROOT%\platforms\android-34" set "DETECTED_SDK=%ANDROID_SDK_ROOT%"
if not defined DETECTED_SDK if exist "%LOCALAPPDATA%\Android\Sdk\platforms\android-34" set "DETECTED_SDK=%LOCALAPPDATA%\Android\Sdk"
if not defined DETECTED_SDK if exist "C:\Android\Sdk\platforms\android-34" set "DETECTED_SDK=C:\Android\Sdk"

if not defined DETECTED_SDK (
    echo.
    echo [INFO] No se encontro el SDK de Android completo en el sistema ni en SDK_Gradle.
    
    if not exist "%SHARED_TOOLS%\.sdk\cmdline-tools\latest\bin\sdkmanager.bat" (
        echo [INFO] Descargando Android Command Line Tools en SDK_Gradle...
        if not exist "%SHARED_TOOLS%\.sdk" mkdir "%SHARED_TOOLS%\.sdk"
        
        set "SDK_ZIP=%SHARED_TOOLS%\.sdk\cmdline.zip"
        set "SDK_URL=https://dl.google.com/android/repository/commandlinetools-win-10406996_latest.zip"

        curl -L -s -o "!SDK_ZIP!" "!SDK_URL!" || powershell -Command "$ProgressPreference = 'SilentlyContinue'; Invoke-WebRequest -Uri '!SDK_URL!' -OutFile '!SDK_ZIP!'"
        
        if exist "!SDK_ZIP!" (
            echo [INFO] Descomprimiendo Command Line Tools...
            powershell -Command "Expand-Archive -Path '!SDK_ZIP!' -DestinationPath '%SHARED_TOOLS%\.sdk\temp_cmd' -Force"
            del /f /q "!SDK_ZIP!" >nul 2>&1
            
            if not exist "%SHARED_TOOLS%\.sdk\cmdline-tools\latest" mkdir "%SHARED_TOOLS%\.sdk\cmdline-tools\latest"
            xcopy "%SHARED_TOOLS%\.sdk\temp_cmd\cmdline-tools\*" "%SHARED_TOOLS%\.sdk\cmdline-tools\latest\" /E /I /Y >nul
            rmdir /s /q "%SHARED_TOOLS%\.sdk\temp_cmd" >nul 2>&1
        ) else (
            echo [ERROR] Fallo la descarga del SDK de Android.
            pause
            exit /b 1
        )
    )
    
    echo [INFO] Aceptando licencias de Google Android SDK...
    powershell -Command "& {for ($i=0; $i -lt 15; $i++) { echo y }}" | "%SHARED_TOOLS%\.sdk\cmdline-tools\latest\bin\sdkmanager.bat" --sdk_root="%SHARED_TOOLS%\.sdk" --licenses >nul 2>&1
    
    echo [INFO] Instalando SDK Platform 34 y Build-Tools mediante sdkmanager...
    powershell -Command "& {for ($i=0; $i -lt 15; $i++) { echo y }}" | "%SHARED_TOOLS%\.sdk\cmdline-tools\latest\bin\sdkmanager.bat" --sdk_root="%SHARED_TOOLS%\.sdk" "platforms;android-34" "build-tools;34.0.0" "platform-tools"
    
    set "DETECTED_SDK=%SHARED_TOOLS%\.sdk"
    echo [OK] SDK de Android e licencias configuradas en la carpeta compartida SDK_Gradle\.sdk.
)

set "ANDROID_HOME=!DETECTED_SDK!"
set "PATH=!ANDROID_HOME!\platform-tools;!PATH!"
echo [OK] SDK de Android localizado en: !ANDROID_HOME!

set "ESCAPED_SDK=!DETECTED_SDK:\=/!"
echo sdk.dir=!ESCAPED_SDK!> local.properties
echo [OK] local.properties generado correctamente.

:: ------------------------------------------------------
:: 5. GRADLE WRAPPER Y RECURSOS
:: ------------------------------------------------------
if not exist "gradlew.bat" (
    echo.
    echo [INFO] No se detecto Gradle Wrapper en el proyecto.
    echo [INFO] Configurando componentes de Gradle...
    
    if not exist "gradle\wrapper" mkdir gradle\wrapper

    if exist "%SHARED_TOOLS%\gradle_wrapper\gradlew.bat" (
        copy /y "%SHARED_TOOLS%\gradle_wrapper\gradlew.bat" "gradlew.bat" >nul
        copy /y "%SHARED_TOOLS%\gradle_wrapper\gradlew" "gradlew" >nul
        copy /y "%SHARED_TOOLS%\gradle_wrapper\gradle-wrapper.jar" "gradle\wrapper\gradle-wrapper.jar" >nul
        copy /y "%SHARED_TOOLS%\gradle_wrapper\gradle-wrapper.properties" "gradle\wrapper\gradle-wrapper.properties" >nul
    ) else (
        curl -L -s -o "gradlew.bat" "https://raw.githubusercontent.com/gradle/gradle/v8.2.0/gradlew.bat"
        curl -L -s -o "gradlew" "https://raw.githubusercontent.com/gradle/gradle/v8.2.0/gradlew"
        curl -L -s -o "gradle\wrapper\gradle-wrapper.jar" "https://raw.githubusercontent.com/gradle/gradle/v8.2.0/gradle/wrapper/gradle-wrapper.jar"

        (
            echo distributionBase=GRADLE_USER_HOME
            echo distributionPath=wrapper/dists
            echo distributionUrl=https\://services.gradle.org/distributions/gradle-8.2-bin.zip
            echo networkTimeout=10000
            echo validateDistributionUrl=true
            echo zipStoreBase=GRADLE_USER_HOME
            echo zipStorePath=wrapper/dists
        ) > gradle\wrapper\gradle-wrapper.properties
    )

    echo [OK] Componentes de Gradle instalados correctamente.
)

:: Iconos de aplicacion por defecto si faltan
if not exist "app\src\main\res\mipmap-hdpi\ic_launcher.png" (
    if not exist "app\src\main\res\mipmap-hdpi\ic_launcher.xml" (
        if not exist "app\src\main\res\mipmap-anydpi-v26\ic_launcher.xml" (
            echo.
            echo [INFO] Generando iconos de aplicacion por defecto...
            if not exist "app\src\main\res\mipmap-hdpi" mkdir "app\src\main\res\mipmap-hdpi"
            
            (
                echo ^<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108"^>
                echo     ^<path android:fillColor="#3DDC84" android:pathData="M0,0h108v108h-108z"/^>
                echo ^</vector^>
            ) > "app\src\main\res\mipmap-hdpi\ic_launcher.xml"
            
            (
                echo ^<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108"^>
                echo     ^<path android:fillColor="#3DDC84" android:pathData="M0,0h108v108h-108z"/^>
                echo ^</vector^>
            ) > "app\src\main\res\mipmap-hdpi\ic_launcher_round.xml"
            echo [OK] Iconos ic_launcher creados correctamente.
        )
    )
)

:: ------------------------------------------------------
:: 6. EXTRACCION DINAMICA DE VERSION
:: ------------------------------------------------------
set "APP_VERSION="
for /f "tokens=2 delims==" %%V in ('findstr /i "versionName" app\build.gradle.kts') do (
    set "RAW_VER=%%~V"
    set "APP_VERSION=!RAW_VER:"=!"
    set "APP_VERSION=!APP_VERSION: =!"
)
if not defined APP_VERSION set "APP_VERSION=1.10"
set "TAG=v!APP_VERSION!"

echo.
echo ====================================================
echo [VERSION DETECTADA]: FlashGallery !TAG!
echo ====================================================

:: ------------------------------------------------------
:: 7. COMPILACION CON GRADLE
:: ------------------------------------------------------
echo.
echo [1/4] Compilando APK con Gradle (!TAG!)...

call gradlew.bat assembleDebug

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] La compilacion ha fallado. Revisa el codigo Kotlin o los XML.
    pause
    exit /b %errorlevel%
)

set "ORIGINAL_APK=%~dp0app\build\outputs\apk\debug\app-debug.apk"
set "RELEASE_APK=%~dp0FlashGallery.apk"

if not exist "%ORIGINAL_APK%" (
    echo [ERROR] No se encontro el archivo APK compilado en: %ORIGINAL_APK%
    pause
    exit /b 1
)

:: Copiar/Renombrar APK como FlashGallery.apk para la Release
copy /y "%ORIGINAL_APK%" "%RELEASE_APK%" >nul
echo.
echo ====================================================
echo [OK] APK CREADA CON EXITO EN:
echo %RELEASE_APK%
echo ====================================================

:: ------------------------------------------------------
:: 8. DESPLIEGUE POR ADB (SI HAY DISPOSITIVO CONECTADO)
:: ------------------------------------------------------
echo.
echo [2/4] Comprobando conexion USB por ADB...
adb devices > "%TEMP%\adb_devices.txt" 2>&1
findstr /R /C:"\<body>" "%TEMP%\adb_devices.txt" >nul 2>&1

adb devices | findstr /V "List of devices attached" | findstr "device" >nul 2>&1
if %errorlevel% neq 0 (
    echo [AVISO] No se detecto ningun movil por ADB. Omitiendo instalacion local...
) else (
    echo [INFO] Instalando APK en el dispositivo por ADB...
    adb install -r "%RELEASE_APK%"
    if !errorlevel! equ 0 (
        echo [INFO] Lanzando FlashGallery en el movil...
        adb shell am start -n com.flashchip.flashgallery/.MainActivity >nul 2>&1
        echo [OK] Aplicacion instalada y abierta en el dispositivo.
    ) else (
        echo [AVISO] Fallo la instalacion por ADB. Continuando con la publicacion...
    )
)

:: ------------------------------------------------------
:: 9. SUBIDA A GIT (COMMIT Y PUSH)
:: ------------------------------------------------------
echo.
echo [3/4] Sincronizando cambios con Git...

git add .
git diff --staged --quiet >nul 2>&1
if %errorlevel% neq 0 (
    echo [INFO] Creando commit para la version !TAG!...
    git commit -m "Actualizacion version !TAG! - FlashGallery by WoRFReD"
) else (
    echo [INFO] No hay cambios de codigo pendientes de commit.
)

echo [INFO] Subiendo cambios a la rama 'main' de GitHub...
git push -u origin main
if %errorlevel% neq 0 (
    echo [AVISO] No se pudo hacer push directo a origin main. Revisa conexion o credenciales.
) else (
    echo [OK] Codigo fuente sincronizado en GitHub.
)

:: ------------------------------------------------------
:: 10. PUBLICACION DE RELEASE OTA EN GITHUB CLI
:: ------------------------------------------------------
echo.
echo [4/4] Publicando Release OTA oficial en GitHub (!TAG!)...

gh release view !TAG! >nul 2>&1
if %errorlevel% equ 0 (
    echo [INFO] La release !TAG! ya existe en GitHub. Actualizando archivo FlashGallery.apk...
    gh release upload !TAG! "FlashGallery.apk" --clobber
) else (
    echo [INFO] Creando release !TAG! y adjuntando FlashGallery.apk...
    gh release create !TAG! "FlashGallery.apk" --title "FlashGallery !TAG!" --notes "Release oficial de FlashGallery !TAG! por WoRFReD - Canal: FlashChip. Instalador FlashGallery.apk listo para descargar e instalar."
)

if %errorlevel% equ 0 (
    echo.
    echo ====================================================
    echo   [OK] RELEASE OTA PUBLICADA CON EXITO EN GITHUB
    echo   Tag: !TAG!
    echo   Archivo: FlashGallery.apk
    echo ====================================================
) else (
    echo.
    echo [AVISO] No se pudo completar la release automatica en GitHub.
)

echo.
echo ====================================================
echo   Proceso completado - FlashGallery !TAG!
echo   by WoRFReD - Canal: FlashChip (https://youtube.com/@flashchiped)
echo ====================================================
pause