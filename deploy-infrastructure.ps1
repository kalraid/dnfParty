# Infrastructure Deployment Script for DFO Party
# This script deploys MariaDB and Nginx which are stable components

param(
    [string]$Namespace = "dfo",
    [string]$ReleaseName = "df-party-infrastructure",
    [string]$ChartPath = "./helm-charts-infrastructure"
)

Write-Host "=== DFO Party Infrastructure Deployment ===" -ForegroundColor Green
Write-Host "Namespace: $Namespace" -ForegroundColor Yellow
Write-Host "Release Name: $ReleaseName" -ForegroundColor Yellow
Write-Host "Chart Path: $ChartPath" -ForegroundColor Yellow

# Check if kubectl is available
try {
    $kubectlVersion = kubectl version --client --output=json | ConvertFrom-Json
    Write-Host "Kubectl version: $($kubectlVersion.clientVersion.gitVersion)" -ForegroundColor Green
} catch {
    Write-Error "Kubectl is not available or not working properly"
    exit 1
}

# Check if helm is available
try {
    $helmVersion = helm version --short
    Write-Host "Helm version: $helmVersion" -ForegroundColor Green
} catch {
    Write-Error "Helm is not available or not working properly"
    exit 1
}

# Function to check if namespace exists
function Test-Namespace {
    param([string]$Namespace)
    
    $ns = kubectl get namespace $Namespace --ignore-not-found=true
    if (-not $ns) {
        Write-Host "Creating namespace: $Namespace" -ForegroundColor Yellow
        kubectl create namespace $Namespace
        Start-Sleep -Seconds 2
    } else {
        Write-Host "Namespace $Namespace already exists" -ForegroundColor Green
    }
}



# Function to check if infrastructure release exists
function Test-InfrastructureRelease {
    param(
        [string]$ReleaseName,
        [string]$Namespace
    )
    
    $release = helm list -n $Namespace --output=json 2>$null | ConvertFrom-Json | Where-Object { $_.name -eq $ReleaseName }
    return $release -ne $null
}

# Main deployment logic
try {
    # Ensure namespace exists
    Test-Namespace -Namespace $Namespace
    
    # Check if infrastructure release exists
    $releaseExists = Test-InfrastructureRelease -ReleaseName $ReleaseName -Namespace $Namespace
    
    if ($releaseExists) {
        Write-Host "Found existing infrastructure release: $ReleaseName" -ForegroundColor Yellow
        Write-Host "Proceeding with upgrade..." -ForegroundColor Green
    } else {
        Write-Host "No existing infrastructure release found. Installing new release..." -ForegroundColor Green
    }
    
    # Build latest Helm chart dependencies before deployment
    Write-Host "Building latest Helm chart dependencies..." -ForegroundColor Cyan
    Set-Location $ChartPath
    helm dependency build
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Helm dependency build failed" -ForegroundColor Red
        exit 1
    }
    Set-Location ..
    Write-Host "Helm dependencies built successfully" -ForegroundColor Green
    
    # Deploy infrastructure
    Write-Host "Deploying infrastructure chart..." -ForegroundColor Green
    
    helm upgrade --install $ReleaseName $ChartPath -n $Namespace --create-namespace
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Infrastructure deployment completed successfully!" -ForegroundColor Green
        
        # Show deployment status
        Write-Host "`n=== Deployment Status ===" -ForegroundColor Green
        helm status $ReleaseName -n $Namespace
        
        Write-Host "`n=== Pods Status ===" -ForegroundColor Green
        kubectl get pods -n $Namespace
        
        Write-Host "`n=== Services Status ===" -ForegroundColor Green
        kubectl get services -n $Namespace
        
    } else {
        Write-Error "Infrastructure deployment failed!"
        exit 1
    }
    
} catch {
    Write-Error "Deployment failed with error: $_"
    exit 1
}

Write-Host "`n=== Infrastructure Deployment Complete ===" -ForegroundColor Green
Write-Host "Next step: Deploy application using deploy-application.ps1" -ForegroundColor Yellow
