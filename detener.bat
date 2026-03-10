@echo off
setlocal ENABLEDELAYEDEXPANSION
echo ============================================================
echo   DETENIENDO ENTORNO LOCAL BACKOFFICE
echo ============================================================
echo.

set "ROOT=%~dp0"
cd /d "%ROOT%"

echo [1/2] Deteniendo procesos de Frontend (puerto 4201)...
:: Encuentra el PID escuchando en el puerto 4201 y lo mata
for /f "tokens=5" %%a in ('netstat -aon ^| find ":4201" ^| find "LISTENING"') do (
    if not "%%a"=="0" (
        echo Matando PID %%a (Frontend)
        taskkill /F /PID %%a >nul 2>&1
    )
)

echo.
echo [2/2] Deteniendo procesos de Backend (puerto 8085)...
:: Encuentra el PID escuchando en el puerto 8085 y lo mata
for /f "tokens=5" %%a in ('netstat -aon ^| find ":8085" ^| find "LISTENING"') do (
    if not "%%a"=="0" (
        echo Matando PID %%a (Backend)
        taskkill /F /PID %%a >nul 2>&1
    )
)

echo.
echo ============================================================
echo Entorno Local detenido por completo.
echo ============================================================
echo.

endlocal
