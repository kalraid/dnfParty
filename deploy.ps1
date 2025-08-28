# DFO Party Management Application Docker Image Build & Push Script
# PowerShell Version

# Parse command line arguments
param(
    [Parameter(Position=0)]
    [ValidateSet("backend", "frontend", "all")]
    [string]$Component = "all"
)

Write-Host "Starting DFO Party Management Application Docker Image Build & Push..." -ForegroundColor Green
Write-Host "Deployment Component: $Component" -ForegroundColor Cyan

# Generate timestamp-based tag (YYYYMMDDHHMM format)
$timestamp = Get-Date -Format "yyyyMMddHHmm"
Write-Host "Generated timestamp tag: $timestamp" -ForegroundColor Cyan

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

# Enable Docker BuildKit for faster builds
$env:DOCKER_BUILDKIT = "1"
$env:COMPOSE_DOCKER_CLI_BUILD = "1"

# Docker image build with optimizations
Write-Host "Building Docker images with BuildKit optimizations..." -ForegroundColor Cyan

# Frontend build with optimizations
if ($Component -eq "all" -or $Component -eq "frontend") {
    Write-Host "Building frontend..." -ForegroundColor Yellow
    Set-Location df-party-frontend

    # Load production environment variables for build
    if (Test-Path "..\config-prod.env") {
        Write-Host "Loading production environment variables for frontend build..." -ForegroundColor Cyan
        Get-Content "..\config-prod.env" | ForEach-Object {
            if ($_ -match "^VITE_([^=]+)=(.*)$") {
                $name = "VITE_" + $matches[1].Trim()
                $value = $matches[2].Trim()
                Write-Host "Setting $name for build: $value" -ForegroundColor Green
                Set-Variable -Name $name -Value $value -Scope Global
                [Environment]::SetEnvironmentVariable($name, $value, "Process")
            }
        }
    } else {
        Write-Host "Warning: config-prod.env not found. Using default localhost values." -ForegroundColor Yellow
        $env:VITE_API_BASE_URL = "http://localhost:8080/api"
        $env:VITE_WS_BASE_URL = "http://localhost:8080"
    }

    # Build with timestamp tag and latest tag
    Write-Host "Building frontend with tags: $timestamp and latest" -ForegroundColor Cyan
    docker build --build-arg BUILDKIT_INLINE_CACHE=1 --build-arg VITE_API_BASE_URL="$env:VITE_API_BASE_URL" --build-arg VITE_WS_BASE_URL="$env:VITE_WS_BASE_URL" --cache-from kimrie92/dfo-party-frontend:latest -t kimrie92/dfo-party-frontend:$timestamp -t kimrie92/dfo-party-frontend:latest .

    if ($LASTEXITCODE -ne 0) {
        Write-Host "Frontend build failed" -ForegroundColor Red
        exit 1
    }
    Set-Location ..
} else {
    Write-Host "Skipping frontend build (Component: $Component)" -ForegroundColor Yellow
}

# Backend build with optimizations
if ($Component -eq "all" -or $Component -eq "backend") {
    Write-Host "Building backend..." -ForegroundColor Yellow
    Set-Location df-party-backend
    
    # Set production profile for backend build
    $env:SPRING_PROFILES_ACTIVE = "production"
    Write-Host "Setting SPRING_PROFILES_ACTIVE=production for backend build" -ForegroundColor Cyan
    
    Write-Host "Building backend with tags: $timestamp and latest" -ForegroundColor Cyan
    docker build --build-arg BUILDKIT_INLINE_CACHE=1 --build-arg SPRING_PROFILES_ACTIVE="production" --cache-from kimrie92/dfo-party-backend:latest -t kimrie92/dfo-party-backend:$timestamp -t kimrie92/dfo-party-backend:latest .
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Backend build failed" -ForegroundColor Red
        exit 1
    }
    Set-Location ..
} else {
    Write-Host "Skipping backend build (Component: $Component)" -ForegroundColor Yellow
}

Write-Host "All image builds completed!" -ForegroundColor Green

# Docker image push
Write-Host "Pushing Docker images..." -ForegroundColor Cyan

if ($Component -eq "all" -or $Component -eq "frontend") {
    Write-Host "Pushing frontend images..." -ForegroundColor Yellow
    # Push timestamp tag first
    docker push kimrie92/dfo-party-frontend:$timestamp
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Frontend timestamp image push failed" -ForegroundColor Red
        exit 1
    }
    # Push latest tag
    docker push kimrie92/dfo-party-frontend:latest
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Frontend latest image push failed" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "Skipping frontend image push (Component: $Component)" -ForegroundColor Yellow
}

if ($Component -eq "all" -or $Component -eq "backend") {
    Write-Host "Pushing backend images..." -ForegroundColor Yellow
    # Push timestamp tag first
    docker push kimrie92/dfo-party-backend:$timestamp
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Backend timestamp image push failed" -ForegroundColor Red
        exit 1
    }
    # Push latest tag
    docker push kimrie92/dfo-party-backend:latest
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Backend latest image push failed" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "Skipping backend image push (Component: $Component)" -ForegroundColor Yellow
}

Write-Host "All image builds and pushes completed!" -ForegroundColor Green

# Clean up old images (keep only last 3 versions)
Write-Host "Cleaning up old Docker images..." -ForegroundColor Cyan
if ($Component -eq "all" -or $Component -eq "frontend") {
    Write-Host "Cleaning up old frontend images..." -ForegroundColor Yellow
    # Get list of frontend images, keep only last 3 (excluding latest and current timestamp)
    $frontendImages = docker images kimrie92/dfo-party-frontend --format "{{.Tag}}" | Where-Object { $_ -ne "latest" -and $_ -ne $timestamp } | Select-Object -Skip 2
    foreach ($tag in $frontendImages) {
        if ($tag -and $tag.Trim() -ne "") {
            Write-Host "Removing old frontend image: kimrie92/dfo-party-frontend:$tag" -ForegroundColor Yellow
            docker rmi kimrie92/dfo-party-frontend:$tag -f 2>$null
        }
    }
}

if ($Component -eq "all" -or $Component -eq "backend") {
    Write-Host "Cleaning up old backend images..." -ForegroundColor Yellow
    # Get list of backend images, keep only last 3 (excluding latest and current timestamp)
    $backendImages = docker images kimrie92/dfo-party-backend --format "{{.Tag}}" | Where-Object { $_ -ne "latest" -and $_ -ne $timestamp } | Select-Object -Skip 2
    foreach ($tag in $backendImages) {
        if ($tag -and $tag.Trim() -ne "") {
            Write-Host "Removing old backend image: kimrie92/dfo-party-backend:$tag" -ForegroundColor Yellow
            docker rmi kimrie92/dfo-party-backend:$tag -f 2>$null
        }
    }
}

Write-Host "Image cleanup completed!" -ForegroundColor Green

# Save timestamp to file for deployment script
$timestamp | Out-File -FilePath "current-timestamp.txt" -Encoding UTF8
Write-Host "Timestamp saved to current-timestamp.txt: $timestamp" -ForegroundColor Green

Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "   - Helm deployment: .\deploy-application.ps1" -ForegroundColor Cyan
Write-Host "   - Or manual Helm deployment: helm upgrade --install dfo-party .\helm-charts --set backend.image.tag=$timestamp --set frontend.image.tag=$timestamp" -ForegroundColor Cyan

# Show usage information
Write-Host ""
Write-Host "Usage Examples:" -ForegroundColor Magenta
Write-Host "   .\deploy.ps1              # Deploy all components (default)" -ForegroundColor White
Write-Host "   .\deploy.ps1 all          # Deploy all components" -ForegroundColor White
Write-Host "   .\deploy.ps1 backend      # Deploy only backend" -ForegroundColor White
Write-Host "   .\deploy.ps1 frontend     # Deploy only frontend" -ForegroundColor White

# Auto-deploy to Kubernetes after successful build and push
Write-Host ""
Write-Host "Starting automatic Kubernetes deployment..." -ForegroundColor Green
Write-Host "Executing deploy-application.ps1 with timestamp: $timestamp" -ForegroundColor Cyan

# Execute deploy-application.ps1 with timestamp
& ".\deploy-application.ps1" -Timestamp $timestamp

if ($LASTEXITCODE -eq 0) {
    Write-Host "Complete deployment pipeline finished successfully!" -ForegroundColor Green
    Write-Host "Deployed with timestamp: $timestamp" -ForegroundColor Green
} else {
    Write-Host "Deployment failed with exit code: $LASTEXITCODE" -ForegroundColor Red
    exit $LASTEXITCODE
}
