# Docker Cleanup & Maintenance Script for Windows
# Removes dangling volumes, images, and containers

Write-Host "🧹 Starting Docker Cleanup..." -ForegroundColor Green
Write-Host ""

# Helper functions
function Print-Status {
    param([string]$message)
    Write-Host "✓ $message" -ForegroundColor Green
}

function Print-Warning {
    param([string]$message)
    Write-Host "⚠ $message" -ForegroundColor Yellow
}

function Print-Error {
    param([string]$message)
    Write-Host "✗ $message" -ForegroundColor Red
}

# 1. Stop all running containers
Write-Host "Stopping containers..." -ForegroundColor Yellow
if (Test-Path "docker-compose.yml") {
    try {
        docker-compose down -v 2> $null
        Print-Status "docker-compose services stopped"
    } catch {
        Print-Warning "docker-compose down encountered an issue"
    }
} else {
    Print-Warning "docker-compose.yml not found in current directory"
}

Write-Host ""

# 2. Remove dangling volumes
Write-Host "Cleaning up dangling volumes..." -ForegroundColor Yellow
$danglingVolumes = docker volume ls -q -f "dangling=true"
if ([string]::IsNullOrEmpty($danglingVolumes)) {
    Print-Status "No dangling volumes to remove"
} else {
    $volumeCount = ($danglingVolumes | Measure-Object -Line).Lines
    $danglingVolumes | ForEach-Object { docker volume rm $_ -f }
    Print-Status "Removed $volumeCount dangling volumes"
}

Write-Host ""

# 3. Remove dangling images
Write-Host "Cleaning up dangling images..." -ForegroundColor Yellow
$danglingImages = docker images -q -f "dangling=true"
if ([string]::IsNullOrEmpty($danglingImages)) {
    Print-Status "No dangling images to remove"
} else {
    $imageCount = ($danglingImages | Measure-Object -Line).Lines
    $danglingImages | ForEach-Object { docker rmi -f $_ }
    Print-Status "Removed $imageCount dangling images"
}

Write-Host ""

# 4. Remove exited containers
Write-Host "Cleaning up exited containers..." -ForegroundColor Yellow
$exitedContainers = docker ps -q -f "status=exited"
if ([string]::IsNullOrEmpty($exitedContainers)) {
    Print-Status "No exited containers to remove"
} else {
    $containerCount = ($exitedContainers | Measure-Object -Line).Lines
    $exitedContainers | ForEach-Object { docker rm $_ }
    Print-Status "Removed $containerCount exited containers"
}

Write-Host ""

# 5. Prune unused networks
Write-Host "Pruning unused networks..." -ForegroundColor Yellow
docker network prune -f | Out-Null
Print-Status "Network cleanup complete"

Write-Host ""

# 6. Show volume summary
Write-Host "Current Docker volumes:" -ForegroundColor Yellow
docker volume ls

Write-Host ""

# 7. Show storage usage
Write-Host "Docker disk usage:" -ForegroundColor Yellow
docker system df

Write-Host ""
Write-Host "✅ Docker cleanup completed!" -ForegroundColor Green

Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. Run: docker-compose up -d" -ForegroundColor Cyan
Write-Host "  2. Verify: docker volume ls" -ForegroundColor Cyan
Write-Host "  3. Check logs: docker-compose logs -f" -ForegroundColor Cyan

