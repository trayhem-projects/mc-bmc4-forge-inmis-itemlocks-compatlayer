@echo off
setlocal
where gradle >nul 2>nul
if errorlevel 1 (
  echo Gradle was not found. Install Gradle 8.1+ and Java 17, then run this again.
  pause
  exit /b 1
)
if not exist "libs\ItemLocks-Forge-1.20.1-1.3.8.jar" (
  echo Missing libs\ItemLocks-Forge-1.20.1-1.3.8.jar
  echo Copy the ItemLocks jar into the libs folder first.
  pause
  exit /b 1
)
if not exist "libs\inmis-2.7.2-2.9.4-1.20.1.jar" (
  echo Missing libs\inmis-2.7.2-2.9.4-1.20.1.jar
  echo Copy the Inmis jar into the libs folder first.
  pause
  exit /b 1
)
gradle build
if errorlevel 1 (
  echo Build failed. Check the error output above.
  pause
  exit /b 1
)
echo.
echo Build complete. Check build\libs\
pause
