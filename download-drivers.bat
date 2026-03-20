@echo off
chcp 65001 >nul
echo ============================================
echo Download JDBC Drivers for QueryCraft
echo ============================================
echo.

REM Create lib directory
if not exist "lib" mkdir lib

echo Downloading MySQL Connector/J...
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar' -OutFile 'lib\mysql-connector-j-8.3.0.jar'"
if errorlevel 1 (
    echo Failed to download MySQL driver. Please check your internet connection.
    pause
    exit /b 1
)
echo [OK] MySQL driver downloaded

echo.
echo Downloading PostgreSQL Driver...
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.2/postgresql-42.7.2.jar' -OutFile 'lib\postgresql-42.7.2.jar'"
if errorlevel 1 (
    echo Failed to download PostgreSQL driver.
    pause
    exit /b 1
)
echo [OK] PostgreSQL driver downloaded

echo.
echo Downloading Microsoft SQL Server Driver...
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/microsoft/sqlserver/mssql-jdbc/12.6.1.jre11/mssql-jdbc-12.6.1.jre11.jar' -OutFile 'lib\mssql-jdbc-12.6.1.jre11.jar'"
if errorlevel 1 (
    echo Failed to download MSSQL driver.
    pause
    exit /b 1
)
echo [OK] MSSQL driver downloaded

echo.
echo Downloading Apache Commons CSV...
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/apache/commons/commons-csv/1.10.0/commons-csv-1.10.0.jar' -OutFile 'lib\commons-csv-1.10.0.jar'"
if errorlevel 1 (
    echo Failed to download Commons CSV.
    pause
    exit /b 1
)
echo [OK] Commons CSV downloaded

echo.
echo Downloading RichTextFX 0.11.2 (Syntax Highlighting)...
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/fxmisc/richtext/richtextfx/0.11.2/richtextfx-0.11.2.jar' -OutFile 'lib\richtextfx-0.11.2.jar'"
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/reactfx/reactfx/2.0-M5/reactfx-2.0-M5.jar' -OutFile 'lib\reactfx-2.0-M5.jar'"
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/fxmisc/undo/undofx/2.1.1/undofx-2.1.1.jar' -OutFile 'lib\undofx-2.1.1.jar'"
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/fxmisc/flowless/flowless/0.7.2/flowless-0.7.2.jar' -OutFile 'lib\flowless-0.7.2.jar'"
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/fxmisc/wellbehaved/wellbehavedfx/0.3.3/wellbehavedfx-0.3.3.jar' -OutFile 'lib\wellbehavedfx-0.3.3.jar'"
if errorlevel 1 (
    echo Failed to download RichTextFX libraries.
    pause
    exit /b 1
)
echo [OK] RichTextFX and dependencies downloaded

echo.
echo ============================================
echo All drivers downloaded successfully!
echo Location: lib\
echo ============================================
pause
