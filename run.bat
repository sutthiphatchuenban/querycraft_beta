@echo off
chcp 65001 >nul
echo QueryCraft Launcher (Clean Build)
echo ==================================
echo.

REM Delete old compiled classes to force rebuild
echo Cleaning old compiled files...
if exist "target\classes" rmdir /s /q target\classes
mkdir target\classes
echo [OK] Cleaned

echo.

REM Create lib directory
if not exist "lib" mkdir lib

REM === Download JavaFX ===
echo Checking JavaFX libraries...

if not exist "lib\javafx-base-21-win.jar" (
    echo Downloading JavaFX Base...
    powershell -Command "& {$progressPreference='silentlyContinue'; Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/openjfx/javafx-base/21/javafx-base-21-win.jar' -OutFile 'lib\javafx-base-21-win.jar'}"
    if exist "lib\javafx-base-21-win.jar" echo [OK] JavaFX Base downloaded
)

if not exist "lib\javafx-controls-21-win.jar" (
    echo Downloading JavaFX Controls...
    powershell -Command "& {$progressPreference='silentlyContinue'; Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/openjfx/javafx-controls/21/javafx-controls-21-win.jar' -OutFile 'lib\javafx-controls-21-win.jar'}"
    if exist "lib\javafx-controls-21-win.jar" echo [OK] JavaFX Controls downloaded
)

if not exist "lib\javafx-graphics-21-win.jar" (
    echo Downloading JavaFX Graphics...
    powershell -Command "& {$progressPreference='silentlyContinue'; Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/openjfx/javafx-graphics/21/javafx-graphics-21-win.jar' -OutFile 'lib\javafx-graphics-21-win.jar'}"
    if exist "lib\javafx-graphics-21-win.jar" echo [OK] JavaFX Graphics downloaded
)

if not exist "lib\javafx-fxml-21-win.jar" (
    echo Downloading JavaFX FXML...
    powershell -Command "& {$progressPreference='silentlyContinue'; Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/openjfx/javafx-fxml/21/javafx-fxml-21-win.jar' -OutFile 'lib\javafx-fxml-21-win.jar'}"
    if exist "lib\javafx-fxml-21-win.jar" echo [OK] JavaFX FXML downloaded
)

REM === Download JDBC Drivers ===
echo.
echo Checking JDBC Drivers...

if not exist "lib\mysql-connector-j-8.3.0.jar" (
    echo Downloading MySQL Driver...
    powershell -Command "& {$progressPreference='silentlyContinue'; Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar' -OutFile 'lib\mysql-connector-j-8.3.0.jar'}"
    if exist "lib\mysql-connector-j-8.3.0.jar" echo [OK] MySQL Driver downloaded
)

if not exist "lib\postgresql-42.7.2.jar" (
    echo Downloading PostgreSQL Driver...
    powershell -Command "& {$progressPreference='silentlyContinue'; Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.2/postgresql-42.7.2.jar' -OutFile 'lib\postgresql-42.7.2.jar'}"
    if exist "lib\postgresql-42.7.2.jar" echo [OK] PostgreSQL Driver downloaded
)

if not exist "lib\mssql-jdbc-12.6.1.jre11.jar" (
    echo Downloading MSSQL Driver...
    powershell -Command "& {$progressPreference='silentlyContinue'; Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/microsoft/sqlserver/mssql-jdbc/12.6.1.jre11/mssql-jdbc-12.6.1.jre11.jar' -OutFile 'lib\mssql-jdbc-12.6.1.jre11.jar'}"
    if exist "lib\mssql-jdbc-12.6.1.jre11.jar" echo [OK] MSSQL Driver downloaded
)

if not exist "lib\commons-csv-1.10.0.jar" (
    echo Downloading Commons CSV...
    powershell -Command "& {$progressPreference='silentlyContinue'; Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/apache/commons/commons-csv/1.10.0/commons-csv-1.10.0.jar' -OutFile 'lib\commons-csv-1.10.0.jar'}"
    if exist "lib\commons-csv-1.10.0.jar" echo [OK] Commons CSV downloaded
)

REM === Download H2 Database for CSV support ===
if not exist "lib\h2-2.2.224.jar" (
    echo Downloading H2 Database for CSV support...
    powershell -Command "& {$progressPreference='silentlyContinue'; Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/h2database/h2/2.2.224/h2-2.2.224.jar' -OutFile 'lib\h2-2.2.224.jar'}"
    if exist "lib\h2-2.2.224.jar" echo [OK] H2 Database downloaded
)

REM === Download RichTextFX and Dependencies ===
if not exist "lib\richtextfx-0.11.2.jar" (
    echo Downloading RichTextFX 0.11.2...
    powershell -Command "& {$progressPreference='silentlyContinue'; Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/fxmisc/richtext/richtextfx/0.11.2/richtextfx-0.11.2.jar' -OutFile 'lib\richtextfx-0.11.2.jar'}"
)
if not exist "lib\reactfx-2.0-M5.jar" (
    echo Downloading ReactFX...
    powershell -Command "& {$progressPreference='silentlyContinue'; Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/reactfx/reactfx/2.0-M5/reactfx-2.0-M5.jar' -OutFile 'lib\reactfx-2.0-M5.jar'}"
)
if not exist "lib\undofx-2.1.1.jar" (
    echo Downloading UndoFX...
    powershell -Command "& {$progressPreference='silentlyContinue'; Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/fxmisc/undo/undofx/2.1.1/undofx-2.1.1.jar' -OutFile 'lib\undofx-2.1.1.jar'}"
)
if not exist "lib\flowless-0.7.2.jar" (
    echo Downloading Flowless...
    powershell -Command "& {$progressPreference='silentlyContinue'; Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/fxmisc/flowless/flowless/0.7.2/flowless-0.7.2.jar' -OutFile 'lib\flowless-0.7.2.jar'}"
)
if not exist "lib\wellbehavedfx-0.3.3.jar" (
    echo Downloading WellBehavedFX...
    powershell -Command "& {$progressPreference='silentlyContinue'; Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/fxmisc/wellbehaved/wellbehavedfx/0.3.3/wellbehavedfx-0.3.3.jar' -OutFile 'lib\wellbehavedfx-0.3.3.jar'}"
)

echo [OK] All Libraries Checked

echo.

REM === Compile Java files ===
echo Compiling Java files...

REM === Setup Paths ===
set MODULE_PATH=lib\javafx-base-21-win.jar;lib\javafx-controls-21-win.jar;lib\javafx-graphics-21-win.jar;lib\javafx-fxml-21-win.jar;lib\richtextfx-0.11.2.jar;lib\reactfx-2.0-M5.jar;lib\undofx-2.1.1.jar;lib\flowless-0.7.2.jar;lib\wellbehavedfx-0.3.3.jar;lib\mysql-connector-j-8.3.0.jar;lib\postgresql-42.7.2.jar;lib\mssql-jdbc-12.6.1.jre11.jar;lib\commons-csv-1.10.0.jar;lib\h2-2.2.224.jar

REM === Compile Java files (Modular) ===
echo Compiling Java modules...
javac -encoding UTF-8 --module-path "%MODULE_PATH%" -d target\classes ^
    src\main\java\module-info.java ^
    src\main\java\querycraft\*.java ^
    src\main\java\querycraft\dialect\*.java ^
    src\main\java\querycraft\model\*.java ^
    src\main\java\querycraft\service\*.java ^
    src\main\java\querycraft\service\handler\*.java ^
    src\main\java\querycraft\util\*.java ^
    src\main\java\querycraft\ui\*.java ^
    src\main\java\querycraft\ui\component\*.java

if errorlevel 1 goto compile_error

echo [OK] Compilation successful

echo.
echo Copying resources...
if exist "src\main\resources" xcopy /s /e /y src\main\resources\* target\classes\
echo [OK] Resources copied
echo Starting QueryCraft (Modularized)...
echo.

REM Run with JPMS
java --module-path "%MODULE_PATH%;target\classes" ^
     --add-modules mysql.connector.j,org.postgresql.jdbc,com.microsoft.sqlserver.jdbc,wellbehavedfx,com.h2database ^
     -m querycraft/querycraft.QueryCraftApp

if errorlevel 1 (
    echo.
    echo [ERROR] Failed to start application
    pause
)

exit /b 0

:compile_error
echo.
echo [ERROR] Compilation failed!
echo Please make sure you have JDK 17+ installed
pause
exit /b 1
