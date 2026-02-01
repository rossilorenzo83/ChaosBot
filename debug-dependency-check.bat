@echo off
echo Debugging OWASP Dependency Check configuration...

if not exist .env.local (
    echo ERROR: .env.local file not found!
    pause
    exit /b 1
)

:: Load environment variables from .env.local
for /f "tokens=1,2 delims==" %%a in (.env.local) do (
    if not "%%a"=="" if not "%%a:~0,1"=="#" (
        set "%%a=%%b"
    )
)

echo.
echo Environment Variables:
echo NVD_API_KEY: %NVD_API_KEY%
echo OSS_INDEX_USERNAME: %OSS_INDEX_USERNAME%
echo OSS_INDEX_PASSWORD: %OSS_INDEX_PASSWORD%
echo.

echo Testing with debug output...
"C:\Program Files\JetBrains\IntelliJ IDEA 2022.1\plugins\maven\lib\maven3\bin\mvn" dependency-check:check -X

pause