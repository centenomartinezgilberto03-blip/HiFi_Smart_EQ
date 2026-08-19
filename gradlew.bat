@echo off
where gradle >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  gradle %*
  exit /b %ERRORLEVEL%
)
echo No Gradle installation is available and this offline archive does not contain gradle-wrapper.jar.
echo Open the project in Android Studio, or install Gradle 8.7+.
exit /b 1
