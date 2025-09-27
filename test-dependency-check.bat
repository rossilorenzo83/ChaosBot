@echo off
echo Loading environment variables from .env.local...

if not exist .env.local (
    echo ERROR: .env.local file not found!
    echo Please create .env.local and add your API credentials
    pause
    exit /b 1
)

:: Load environment variables from .env.local
for /f "tokens=1,2 delims==" %%a in (.env.local) do (
    if not "%%a"=="" if not "%%a:~0,1"=="#" (
        set "%%a=%%b"
        echo Set %%a=%%b
    )
)

echo.
echo Testing OWASP Dependency Check with environment variables...
echo NVD_API_KEY: %NVD_API_KEY%
echo OSS_INDEX_USERNAME: %OSS_INDEX_USERNAME%
echo OSS_INDEX_PASSWORD: %OSS_INDEX_PASSWORD%
echo.

:: Run dependency check with credentials passed as system properties
set "MAVEN_OPTS=-DnvdApiKey=%NVD_API_KEY% -DossIndexUsername=%OSS_INDEX_USERNAME% -DossIndexPassword=%OSS_INDEX_PASSWORD%"
"C:\Program Files\JetBrains\IntelliJ IDEA 2022.1\plugins\maven\lib\maven3\bin\mvn" dependency-check:check

pause