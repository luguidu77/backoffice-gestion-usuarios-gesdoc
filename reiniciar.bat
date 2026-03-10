@echo off
setlocal
echo ============================================================
echo   REINICIANDO ENTORNO LOCAL BACKOFFICE
echo ============================================================
echo.

set "ROOT=%~dp0"
cd /d "%ROOT%"

call "%ROOT%detener.bat"
if errorlevel 1 (
  echo ERROR: No se pudo detener correctamente el entorno.
  pause
  exit /b 1
)

echo Esperando 3 segundos antes de volver a arrancar...
timeout /t 3 /nobreak >nul

call "%ROOT%iniciar.bat"
if errorlevel 1 (
  echo ERROR: No se pudo iniciar correctamente el entorno.
  pause
  exit /b 1
)

endlocal
