@echo off
setlocal

set "ROOT=%~dp0"
cd /d "%ROOT%"

if not exist "%ROOT%target\admin-usuarios.jar" (
  echo No se encontro el JAR en %ROOT%target\admin-usuarios.jar
  echo Ejecuta build.ps1 para generarlo.
  exit /b 1
)

set "TRUSTSTORE=%ROOT%backoffice-truststore.jks"
set "TRUSTSTORE_PASS=changeit"

if not exist "%TRUSTSTORE%" (
  echo No se encontro el truststore en %TRUSTSTORE%
  echo Importa el certificado guardiacivil-es.pem con keytool.
  exit /b 1
)

echo ============================================================
echo   INICIANDO BACKOFFICE (PRODUCCION)
echo ============================================================
echo.
echo JAR: %ROOT%target\admin-usuarios.jar
echo Truststore: %TRUSTSTORE%
echo.

java -Djavax.net.ssl.trustStore="%TRUSTSTORE%" ^
     -Djavax.net.ssl.trustStoreType=PKCS12 ^
     -Djavax.net.ssl.trustStorePassword="%TRUSTSTORE_PASS%" ^
     -jar "%ROOT%target\admin-usuarios.jar"

echo.
echo Proceso finalizado. Pulsa una tecla para cerrar.
pause >nul

endlocal
