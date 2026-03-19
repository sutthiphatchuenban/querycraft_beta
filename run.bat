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

echo.

REM === Compile Java files ===
echo Compiling Java files...

set JAVAFX_CP=lib\javafx-base-21-win.jar;lib\javafx-controls-21-win.jar;lib\javafx-graphics-21-win.jar;lib\javafx-fxml-21-win.jar
set LIBS_CP=lib\mysql-connector-j-8.3.0.jar;lib\postgresql-42.7.2.jar;lib\mssql-jdbc-12.6.1.jre11.jar;lib\commons-csv-1.10.0.jar

REM Compile model classes
echo - Compiling model classes...
javac -cp "%JAVAFX_CP%;%LIBS_CP%" -d target\classes src\main\java\querycraft\model\*.java 2>nul
if errorlevel 1 goto compile_error

REM Compile service classes
echo - Compiling service classes...
javac -cp "%JAVAFX_CP%;%LIBS_CP%;target\classes" -d target\classes src\main\java\querycraft\service\*.java 2>nul
if errorlevel 1 goto compile_error

REM Compile util classes
echo - Compiling util classes...
javac -cp "%JAVAFX_CP%;%LIBS_CP%;target\classes" -d target\classes src\main\java\querycraft\util\*.java 2>nul
if errorlevel 1 goto compile_error

REM Compile ui classes
echo - Compiling ui classes...
javac -cp "%JAVAFX_CP%;%LIBS_CP%;target\classes" -d target\classes src\main\java\querycraft\ui\*.java 2>nul
if errorlevel 1 goto compile_error

REM Compile main classes
echo - Compiling main classes...
javac -cp "%JAVAFX_CP%;%LIBS_CP%;target\classes" -d target\classes src\main\java\querycraft\*.java 2>nul
if errorlevel 1 goto compile_error

echo [OK] Compilation successful

echo.
echo Copying resources...
if exist "src\main\resources" xcopy /s /e /y src\main\resources\* target\classes\
echo [OK] Resources copied
echo Starting QueryCraft...
echo.

REM Build module path for JavaFX
set MODULE_PATH=lib\javafx-base-21-win.jar;lib\javafx-controls-21-win.jar;lib\javafx-graphics-21-win.jar;lib\javafx-fxml-21-win.jar
set CLASSPATH=target\classes;lib\mysql-connector-j-8.3.0.jar;lib\postgresql-42.7.2.jar;lib\mssql-jdbc-12.6.1.jre11.jar;lib\commons-csv-1.10.0.jar

REM Run with JavaFX modules
java --module-path "%MODULE_PATH%" --add-modules javafx.controls,javafx.fxml,javafx.graphics -cp "%CLASSPATH%" querycraft.QueryCraftApp

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
