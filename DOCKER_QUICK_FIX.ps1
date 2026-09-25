#!/usr/bin/env pwsh
# Quick Docker Volume Fix Script for Windows

Write-Host "╔════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║     Docker Volume Cleanup & Optimization Script        ║" -ForegroundColor Cyan
Write-Host "║                    For Your Project                    ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# Check if running as admin
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole] "Administrator")
if (-not $isAdmin) {
    Write-Host "⚠️  This script should ideally be run as Administrator for best results." -ForegroundColor Yellow
    Write-Host ""
}

Write-Host "📊 Current Status:" -ForegroundColor Cyan
Write-Host "─────────────────────────────────────────────────────────" -ForegroundColor Gray

# Count volumes
$totalVolumes = (docker volume ls -q | Measure-Object -Line).Lines
$danglingVolumes = (docker volume ls -q -f "dangling=true" | Measure-Object -Line).Lines

Write-Host "  Total Volumes: $totalVolumes" -ForegroundColor Yellow
Write-Host "  Dangling Volumes: $danglingVolumes" -ForegroundColor Red
Write-Host ""

# Show running containers
$runningContainers = (docker ps -q | Measure-Object -Line).Lines
Write-Host "  Running Containers: $runningContainers" -ForegroundColor Green
Write-Host ""

Write-Host "🛠️  Steps to Fix:" -ForegroundColor Cyan
Write-Host "─────────────────────────────────────────────────────────" -ForegroundColor Gray
Write-Host ""

Write-Host "STEP 1️⃣  Create data directory (one-time setup)"
Write-Host "  Run: mkdir -p ./data/postgres" -ForegroundColor Cyan
Write-Host ""

Write-Host "STEP 2️⃣  Clean up orphaned volumes"
Write-Host "  Run: .\docker-cleanup.ps1" -ForegroundColor Cyan
Write-Host ""

Write-Host "STEP 3️⃣  Restart services with new configuration"
Write-Host "  Run: docker-compose up -d" -ForegroundColor Cyan
Write-Host ""

Write-Host "STEP 4️⃣  Verify setup is working"
Write-Host "  Run: docker volume ls" -ForegroundColor Cyan
Write-Host "  Run: docker-compose logs" -ForegroundColor Cyan
Write-Host ""

Write-Host "✅ What Changed:" -ForegroundColor Cyan
Write-Host "─────────────────────────────────────────────────────────" -ForegroundColor Gray
Write-Host "  ✓ Updated docker-compose.yml with explicit volume config" -ForegroundColor Green
Write-Host "  ✓ Updated docker-compose_local.yml with explicit volume config" -ForegroundColor Green
Write-Host "  ✓ Data now stored in ./data/postgres (predictable location)" -ForegroundColor Green
Write-Host "  ✓ No more anonymous/orphaned volumes created" -ForegroundColor Green
Write-Host ""

Write-Host "📁 File Manifest:" -ForegroundColor Cyan
Write-Host "─���───────────────────────────────────────────────────────" -ForegroundColor Gray
Write-Host "  📄 docker-compose.yml (UPDATED)" -ForegroundColor Yellow
Write-Host "  📄 docker-compose_local.yml (UPDATED)" -ForegroundColor Yellow
Write-Host "  📄 docker-cleanup.ps1 (NEW) - Windows cleanup script" -ForegroundColor Yellow
Write-Host "  📄 docker-cleanup.sh (NEW) - Linux/macOS cleanup script" -ForegroundColor Yellow
Write-Host "  📄 DOCKER_VOLUME_MANAGEMENT.md (NEW) - Complete guide" -ForegroundColor Yellow
Write-Host ""

Write-Host "💡 Pro Tips:" -ForegroundColor Cyan
Write-Host "─────────────────────────────────────────────────────────" -ForegroundColor Gray
Write-Host "  • Always use 'docker-compose down -v' to cleanup properly" -ForegroundColor Magenta
Write-Host "  • Run docker-cleanup.ps1 monthly to maintain system health" -ForegroundColor Magenta
Write-Host "  • Your data is now persistent in ./data/postgres" -ForegroundColor Magenta
Write-Host "  • Easy to backup: zip ./data/postgres" -ForegroundColor Magenta
Write-Host ""

Write-Host "🆘 Need Help?" -ForegroundColor Cyan
Write-Host "─────────────────────────────────────────────────────────" -ForegroundColor Gray
Write-Host "  Read: DOCKER_VOLUME_MANAGEMENT.md" -ForegroundColor Magenta
Write-Host "  Questions? Check the Troubleshooting section" -ForegroundColor Magenta
Write-Host ""

Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Green
Write-Host "Ready? Follow the 4 steps above! 🚀" -ForegroundColor Green
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Green

