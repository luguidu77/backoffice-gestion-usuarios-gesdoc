@echo off
setlocal
echo ============================================================
echo   INICIANDO ENTORNO LOCAL BACKOFFICE
echo ============================================================
echo.

set "ROOT=%~dp0"
cd /d "%ROOT%"

echo.
echo [1/2] Iniciando Backend (Spring Boot)...
echo Compilando y arrancando el Backend en ventana separada...
start "Backend - Spring Boot" cmd /k "cd /d "%ROOT%" && mvn spring-boot:run -Dspring-boot.run.profiles=local"

:: Pausa para que el backend (Spring Boot ~8085) termine de arrancar
echo Esperando a que el backend arranque (15 segundos)...
timeout /t 15 /nobreak >nul

echo.
echo [2/2] Iniciando Frontend (Angular)...
echo Abriendo una nueva ventana para el Frontend...
start "Frontend - Angular" cmd /c "cd frontend & npm start & pause"

echo.
echo ============================================================
echo Entorno Local iniciado en segundo plano.
echo - Alfresco API:     http://localhost:8081/alfresco
echo - Alfresco Share:   http://localhost:8080/share
echo - Frontend Angular: http://localhost:4201 (Proxy a backend en 8085)
echo ============================================================
echo Las ventanas del backend y frontend permaneceran abiertas. 
echo Puedes cerrarlas manualmente o usar detener.bat
echo.

endlocal
