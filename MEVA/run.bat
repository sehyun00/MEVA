@echo off
echo ========================================
echo MEVA Application Launcher
echo ========================================
echo.

if not exist bin\meva\Main.class (
    echo Error: Project not built yet!
    echo Please run build.bat first.
    echo.
    pause
    exit /b 1
)

echo Starting MEVA...
java -cp bin meva.Main

if %errorlevel% neq 0 (
    echo.
    echo Application terminated with errors.
    pause
)

