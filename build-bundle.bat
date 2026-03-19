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
set MODULE_PATH=lib\javafx-base-21-win.jar;lib\javafx-controls-21-win.jar;lib\javafx-graphics-21-win.jar;lib\javafx-fxml-21-win.jar
set CLASSPATH=lib\mysql-connector-j-8.3.0.jar;lib\postgresql-42.7.2.jar;lib\mssql-jdbc-12.6.1.jre11.jar;lib\commons-csv-1.10.0.jar

REM 3. Compile Java source files
echo [2/5] Compiling Java source files...
javac --module-path "%MODULE_PATH%" --add-modules javafx.controls,javafx.fxml,javafx.graphics ^
      -cp "%CLASSPATH%" -d target\classes ^
      src\main\java\querycraft\*.java ^
      src\main\java\querycraft\model\*.java ^
      src\main\java\querycraft\service\*.java ^
      src\main\java\querycraft\ui\*.java ^
      src\main\java\querycraft\util\*.java

if %ERRORLEVEL% neq 0 (
    echo [ERROR] Compilation failed.
    pause
    exit /b %ERRORLEVEL%
)

REM 4. Create Manifest and JAR
echo [3/5] Packing resources and creating JAR with Manifest...
if exist "src\main\resources" xcopy /s /e /y src\main\resources\* target\classes\

REM Build library list for Manifest Class-Path
set MANIFEST_CP=
for %%f in (lib\*.jar) do (
    set MANIFEST_CP=!MANIFEST_CP! %%~nxf
)

echo Main-Class: querycraft.QueryCraftApp > manifest_temp.txt
echo Class-Path: !MANIFEST_CP! >> manifest_temp.txt

jar --create --file target\bundle_input\QueryApp.jar --manifest manifest_temp.txt -C target\classes .
del manifest_temp.txt

REM 5. Collect all libraries into bundle_input
echo [4/5] Collecting libraries...
xcopy /y lib\*.jar target\bundle_input\

REM 6. Create App Image using jpackage
echo [5/5] Creating Portable App Bundle (EXE)...
echo.

REM IMPORTANT: Added jdk.charsets for Thai language support
jpackage ^
  --name QueryCraft ^
  --input target\bundle_input ^
  --dest build_output ^
  --main-jar QueryApp.jar ^
  --main-class querycraft.QueryCraftApp ^
  --module-path "%MODULE_PATH%" ^
  --add-modules javafx.controls,javafx.fxml,javafx.graphics,java.sql,java.naming,jdk.charsets ^
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
echo ==========================================
echo   SUCCESS! 
echo ==========================================
echo.
echo Portable app created in: build_output\QueryCraft
echo.
pause
