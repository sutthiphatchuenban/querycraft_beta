@echo off
setlocal enabledelayedexpansion

echo ==========================================
echo    QueryCraft EXE Bundle Builder (Thai Fix)
echo ==========================================
echo.

REM 1. Clean directories
echo [1/5] Preparing target directories...
if exist "target" rmdir /s /q "target"
if exist "build_output" rmdir /s /q "build_output"
mkdir target\classes
mkdir target\bundle_input
mkdir build_output

REM 2. Setup Paths
set MODULE_PATH=lib\javafx-base-21-win.jar;lib\javafx-controls-21-win.jar;lib\javafx-graphics-21-win.jar;lib\javafx-fxml-21-win.jar;lib\richtextfx-0.11.2.jar;lib\reactfx-2.0-M5.jar;lib\undofx-2.1.1.jar;lib\flowless-0.7.2.jar;lib\wellbehavedfx-0.3.3.jar;lib\mysql-connector-j-8.3.0.jar;lib\postgresql-42.7.2.jar;lib\mssql-jdbc-12.6.1.jre11.jar;lib\commons-csv-1.10.0.jar

REM 3. Compile Java source files (Modular)
echo [2/5] Compiling Java modules...
javac -encoding UTF-8 --module-path "%MODULE_PATH%" -d target\classes ^
      src\main\java\module-info.java ^
      src\main\java\querycraft\*.java ^
      src\main\java\querycraft\model\*.java ^
      src\main\java\querycraft\service\*.java ^
      src\main\java\querycraft\ui\*.java ^
      src\main\java\querycraft\ui\component\*.java ^
      src\main\java\querycraft\util\*.java

if %ERRORLEVEL% neq 0 (
    echo [ERROR] Compilation failed.
    pause
    exit /b %ERRORLEVEL%
)

REM 4. Prepare Bundle Input (Hybrid Approach)
echo [3/5] Packing resources and creating JAR...
if exist "src\main\resources" xcopy /s /e /y src\main\resources\* target\classes\

REM Create a JAR for our application
jar --create --file target\bundle_input\QueryApp.jar -C target\classes .

REM Copy all libraries to bundle_input
xcopy /y lib\*.jar target\bundle_input\

REM 5. Create App Image using jpackage
echo [4/5] Creating App Bundle (EXE)...
echo.

REM Note: We don't use --module here to avoid jlink choosing automatic modules as core.
REM Instead we use --input and explicitly add necessary platform modules.
jpackage ^
  --name QueryCraft ^
  --input target\bundle_input ^
  --dest build_output ^
  --main-jar QueryApp.jar ^
  --main-class querycraft.QueryCraftApp ^
  --module-path "%MODULE_PATH%" ^
  --add-modules javafx.controls,javafx.fxml,javafx.graphics,java.sql,java.naming,jdk.charsets,java.prefs,java.desktop ^
  --type app-image ^
  --icon "src\main\resources\images\logo.ico" ^
  --vendor "Antigravity" ^
  --description "QueryCraft - Database Query Tool" ^
  --app-version "1.0.0" ^
  --java-options "-Dfile.encoding=UTF-8"

if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] jpackage failed.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo [6/6] Creating ZIP archive for distribution...
powershell -Command "Compress-Archive -Path 'build_output\QueryCraft' -DestinationPath 'build_output\QueryCraft.zip' -Force"
if %ERRORLEVEL% neq 0 (
    echo [WARNING] Failed to create ZIP archive.
) else (
    echo [OK] ZIP archive created: build_output\QueryCraft.zip
)

echo.
echo ==========================================
echo   SUCCESS! 
echo ==========================================
echo.
echo Portable app created in: build_output\QueryCraft
echo Downloadable ZIP: build_output\QueryCraft.zip
echo.
pause
