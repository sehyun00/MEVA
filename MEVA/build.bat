@echo off
echo ========================================
echo MEVA Project Build Script
echo ========================================
echo.

echo Cleaning bin directory...
if exist bin rmdir /s /q bin
mkdir bin

echo Compiling Java files with UTF-8 encoding...
dir /s /B src\main\java\*.java > sources.txt
javac -encoding UTF-8 -d bin @sources.txt
set COMPILE_ERROR=%errorlevel%
del sources.txt

if %COMPILE_ERROR% equ 0 (
    echo.
    echo ========================================
    echo BUILD SUCCESSFUL!
    echo ========================================
    echo.
    echo To run the application:
    echo   java -cp bin meva.Main
    echo.
) else (
    echo.
    echo ========================================
    echo BUILD FAILED!
    echo ========================================
    exit /b 1
)

