# ============================================================
# Script de compilación y empaquetado completo
# Backoffice: Spring Boot + Angular
# ============================================================

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  COMPILACION COMPLETA - BACKOFFICE PROJECT" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

# ============================================================
# 1. COMPILAR FRONTEND ANGULAR
# ============================================================
Write-Host "[1/4] Compilando frontend Angular..." -ForegroundColor Yellow
Set-Location frontend

$buildResult = npm run build 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Fallo la compilacion del frontend" -ForegroundColor Red
    Set-Location ..
    exit 1
}

Set-Location ..
Write-Host "✓ Frontend compilado correctamente" -ForegroundColor Green
Write-Host ""

# ============================================================
# 2. COPIAR FRONTEND A RESOURCES/STATIC
# ============================================================
Write-Host "[2/4] Copiando frontend a resources/static..." -ForegroundColor Yellow

# Crear directorio static si no existe
New-Item -ItemType Directory -Force -Path ".\src\main\resources\static" | Out-Null

# Limpiar static anterior
Remove-Item -Recurse -Force ".\src\main\resources\static\*" -ErrorAction SilentlyContinue

# Copiar nuevo frontend
Copy-Item -Recurse -Force ".\frontend\dist\frontend\browser\*" ".\src\main\resources\static\"

Write-Host "✓ Frontend copiado a resources/static" -ForegroundColor Green
Write-Host ""

# ============================================================
# 3. COMPILAR JAR CON MAVEN
# ============================================================
Write-Host "[3/4] Compilando JAR con Maven..." -ForegroundColor Yellow

$mavenResult = mvn clean package -DskipTests 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Fallo la compilacion de Maven" -ForegroundColor Red
    exit 1
}

Write-Host "✓ JAR compilado correctamente" -ForegroundColor Green
Write-Host ""

# ============================================================
# 4. COMPRIMIR JAR PARA DEPLOYMENT
# ============================================================
Write-Host "[4/4] Comprimiendo JAR para transferencia..." -ForegroundColor Yellow

$zipPath = "$env:USERPROFILE\Desktop\admin-usuarios.zip"
Compress-Archive -Force -Path ".\target\admin-usuarios.jar" -DestinationPath $zipPath

$fileSize = (Get-Item $zipPath).Length / 1MB
Write-Host "✓ JAR comprimido: $zipPath" -ForegroundColor Green
Write-Host "  Tamaño: $([math]::Round($fileSize, 2)) MB" -ForegroundColor Gray
Write-Host ""

# ============================================================
# RESUMEN FINAL
# ============================================================
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  COMPILACION COMPLETADA EXITOSAMENTE" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Archivo generado:" -ForegroundColor White
Write-Host "  → $zipPath" -ForegroundColor Cyan
Write-Host ""
Write-Host "Para desplegar en la VM:" -ForegroundColor White
Write-Host "  1. Transferir: admin-usuarios.zip" -ForegroundColor Gray
Write-Host "  2. Descomprimir en la VM" -ForegroundColor Gray
Write-Host "  3. Ejecutar: java -jar admin-usuarios.jar" -ForegroundColor Gray
Write-Host "  4. Acceder: http://localhost:8085" -ForegroundColor Gray
Write-Host ""
