# DFO Party Application Helm Deployment Script
# PowerShell Version

# 백그라운드 실행을 위한 설정
$Host.UI.RawUI.WindowTitle = "DFO Party Application Deployment - Background"
$ProgressPreference = 'SilentlyContinue'

param(
    [string]$ReleaseName,
    [string]$Namespace,
    [string]$ValuesFile,
    [string]$Timestamp
)

# Set default values if not provided
if (-not $ReleaseName) { $ReleaseName = "df-party-application" }
if (-not $Namespace) { $Namespace = "dfo" }
if (-not $ValuesFile) { $ValuesFile = "helm-charts/values.yaml" }

# Load timestamp from file if not provided
if (-not $Timestamp) {
    if (Test-Path "current-timestamp.txt") {
        $Timestamp = Get-Content "current-timestamp.txt" -Raw
        $Timestamp = $Timestamp.Trim()
        Write-Host "Loaded timestamp from file: $Timestamp" -ForegroundColor Cyan
    } else {
        Write-Host "Warning: No timestamp provided and current-timestamp.txt not found. Using 'latest' tag." -ForegroundColor Yellow
        $Timestamp = "latest"
    }
}

Write-Host "Starting DFO Party Management Application Helm Deployment in Background..." -ForegroundColor Green
Write-Host "Process ID: $PID" -ForegroundColor Cyan
Write-Host "Timestamp: $(Get-Date)" -ForegroundColor Cyan
Write-Host "Image Tag: $Timestamp" -ForegroundColor Cyan

# Function to find existing Helm releases across multiple namespaces
function Find-ExistingHelmReleases {
    param([string]$ReleaseName)
    
    Write-Host "Searching for existing Helm releases with name: $ReleaseName" -ForegroundColor Cyan
    
    $namespaces = @("dfo", "df", "default")
    $foundReleases = @()
    
    foreach ($ns in $namespaces) {
        Write-Host "Checking namespace: $ns" -ForegroundColor Yellow
        try {
            $releases = helm list -n $ns --filter "^$ReleaseName$" --output json | ConvertFrom-Json
            if ($releases) {
                foreach ($release in $releases) {
                    $foundReleases += [PSCustomObject]@{
                        Name = $release.name
                        Namespace = $ns
                        Status = $release.status
                        Chart = $release.chart
                        AppVersion = $release.appVersion
                    }
                }
            }
        } catch {
            Write-Host "No releases found in namespace: $ns" -ForegroundColor Gray
        }
    }
    
    return $foundReleases
}



# Helpers: Check if a Helm release exists reliably
function Test-HelmReleaseExists {
    param([string]$ReleaseName, [string]$Namespace)
    $exists = $false
    try {
        helm status $ReleaseName -n $Namespace | Out-Null
        if ($LASTEXITCODE -eq 0) { $exists = $true }
    } catch { $exists = $false }
    if (-not $exists) {
        # Fallback: parse helm list output defensively
        $out = helm list -n $Namespace --short 2>$null
        if ($out -and ($out -split "\r?\n") -contains $ReleaseName) { $exists = $true }
    }
    return $exists
}

# Create target namespace if it doesn't exist
Write-Host "Creating $Namespace namespace if it doesn't exist..." -ForegroundColor Cyan
kubectl create namespace $Namespace --dry-run=client -o yaml | kubectl apply -f -
Write-Host "$Namespace namespace ready." -ForegroundColor Green

# Load environment variables from config-prod.env file
if (Test-Path "config-prod.env") {
    Write-Host "Loading environment variables from config-prod.env..." -ForegroundColor Cyan
    Get-Content "config-prod.env" | ForEach-Object {
        if ($_ -match "^([^#][^=]+)=(.*)$") {
            $name = $matches[1].Trim()
            $value = $matches[2].Trim()
            Set-Variable -Name $name -Value $value -Scope Global
            Write-Host "  $name = $value" -ForegroundColor Gray
        }
    }
    Write-Host "Environment variables loaded from config-prod.env" -ForegroundColor Green
    
    if ($DF_API_KEY) {
        Write-Host "DF_API_KEY loaded: $($DF_API_KEY.Substring(0, [Math]::Min(10, $DF_API_KEY.Length)))..." -ForegroundColor Green
    }
    } else {
    Write-Host "config-prod.env file not found. Using default values." -ForegroundColor Yellow
}

# Check Helm version
Write-Host "Helm version: $(helm version --short)" -ForegroundColor Cyan

# Check Kubernetes context
try {
    $context = kubectl config current-context
    Write-Host "Kubernetes context: $context" -ForegroundColor Cyan
} catch {
    Write-Host "Warning: Could not determine Kubernetes context" -ForegroundColor Yellow
}

# Check if application release exists
Write-Host "Checking for existing application release..." -ForegroundColor Cyan
$releaseExists = Test-HelmReleaseExists -ReleaseName $ReleaseName -Namespace $Namespace

if ($releaseExists) {
    Write-Host "Found existing application release: $ReleaseName" -ForegroundColor Yellow
    Write-Host "Proceeding with upgrade..." -ForegroundColor Green
} else {
    Write-Host "No existing application release found. Installing new release..." -ForegroundColor Green
}
# Deploy/upgrade Helm release
Write-Host "Deploying/upgrading Helm release..." -ForegroundColor Green
Write-Host "   Release name: $ReleaseName" -ForegroundColor Cyan
Write-Host "   Namespace: $Namespace" -ForegroundColor Cyan

# Clean up any existing .tgz files and rebuild dependencies
Write-Host "Cleaning up .tgz files and rebuilding dependencies..." -ForegroundColor Cyan
Set-Location helm-charts

# Remove any existing .tgz files
if (Test-Path "charts") {
    Write-Host "Removing existing charts directory..." -ForegroundColor Yellow
    Remove-Item -Recurse -Force "charts" -ErrorAction SilentlyContinue
    Write-Host "Charts directory removed" -ForegroundColor Green
}

# Remove Chart.lock if it exists
if (Test-Path "Chart.lock") {
    Write-Host "Removing Chart.lock..." -ForegroundColor Yellow
    Remove-Item -Force "Chart.lock" -ErrorAction SilentlyContinue
    Write-Host "Chart.lock removed" -ForegroundColor Green
}

# Build dependencies from local subcharts (not from .tgz)
Write-Host "Building dependencies from local subcharts..." -ForegroundColor Cyan
helm dependency build --skip-refresh
if ($LASTEXITCODE -ne 0) {
    Write-Host "Helm dependency build failed" -ForegroundColor Red
    exit 1
}
Write-Host "Dependencies built from local subcharts successfully" -ForegroundColor Green

Set-Location ..
Write-Host "Local charts ready for deployment" -ForegroundColor Green

# Deploy using helm upgrade --install
Write-Host "Deploying using helm upgrade --install..." -ForegroundColor Green

# Set frontend environment variables from config-prod.env
$frontendEnvSets = @()
if ($VITE_API_BASE_URL) {
    $frontendEnvSets += "--set frontend.env.VITE_API_BASE_URL=`"$VITE_API_BASE_URL`""
    Write-Host "Setting VITE_API_BASE_URL: $VITE_API_BASE_URL" -ForegroundColor Cyan
}
if ($VITE_WS_BASE_URL) {
    $frontendEnvSets += "--set frontend.env.VITE_WS_BASE_URL=`"$VITE_WS_BASE_URL`""
    Write-Host "Setting VITE_WS_BASE_URL: $VITE_WS_BASE_URL" -ForegroundColor Cyan
}

# Build helm command with environment variables
# Use local chart directory directly
$helmCommand = "helm upgrade --install $ReleaseName ./helm-charts --namespace $Namespace --values $ValuesFile --set frontend.image.tag=`"$Timestamp`" --set backend.image.tag=`"$Timestamp`" --set frontend.image.pullPolicy=Always --set backend.image.pullPolicy=Always --set backend.dfoApiKey=`"$DF_API_KEY`" --timeout 10m"
if ($frontendEnvSets.Count -gt 0) {
    $helmCommand += " " + ($frontendEnvSets -join " ")
}

Write-Host "Executing: $helmCommand" -ForegroundColor Yellow
Invoke-Expression $helmCommand

if ($LASTEXITCODE -eq 0) {
    Write-Host "Helm deployment completed successfully!" -ForegroundColor Green
    Write-Host "Waiting for deployment to stabilize..." -ForegroundColor Cyan
    Start-Sleep -Seconds 30
    Write-Host "Checking deployment status..." -ForegroundColor Cyan
    kubectl get pods -n $Namespace
    Write-Host "Deployment completed successfully!" -ForegroundColor Green
} else {
    Write-Host "Helm deployment failed" -ForegroundColor Red
    Write-Host "Error details:" -ForegroundColor Red
    exit 1
}

# Force rollout restart for both frontend and backend to ensure new images are used
Write-Host ""
Write-Host "Forcing rollout restart to ensure new images are used..." -ForegroundColor Cyan

# Restart frontend deployment
Write-Host "Restarting frontend deployment..." -ForegroundColor Yellow
kubectl rollout restart deployment df-party-application-frontend -n $Namespace
if ($LASTEXITCODE -eq 0) {
    Write-Host "Frontend rollout restart initiated successfully" -ForegroundColor Green
} else {
    Write-Host "Frontend rollout restart failed" -ForegroundColor Red
}

# Restart backend deployment
Write-Host "Restarting backend deployment..." -ForegroundColor Yellow
kubectl rollout restart deployment df-party-application-backend -n $Namespace
if ($LASTEXITCODE -eq 0) {
    Write-Host "Backend rollout restart initiated successfully" -ForegroundColor Green
} else {
    Write-Host "Backend rollout restart failed" -ForegroundColor Red
}

# Wait for rollouts to complete
Write-Host ""
Write-Host "Waiting for rollouts to complete..." -ForegroundColor Cyan

# Wait for frontend rollout
Write-Host "Waiting for frontend rollout..." -ForegroundColor Yellow
kubectl rollout status deployment df-party-application-frontend -n $Namespace --timeout=300s
if ($LASTEXITCODE -eq 0) {
    Write-Host "Frontend rollout completed successfully" -ForegroundColor Green
} else {
    Write-Host "Frontend rollout failed or timed out" -ForegroundColor Red
}

# Wait for backend rollout
Write-Host "Waiting for backend rollout..." -ForegroundColor Yellow
kubectl rollout status deployment df-party-application-backend -n $Namespace --timeout=300s
if ($LASTEXITCODE -eq 0) {
    Write-Host "Backend rollout completed successfully" -ForegroundColor Green
} else {
    Write-Host "Backend rollout failed or timed out" -ForegroundColor Red
}

# Final status check
Write-Host ""
Write-Host "Final deployment status:" -ForegroundColor Cyan
kubectl get pods -n $Namespace
Write-Host ""
Write-Host "All deployments and rollouts completed!" -ForegroundColor Green
