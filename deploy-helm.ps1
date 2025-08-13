# DFO Party Management Application Helm Deployment Script
# PowerShell Version

param(
    [string]$ReleaseName = "dfo-party",
    [string]$Namespace = "dfo",
    [string]$ValuesFile = "helm-charts/values.yaml"
)

Write-Host "Starting DFO Party Management Application Helm Deployment..." -ForegroundColor Green

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

# Check Helm installation
try {
    $helmVersion = helm version --short
    Write-Host "Helm version: $helmVersion" -ForegroundColor Green
} catch {
    Write-Host "Helm is not installed." -ForegroundColor Red
    Write-Host "   Install from: https://helm.sh/docs/intro/install/" -ForegroundColor Yellow
    exit 1
}

# Check kubectl connection
try {
    $context = kubectl config current-context
    Write-Host "Kubernetes context: $context" -ForegroundColor Green
} catch {
    Write-Host "kubectl connection failed." -ForegroundColor Red
    Write-Host "   Check cluster connection." -ForegroundColor Yellow
    exit 1
}

# Function to force cleanup all resources
function Remove-AllResources {
    param([string]$Namespace)
    
    Write-Host "Force cleaning up all resources in namespace: $Namespace" -ForegroundColor Red
    
    # Delete all Kubernetes resources
    $resourceTypes = @("all", "pvc", "secret", "configmap", "ingress", "networkpolicy", "role", "rolebinding", "serviceaccount")
    
    foreach ($resourceType in $resourceTypes) {
        Write-Host "Deleting $resourceType resources..." -ForegroundColor Yellow
        kubectl delete $resourceType --all -n $Namespace --ignore-not-found=true --grace-period=0 --force
        Start-Sleep -Seconds 2
    }
    
    # Wait for cleanup to complete
    Write-Host "Waiting for cleanup to complete..." -ForegroundColor Cyan
    Start-Sleep -Seconds 10
    
    # Verify cleanup
    $remainingResources = kubectl get all -n $Namespace --ignore-not-found=true
    if ($remainingResources) {
        Write-Host "Warning: Some resources may still exist after cleanup." -ForegroundColor Yellow
        return $false
    } else {
        Write-Host "Cleanup verified. All resources removed." -ForegroundColor Green
        return $true
    }
}

# Check and clean up existing Helm release
Write-Host "Checking for existing Helm release..." -ForegroundColor Cyan
$existingRelease = helm list -n $Namespace --filter "^$ReleaseName$" --output json | ConvertFrom-Json
if ($existingRelease) {
    Write-Host "Found existing Helm release: $($existingRelease.name) (Status: $($existingRelease.status))" -ForegroundColor Yellow
    Write-Host "Removing existing release..." -ForegroundColor Cyan
    
    # Try to uninstall existing release
    Write-Host "Attempting Helm uninstall..." -ForegroundColor Cyan
    helm uninstall $ReleaseName -n $Namespace --no-hooks --timeout 5m
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Existing release removed successfully via Helm." -ForegroundColor Green
    } else {
        Write-Host "Helm uninstall failed. Attempting force cleanup..." -ForegroundColor Red
    }
    
    # Always perform force cleanup to ensure complete removal
    Write-Host "Performing force cleanup..." -ForegroundColor Cyan
    $cleanupSuccess = Remove-AllResources -Namespace $Namespace
    
    if (-not $cleanupSuccess) {
        Write-Host "Warning: Cleanup may not be complete. Proceeding with deployment..." -ForegroundColor Yellow
    }
    
    # Additional wait time for cleanup
    Write-Host "Additional wait time for cleanup..." -ForegroundColor Cyan
    Start-Sleep -Seconds 15
    
} else {
    Write-Host "No existing Helm release found." -ForegroundColor Green
}

# Final verification - check for any remaining resources
Write-Host "Final verification of namespace cleanup..." -ForegroundColor Cyan
$finalCheck = kubectl get all -n $Namespace --ignore-not-found=true
if ($finalCheck) {
    Write-Host "Warning: Found remaining resources. Attempting final cleanup..." -ForegroundColor Yellow
    Remove-AllResources -Namespace $Namespace
} else {
    Write-Host "Namespace is clean. Ready for deployment." -ForegroundColor Green
}

# Update Helm chart dependencies (local charts only)
Write-Host "Building Helm chart dependencies..." -ForegroundColor Cyan
Set-Location helm-charts
# Only build local chart dependencies, skip external repositories
helm dependency build --skip-refresh
if ($LASTEXITCODE -ne 0) {
    Write-Host "Helm dependency build failed" -ForegroundColor Red
    exit 1
}
Set-Location ..

# Deploy/upgrade Helm release
Write-Host "Deploying/upgrading Helm release..." -ForegroundColor Cyan
Write-Host "   Release name: $ReleaseName" -ForegroundColor White
Write-Host "   Namespace: $Namespace" -ForegroundColor White

# Fresh install after cleanup
Write-Host "Installing fresh Helm release..." -ForegroundColor Cyan
helm install $ReleaseName ./helm-charts `
    --namespace $Namespace `
    --values $ValuesFile `
    --set frontend.image.repository=kimrie92/dfo-party-frontend `
    --set backend.image.repository=kimrie92/dfo-party-backend `
    --wait `
    --timeout 10m

if ($LASTEXITCODE -ne 0) {
    Write-Host "Helm installation failed" -ForegroundColor Red
    Write-Host "Error details:" -ForegroundColor Red
    helm install $ReleaseName ./helm-charts `
        --namespace $Namespace `
        --values $ValuesFile `
        --set frontend.image.repository=kimrie92/dfo-party-frontend `
        --set backend.image.repository=kimrie92/dfo-party-backend `
        --dry-run --debug
    exit 1
}

Write-Host "Helm installation completed successfully!" -ForegroundColor Green

# Check deployment status
Write-Host "Checking deployment status..." -ForegroundColor Cyan
kubectl get pods -n $Namespace -l app.kubernetes.io/instance=$ReleaseName

Write-Host "Deployment completed!" -ForegroundColor Green
Write-Host ""
Write-Host "Access information:" -ForegroundColor Cyan
Write-Host "   - Cluster internal: kubectl port-forward -n $Namespace svc/$ReleaseName-frontend 3000:80" -ForegroundColor White
Write-Host "   - NodePort service: kubectl get svc -n $Namespace" -ForegroundColor White
Write-Host ""
Write-Host "Management commands:" -ForegroundColor Cyan
Write-Host "   - Status check: kubectl get all -n $Namespace -l app.kubernetes.io/instance=$ReleaseName" -ForegroundColor White
Write-Host "   - Logs: kubectl logs -n $Namespace -l app.kubernetes.io/instance=$ReleaseName" -ForegroundColor White
Write-Host "   - Delete: helm uninstall $ReleaseName -n $Namespace" -ForegroundColor White
