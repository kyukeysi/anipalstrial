@echo off
where java >nul 2>&1
if errorlevel 1 (
  echo Java was not found on PATH. Install JDK 25 and try again.
  exit /b 1
)

call "%~dp0mvnw.cmd" spring-boot:run
