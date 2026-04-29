@echo off
echo Installing frontend dependencies...
echo.
npm install
echo.
if %ERRORLEVEL% EQU 0 (
    echo ✅ Dependencies installed successfully!
    echo.
    echo You can now run: npm run dev
) else (
    echo ❌ Installation failed. Check the error above.
    pause
)
