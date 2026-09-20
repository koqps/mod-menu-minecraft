@echo off
setlocal EnableExtensions
set "GRADLE_VERSION=9.5.1"
set "BASE=%~dp0.gradle-bootstrap"
set "ZIP=%BASE%\gradle-%GRADLE_VERSION%-bin.zip"
set "GRADLE_HOME=%BASE%\gradle-%GRADLE_VERSION%"
set "GRADLE=%GRADLE_HOME%\bin\gradle.bat"

where java >nul 2>nul
if errorlevel 1 (
  echo ERROR: Java was not found. Install JDK 25 and reopen PowerShell.
  exit /b 1
)

if not exist "%GRADLE%" (
  echo Gradle %GRADLE_VERSION% is not installed for this project. Downloading it now...
  if not exist "%BASE%" mkdir "%BASE%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; Invoke-WebRequest -UseBasicParsing 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%ZIP%'"
  if errorlevel 1 (
    echo ERROR: Could not download Gradle.
    exit /b 1
  )
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; Expand-Archive -LiteralPath '%ZIP%' -DestinationPath '%BASE%' -Force"
  if errorlevel 1 (
    echo ERROR: Could not extract Gradle.
    exit /b 1
  )
)

call "%GRADLE%" %*
exit /b %ERRORLEVEL%
