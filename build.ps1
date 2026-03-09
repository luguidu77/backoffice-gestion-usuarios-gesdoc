# ============================================================
# Script de compilación rápida (sin comprimir)
# Solo compila y prepara el JAR
# ============================================================

Write-Host "Compilando frontend..." -ForegroundColor Yellow
Set-Location frontend
npm run build
Set-Location ..

Write-Host "Copiando frontend a static..." -ForegroundColor Yellow
New-Item -ItemType Directory -Force -Path ".\src\main\resources\static" | Out-Null
Remove-Item -Recurse -Force ".\src\main\resources\static\*" -ErrorAction SilentlyContinue
Copy-Item -Recurse -Force ".\frontend\dist\frontend\browser\*" ".\src\main\resources\static\"

Write-Host "Compilando JAR..." -ForegroundColor Yellow
mvn clean package -DskipTests

Write-Host ""
Write-Host "✓ Compilacion completa" -ForegroundColor Green
Write-Host "JAR generado: .\target\admin-usuarios.jar" -ForegroundColor Cyan
Write-Host ""
Write-Host "Para ejecutar:" -ForegroundColor White
Write-Host "  java -jar target\admin-usuarios.jar" -ForegroundColor Gray
Write-Host "  O: mvn spring-boot:run" -ForegroundColor Gray
