# VolunteerPulse PowerShell Launcher
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  VOLUNTEERPULSE PLATFORM - LAUNCHING WEB APPLICATION" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Check MySQL
$mysql = Get-NetTCPConnection -LocalPort 3306 -State Listen -ErrorAction SilentlyContinue
if ($mysql) {
    Write-Host "[OK] MySQL is active on port 3306." -ForegroundColor Green
} else {
    Write-Host "[WARN] MySQL is not detected on port 3306. Please ensure MySQL is running." -ForegroundColor Yellow
}

# 2. Check and free port 8080 if needed
$oldPort = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
if ($oldPort) {
    $procId = $oldPort.OwningProcess
    Write-Host "[INFO] Freeing port 8080 (Process ID: $procId)..." -ForegroundColor Yellow
    Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 1
}

# 3. Start web platform
Write-Host "[INFO] Starting web application at: http://localhost:8080" -ForegroundColor Green
Write-Host "[INFO] Press Ctrl+C in terminal to stop." -ForegroundColor White

mvn exec:java
