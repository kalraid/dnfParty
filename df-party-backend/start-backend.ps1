# DFO Party Backend Local Startup Script
# This script handles port forwarding, reads config.env and starts the backend with bootRun --info

Write-Host "=== DFO Party Backend Local Startup Script ===" -ForegroundColor Cyan
Write-Host "Starting backend with environment variables from config.env..." -ForegroundColor Yellow

# Check if we're in the correct directory
if (-not (Test-Path "build.gradle")) {
    Write-Host "Error: This script must be run from the backend directory (df-party-backend)" -ForegroundColor Red
    Write-Host "Current directory: $(Get-Location)" -ForegroundColor Red
    Write-Host "Please run this script from: dnfParty/df-party-backend" -ForegroundColor Red
    exit 1
}

# Check if config.env exists
$configPath = "..\config.env"
if (-not (Test-Path $configPath)) {
    Write-Host "Error: config.env file not found at: $configPath" -ForegroundColor Red
    Write-Host "Please ensure config.env exists in the dnfParty directory" -ForegroundColor Red
    exit 1
}

Write-Host "Reading environment variables from: $configPath" -ForegroundColor Green

# Read config.env and set environment variables
try {
    Get-Content $configPath | ForEach-Object { 
        if ($_ -match '^([^#][^=]+)=(.*)$') { 
            $key = $matches[1].Trim()
            $value = $matches[2].Trim()
            
            # Remove quotes if present
            if ($value.StartsWith('"') -and $value.EndsWith('"')) {
                $value = $value.Substring(1, $value.Length - 2)
            }
            if ($value.StartsWith("'") -and $value.EndsWith("'")) {
                $value = $value.Substring(1, $value.Length - 2)
            }
            
            [Environment]::SetEnvironmentVariable($key, $value, 'Process')
            Write-Host "Set $key = $value" -ForegroundColor DarkGray
        } 
    }
    Write-Host "Environment variables loaded successfully!" -ForegroundColor Green
} catch {
    Write-Host "Error loading environment variables: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Display key environment variables
Write-Host "`n=== Key Environment Variables ===" -ForegroundColor Cyan
Write-Host "DF_API_KEY: $env:DF_API_KEY" -ForegroundColor Green
Write-Host "SPRING_PROFILES_ACTIVE: $env:SPRING_PROFILES_ACTIVE" -ForegroundColor Green
Write-Host "DB_HOST: $env:DB_HOST" -ForegroundColor Green
Write-Host "DB_PORT: $env:DB_PORT" -ForegroundColor Green
Write-Host "DB_NAME: $env:DB_NAME" -ForegroundColor Green

# Check if Java is available
if (-not (Get-Command "java" -ErrorAction SilentlyContinue)) {
    Write-Host "Error: Java is not installed or not in PATH" -ForegroundColor Red
    Write-Host "Please install Java 17 or later" -ForegroundColor Red
    exit 1
}

$javaVersion = java -version 2>&1 | Select-String "version"
Write-Host "Java version: $javaVersion" -ForegroundColor Green

# Check if Gradle wrapper exists
if (-not (Test-Path "gradlew.bat")) {
    Write-Host "Error: Gradle wrapper (gradlew.bat) not found" -ForegroundColor Red
    Write-Host "Please ensure you're in the correct backend directory" -ForegroundColor Red
    exit 1
}

# Check if kubectl is available
if (-not (Get-Command "kubectl" -ErrorAction SilentlyContinue)) {
    Write-Host "Warning: kubectl is not available - skipping port forwarding" -ForegroundColor Yellow
    Write-Host "Please ensure kubectl is installed if you need database connection" -ForegroundColor Yellow
} else {
    Write-Host "`n=== Setting up Database Port Forwarding ===" -ForegroundColor Cyan
    
    # Check if port 3306 is already in use
    $portInUse = Get-NetTCPConnection -LocalPort 3306 -ErrorAction SilentlyContinue
    if ($portInUse) {
        Write-Host "Port 3306 is already in use. Checking if it's kubectl port-forward..." -ForegroundColor Yellow
        
        # Get the process using port 3306
        $processId = (Get-NetTCPConnection -LocalPort 3306 | Select-Object -First 1).OwningProcess
        $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
        
        if ($process -and $process.ProcessName -eq "kubectl") {
            Write-Host "kubectl port-forward is already running on port 3306" -ForegroundColor Green
        } else {
            Write-Host "Warning: Port 3306 is occupied by process: $($process.ProcessName) (PID: $processId)" -ForegroundColor Yellow
            Write-Host "You may need to stop this process or use a different port" -ForegroundColor Yellow
        }
    } else {
        Write-Host "Starting kubectl port-forward in background..." -ForegroundColor Green
        
        # Start kubectl port-forward in background
        $portForwardJob = Start-Job -ScriptBlock {
            kubectl port-forward -n dfo svc/df-party-infrastructure-mariadb 3306:3306
        }
        
        if ($portForwardJob) {
            Write-Host "Port forwarding started in background (Job ID: $($portForwardJob.Id))" -ForegroundColor Green
            Write-Host "Database will be available on localhost:3306" -ForegroundColor Green
            
            # Wait a moment for port forwarding to establish
            Start-Sleep -Seconds 3
            
            # Check if port forwarding is working
            $portCheck = Get-NetTCPConnection -LocalPort 3306 -ErrorAction SilentlyContinue
            if ($portCheck) {
                Write-Host "Port forwarding established successfully!" -ForegroundColor Green
            } else {
                Write-Host "Warning: Port forwarding may not be ready yet" -ForegroundColor Yellow
            }
        } else {
            Write-Host "Failed to start port forwarding" -ForegroundColor Red
        }
    }
}

Write-Host "`n=== Starting Backend with bootRun --info ===" -ForegroundColor Cyan
Write-Host "Backend will start on: http://localhost:8080" -ForegroundColor Green
Write-Host "Press Ctrl+C to stop the backend" -ForegroundColor Yellow
Write-Host ""

# Start the backend with bootRun --info
try {
    .\gradlew.bat bootRun --info
} catch {
    Write-Host "Error starting backend: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
} finally {
    # Clean up port forwarding jobs when script exits
    Write-Host "`n=== Cleaning up background jobs ===" -ForegroundColor Cyan
    Get-Job | Where-Object { $_.State -eq "Running" } | ForEach-Object {
        Write-Host "Stopping background job: $($_.Name) (ID: $($_.Id))" -ForegroundColor Yellow
        Stop-Job -Job $_
        Remove-Job -Job $_
    }
}
