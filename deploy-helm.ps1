# DFO Party Application Helm Deployment Script
# PowerShell Version

# 백그라운드 실행을 위한 설정
$Host.UI.RawUI.WindowTitle = "DFO Party Application Deployment - Background"
$ProgressPreference = 'SilentlyContinue'

param(
    [string]$ReleaseName,
    [string]$Namespace,
    [string]$ValuesFile
)

# Set default values if not provided
if (-not $ReleaseName) { $ReleaseName = "df-party-application" }
if (-not $Namespace) { $Namespace = "dfo" }
if (-not $ValuesFile) { $ValuesFile = "helm-charts/values.yaml" }

Write-Host "Starting DFO Party Management Application Helm Deployment in Background..." -ForegroundColor Green
Write-Host "Process ID: $PID" -ForegroundColor Cyan
Write-Host "Timestamp: $(Get-Date)" -ForegroundColor Cyan

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

# Function to cleanup Helm release and ALL resources in dfo namespace
function Remove-HelmReleaseAndAllResources {
    param([string]$ReleaseName, [string]$Namespace)
    
    Write-Host "=== COMPLETE CLEANUP PROCESS ===" -ForegroundColor Red
    Write-Host "Step 1: Force deleting ALL resources in namespace: $Namespace" -ForegroundColor Red
    
    # Force delete ALL resources in dfo namespace
    $resourceTypes = @("all", "pvc", "secret", "configmap", "ingress", "networkpolicy", "role", "rolebinding", "serviceaccount")
    
    foreach ($resourceType in $resourceTypes) {
        Write-Host "Deleting $resourceType resources..." -ForegroundColor Yellow
        kubectl delete $resourceType --all -n $Namespace --ignore-not-found=true --grace-period=0 --force
        Start-Sleep -Seconds 2
    }
    
    # Wait for complete cleanup
    Write-Host "Waiting for complete cleanup to finish..." -ForegroundColor Cyan
    Start-Sleep -Seconds 15
    
    # Verify cleanup
    $remainingResources = kubectl get all -n $Namespace --ignore-not-found=true
    if ($remainingResources) {
        Write-Host "Warning: Some resources may still exist after cleanup." -ForegroundColor Yellow
        Write-Host "Remaining resources:" -ForegroundColor Yellow
        $remainingResources | ForEach-Object { Write-Host "  $_" -ForegroundColor Gray }
    } else {
        Write-Host "Complete cleanup verified. All resources removed." -ForegroundColor Green
    }
    
        return $true
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

# Load environment variables from config.env file
if (Test-Path "config.env") {
    Write-Host "Loading environment variables from config.env..." -ForegroundColor Cyan
    Get-Content "config.env" | ForEach-Object {
        if ($_ -match "^([^#][^=]+)=(.*)$") {
            $name = $matches[1].Trim()
            $value = $matches[2].Trim()
            Set-Variable -Name $name -Value $value -Scope Global
            Write-Host "  $name = $value" -ForegroundColor Gray
        }
    }
    Write-Host "Environment variables loaded from config.env" -ForegroundColor Green
    
    if ($DF_API_KEY) {
        Write-Host "DF_API_KEY loaded: $($DF_API_KEY.Substring(0, [Math]::Min(10, $DF_API_KEY.Length)))..." -ForegroundColor Green
    }
    } else {
    Write-Host "config.env file not found. Using default values." -ForegroundColor Yellow
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

# Search for existing Helm releases
Write-Host "Searching for existing Helm releases..." -ForegroundColor Cyan
$namespacesToCheck = @($Namespace)
$releaseFound = $false
foreach ($ns in $namespacesToCheck) {
    if (Test-HelmReleaseExists -ReleaseName $ReleaseName -Namespace $ns) {
        Write-Host "Found existing release '$ReleaseName' in namespace '$ns'" -ForegroundColor Yellow
        $releaseFound = $true
        
        # 항상 기존 릴리스를 제거 시도 (helm list 확인 없이)
        Write-Host "Force uninstall (without checking helm list)..." -ForegroundColor Cyan
        helm uninstall $ReleaseName -n $Namespace --no-hooks --timeout 5m
        if ($LASTEXITCODE -eq 0) {
            Write-Host "Helm release '$ReleaseName' uninstalled successfully (namespace: $Namespace)." -ForegroundColor Green
        } else {
            Write-Host "Helm uninstall returned non-zero (may be already absent). Proceeding..." -ForegroundColor Yellow
        }

        # stuck 상태 방지를 위해 Helm 릴리스 시크릿 정리
        Write-Host "Cleaning any Helm-owned secrets for '$ReleaseName' in namespace '$Namespace'..." -ForegroundColor Cyan
        $helmSecrets = kubectl get secret -n $Namespace -l "owner=helm,name=$ReleaseName" -o name 2>$null
        if ($helmSecrets) {
            $helmSecrets | ForEach-Object { kubectl delete $_ -n $Namespace --ignore-not-found=true }
} else {
            Write-Host "No Helm-owned secrets found (owner=helm,name=$ReleaseName)." -ForegroundColor Gray
        }

        # 네임스페이스 자원 강제 삭제
        Write-Host "Force deleting ALL resources in namespace '$Namespace'..." -ForegroundColor Red
        Remove-HelmReleaseAndAllResources -ReleaseName $ReleaseName -Namespace $Namespace | Out-Null
    }
}

if (-not $releaseFound) {
    Write-Host "No existing Helm releases found for '$ReleaseName' in namespace '$Namespace'." -ForegroundColor Green
}

# Final verification of namespace cleanup
Write-Host "Final verification of namespace cleanup..." -ForegroundColor Cyan
$finalCheck = kubectl get all -n $Namespace --ignore-not-found=true
if ($finalCheck) {
    Write-Host "Warning: Some resources still exist. Attempting final cleanup..." -ForegroundColor Red
    kubectl delete all --all -n $Namespace --ignore-not-found=true --grace-period=0 --force
    Start-Sleep -Seconds 10
} else {
    Write-Host "Namespace is clean. Ready for deployment." -ForegroundColor Green
}

# Deploy/upgrade Helm release
Write-Host "Deploying/upgrading Helm release..." -ForegroundColor Green
Write-Host "   Release name: $ReleaseName" -ForegroundColor Cyan
Write-Host "   Namespace: $Namespace" -ForegroundColor Cyan

# Install fresh Helm release (pull latest images)
Write-Host "Installing fresh Helm release..." -ForegroundColor Green
helm install $ReleaseName ./helm-charts `
    --namespace $Namespace `
    --values $ValuesFile `
    --set frontend.image.pullPolicy=Always `
    --set backend.image.pullPolicy=Always `
    --timeout 10m

if ($LASTEXITCODE -eq 0) {
    Write-Host "Helm installation completed successfully!" -ForegroundColor Green
    Write-Host "Waiting for deployment to stabilize..." -ForegroundColor Cyan
    Start-Sleep -Seconds 30
    Write-Host "Checking deployment status..." -ForegroundColor Cyan
    kubectl get pods -n $Namespace
    Write-Host "Deployment completed successfully!" -ForegroundColor Green
} else {
    Write-Host "Helm installation failed" -ForegroundColor Red
    Write-Host "Error details:" -ForegroundColor Red
    exit 1
}
