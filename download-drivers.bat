@echo off
chcp 65001 >nul
echo ============================================
echo Download All Libraries for QueryCraft
echo ============================================
echo.

REM Create lib directory
if not exist "lib" mkdir lib

REM === Download JavaFX ===
echo Downloading JavaFX libraries...

if not exist "lib\javafx-base-21-win.jar" (
    echo Downloading JavaFX Base...
    powershell -Command "& {$progressPreference='silentlyContinue'; Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/openjfx/javafx-base/21/javafx-base-21-win.jar' -OutFile 'lib\javafx-base-21-win.jar'}"
    if errorlevel 1 (
        echo Failed to download JavaFX Base.
        pause
        exit /b 1
    )
    echo [OK] JavaFX Base downloaded
)

if not exist "lib\javafx-controls-21-win.jar" (
    echo Downloading JavaFX Controls...
    powershell -Command "& {$progressPreference='silentlyContinue'; Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/openjfx/javafx-controls/21/javafx-controls-21-win.jar' -OutFile 'lib\javafx-controls-21-win.jar'}"
    if errorlevel 1 (
        echo Failed to download JavaFX Controls.
        pause
        exit /b 1
    )
    echo [OK] JavaFX Controls downloaded
)

if not exist "lib\javafx-graphics-21-win.jar" (
    echo Downloading JavaFX Graphics...
    powershell -Command "& {$progressPreference='silentlyContinue'; Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/openjfx/javafx-graphics/21/javafx-graphics-21-win.jar' -OutFile 'lib\javafx-graphics-21-win.jar'}"
    if errorlevel 1 (
        echo Failed to download JavaFX Graphics.
        pause
        exit /b 1
    )
    echo [OK] JavaFX Graphics downloaded
)

if not exist "lib\javafx-fxml-21-win.jar" (
    echo Downloading JavaFX FXML...
    powershell -Command "& {$progressPreference='silentlyContinue'; Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/openjfx/javafx-fxml/21/javafx-fxml-21-win.jar' -OutFile 'lib\javafx-fxml-21-win.jar'}"
    if errorlevel 1 (
        echo Failed to download JavaFX FXML.
        pause
        exit /b 1
    )
    echo [OK] JavaFX FXML downloaded
)

REM === Download JDBC Drivers ===
echo.
echo Downloading JDBC Drivers...

if not exist "lib\mysql-connector-j-8.3.0.jar" (
    echo Downloading MySQL Connector/J...
    powershell -Command "& {$progressPreference='silentlyContinue'; Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar' -OutFile 'lib\mysql-connector-j-8.3.0.jar'}"
    if errorlevel 1 (
        echo Failed to download MySQL driver.
        pause
        exit /b 1
    )
    echo [OK] MySQL driver downloaded
)

if not exist "lib\postgresql-42.7.2.jar" (
    echo Downloading PostgreSQL Driver...
    powershell -Command "& {$progressPreference='silentlyContinue'; Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.2/postgresql-42.7.2.jar' -OutFile 'lib\postgresql-42.7.2.jar'}"
    if errorlevel 1 (
        echo Failed to download PostgreSQL driver.
        pause
        exit /b 1
    )
    echo [OK] PostgreSQL driver downloaded
)

if not exist "lib\mssql-jdbc-12.6.1.jre11.jar" (
    echo Downloading Microsoft SQL Server Driver...
    powershell -Command "& {$progressPreference='silentlyContinue'; Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/microsoft/sqlserver/mssql-jdbc/12.6.1.jre11/mssql-jdbc-12.6.1.jre11.jar' -OutFile 'lib\mssql-jdbc-12.6.1.jre11.jar'}"
    if errorlevel 1 (
        echo Failed to download MSSQL driver.
        pause
        exit /b 1
    )
    echo [OK] MSSQL driver downloaded
)

REM === Download Commons CSV ===
if not exist "lib\commons-csv-1.10.0.jar" (
    echo Downloading Apache Commons CSV...
    powershell -Command "& {$progressPreference='silentlyContinue'; Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/apache/commons/commons-csv/1.10.0/commons-csv-1.10.0.jar' -OutFile 'lib\commons-csv-1.10.0.jar'}"
    if errorlevel 1 (
        echo Failed to download Commons CSV.
        pause
        exit /b 1
    )
    echo [OK] Commons CSV downloaded
)

REM === Download H2 Database for CSV support ===
if not exist "lib\h2-2.2.224.jar" (
    echo Downloading H2 Database for CSV support...
    powershell -Command "& {$progressPreference='silentlyContinue'; Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/h2database/h2/2.2.224/h2-2.2.224.jar' -OutFile 'lib\h2-2.2.224.jar'}"
    if errorlevel 1 (
        echo Failed to download H2 Database.
        pause
        exit /b 1
    )
    echo [OK] H2 Database downloaded
)

REM === Download RichTextFX and Dependencies ===
echo.
echo Downloading RichTextFX 0.11.2 (Syntax Highlighting)...

if not exist "lib\richtextfx-0.11.2.jar" (
    echo Downloading RichTextFX...
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
echo [OK] RichTextFX and dependencies downloaded

REM === Download SLF4J and Logback ===
echo.
echo Downloading Logging Libraries...

if not exist "lib\slf4j-api-2.0.12.jar" (
    echo Downloading SLF4J API...
    powershell -Command "& {$progressPreference='silentlyContinue'; Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.12/slf4j-api-2.0.12.jar' -OutFile 'lib\slf4j-api-2.0.12.jar'}"
)
if not exist "lib\logback-classic-1.5.0.jar" (
    echo Downloading Logback Classic...
    powershell -Command "& {$progressPreference='silentlyContinue'; Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/ch/qos/logback/logback-classic/1.5.0/logback-classic-1.5.0.jar' -OutFile 'lib\logback-classic-1.5.0.jar'}"
)
if not exist "lib\logback-core-1.5.0.jar" (
    echo Downloading Logback Core...
    powershell -Command "& {$progressPreference='silentlyContinue'; Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/ch/qos/logback/logback-core/1.5.0/logback-core-1.5.0.jar' -OutFile 'lib\logback-core-1.5.0.jar'}"
)
echo [OK] Logging libraries downloaded

REM === Download HikariCP ===
if not exist "lib\HikariCP-5.1.0.jar" (
    echo Downloading HikariCP...
    powershell -Command "& {$progressPreference='silentlyContinue'; Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/zaxxer/HikariCP/5.1.0/HikariCP-5.1.0.jar' -OutFile 'lib\HikariCP-5.1.0.jar'}"
    if errorlevel 1 (
        echo Failed to download HikariCP.
        pause
        exit /b 1
    )
    echo [OK] HikariCP downloaded
)

REM === Download SQL Server JDBC Auth DLL for Windows Authentication ===
if not exist "lib\mssql-jdbc_auth-12.6.1.x64.dll" (
    echo Downloading SQL Server JDBC Auth DLL for Windows Authentication...
    powershell -Command "& {$progressPreference='silentlyContinue'; Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/microsoft/sqlserver/mssql-jdbc_auth/12.6.1.x64/mssql-jdbc_auth-12.6.1.x64.dll' -OutFile 'lib\mssql-jdbc_auth-12.6.1.x64.dll'}"
    if exist "lib\mssql-jdbc_auth-12.6.1.x64.dll" echo [OK] SQL Server Auth DLL downloaded
)

echo.
echo ============================================
echo All libraries downloaded successfully!
echo Location: lib\
echo ============================================
pause
