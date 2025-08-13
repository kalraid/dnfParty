# DFO Party Management Application Docker Image Build & Push Script
# PowerShell Version

Write-Host "Starting DFO Party Management Application Docker Image Build & Push..." -ForegroundColor Green

# Create dfo namespace if it doesn't exist
Write-Host "Creating dfo namespace if it doesn't exist..." -ForegroundColor Cyan
kubectl create namespace dfo --dry-run=client -o yaml | kubectl apply -f -
Write-Host "dfo namespace ready." -ForegroundColor Green

# Load environment variables from config.env file
if (Test-Path "config.env") {
    Write-Host "Loading environment variables from config.env..." -ForegroundColor Cyan
    Get-Content "config.env" | ForEach-Object {
        if ($_ -match "^([^#][^=]+)=(.*)$") {
            $name = $matches[1].Trim()
            $value = $matches[2].Trim()
            Set-Variable -Name $name -Value $value -Scope Global
            [Environment]::SetEnvironmentVariable($name, $value, "Process")
        }
    }
    Write-Host "Environment variables loaded from config.env" -ForegroundColor Green
} else {
    Write-Host "Warning: config.env file not found." -ForegroundColor Yellow
    Write-Host "   Create config.env file with DF_API_KEY=your_api_key_here" -ForegroundColor Yellow
    exit 1
}

# Check if DF_API_KEY is loaded
if (-not $env:DF_API_KEY) {
    Write-Host "Warning: DF_API_KEY not found in config.env or environment." -ForegroundColor Yellow
    Write-Host "   Add DF_API_KEY=your_api_key_here to config.env file" -ForegroundColor Yellow
    exit 1
}

Write-Host "DF_API_KEY loaded: $($env:DF_API_KEY.Substring(0, [Math]::Min(8, $env:DF_API_KEY.Length)))..." -ForegroundColor Green

# Docker image build
Write-Host "Building Docker images..." -ForegroundColor Cyan

# Frontend build
Write-Host "Building frontend..." -ForegroundColor Yellow
Set-Location df-party-frontend
docker build -t kimrie92/dfo-party-frontend:latest .
if ($LASTEXITCODE -ne 0) {
    Write-Host "Frontend build failed" -ForegroundColor Red
    exit 1
}
Set-Location ..

# Backend build
Write-Host "Building backend..." -ForegroundColor Yellow
Set-Location df-party-backend
docker build -t kimrie92/dfo-party-backend:latest .
if ($LASTEXITCODE -ne 0) {
    Write-Host "Backend build failed" -ForegroundColor Red
    exit 1
}
Set-Location ..

# Mock backend build
Write-Host "Building mock backend..." -ForegroundColor Yellow
Set-Location df-party-mock
./gradlew build -x test
if ($LASTEXITCODE -ne 0) {
    Write-Host "Mock backend Gradle build failed" -ForegroundColor Red
    exit 1
}
docker build -t kimrie92/dfo-party-mock:latest .
if ($LASTEXITCODE -ne 0) {
    Write-Host "Mock backend Docker build failed" -ForegroundColor Red
    exit 1
}
Set-Location ..

Write-Host "All image builds completed!" -ForegroundColor Green

# Docker image push
Write-Host "Pushing Docker images..." -ForegroundColor Cyan

Write-Host "Pushing frontend image..." -ForegroundColor Yellow
docker push kimrie92/dfo-party-frontend:latest
if ($LASTEXITCODE -ne 0) {
    Write-Host "Frontend image push failed" -ForegroundColor Red
    exit 1
}

Write-Host "Pushing backend image..." -ForegroundColor Yellow
docker push kimrie92/dfo-party-backend:latest
if ($LASTEXITCODE -ne 0) {
    Write-Host "Backend image push failed" -ForegroundColor Red
    exit 1
}

Write-Host "Pushing mock backend image..." -ForegroundColor Yellow
docker push kimrie92/dfo-party-mock:latest
if ($LASTEXITCODE -ne 0) {
    Write-Host "Mock backend image push failed" -ForegroundColor Red
    exit 1
}

Write-Host "All image builds and pushes completed!" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "   - Helm deployment: .\deploy-helm.ps1" -ForegroundColor Cyan
Write-Host "   - Or manual Helm deployment: helm upgrade --install dfo-party .\helm-charts" -ForegroundColor Cyan
