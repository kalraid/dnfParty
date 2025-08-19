# DFO Party Backend Local Startup Script
# This script handles port forwarding, reads config.env and starts the backend with bootRun --info
# Usage: .\start-backend.ps1 [-Background] [-Foreground] [-NoPortForward]

param(
    [switch]$Background,
    [switch]$Foreground,
    [switch]$NoPortForward,
    [switch]$Help
)

if ($Help) {
    Write-Host "=== DFO Party Backend Startup Script Help ===" -ForegroundColor Cyan
    Write-Host "Usage: .\start-backend.ps1 [options]" -ForegroundColor Green
    Write-Host ""
    Write-Host "Options:" -ForegroundColor Yellow
    Write-Host "  -Background      : Run backend in background mode" -ForegroundColor White
    Write-Host "  -Foreground      : Run backend in foreground mode (default)" -ForegroundColor White
    Write-Host "  -NoPortForward   : Skip kubectl port forwarding setup" -ForegroundColor White
    Write-Host "  -Help            : Show this help message" -ForegroundColor White
    Write-Host ""
    Write-Host "Examples:" -ForegroundColor Yellow
    Write-Host "  .\start-backend.ps1                    # Foreground mode with port forwarding" -ForegroundColor DarkGray
    Write-Host "  .\start-backend.ps1 -Background        # Background mode" -ForegroundColor DarkGray
    Write-Host "  .\start-backend.ps1 -NoPortForward     # No port forwarding" -ForegroundColor DarkGray
    exit 0
}

# Default to foreground if no mode specified
if (-not $Background -and -not $Foreground) {
    $Foreground = $true
}

$runMode = if ($Background) { "Background" } else { "Foreground" }

Write-Host "=== DFO Party Backend Local Startup Script ===" -ForegroundColor Cyan
Write-Host "Mode: $runMode" -ForegroundColor Magenta
Write-Host "Starting backend with environment variables from config.env..." -ForegroundColor Yellow

# Check and terminate existing Java processes before starting
Write-Host "`n=== Checking for port 8080 usage ===" -ForegroundColor Cyan

function Find-ProcessesUsingPort {
    param([int]$Port)
    
    $processes = @()
    
    # Method 1: Get-NetTCPConnection
    try {
        $netConnections = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue | ForEach-Object {
            $processId = $_.OwningProcess
            # Skip PID 0 (Idle process) and PID 4 (System process)
            if ($processId -gt 4) {
                Get-Process -Id $processId -ErrorAction SilentlyContinue
            }
        } | Where-Object { $_ -ne $null } | Sort-Object Id -Unique
        
        if ($netConnections) {
            $processes += $netConnections
        }
    } catch {
        Write-Host "Warning: Get-NetTCPConnection failed: $($_.Exception.Message)" -ForegroundColor Yellow
    }
    
    # Method 2: netstat as fallback
    try {
        $netstatOutput = netstat -ano | Select-String ":$Port "
        foreach ($line in $netstatOutput) {
            if ($line -match "\s+(\d+)$") {
                $pid = [int]$matches[1]
                # Skip PID 0 (Idle process) and PID 4 (System process)
                if ($pid -gt 4) {
                    $process = Get-Process -Id $pid -ErrorAction SilentlyContinue
                    if ($process -and $process.Id -notin $processes.Id) {
                        $processes += $process
                    }
                }
            }
        }
    } catch {
        Write-Host "Warning: netstat method failed: $($_.Exception.Message)" -ForegroundColor Yellow
    }
    
    return $processes
}

function Find-JavaProcesses {
    # Find all Java processes (including those that might be using port 8080)
    $javaProcesses = Get-Process | Where-Object { 
        $_.ProcessName -like "*java*" -or 
        $_.ProcessName -like "*javaw*" -or
        $_.ProcessName -like "*gradle*" -or
        ($_.ProcessName -eq "java" -and $_.MainWindowTitle -like "*spring*") -or
        ($_.CommandLine -and $_.CommandLine -like "*spring-boot*") -or
        ($_.CommandLine -and $_.CommandLine -like "*bootRun*")
    }
    
    return $javaProcesses
}

# Check for processes using port 8080
$port8080Processes = Find-ProcessesUsingPort -Port 8080

if ($port8080Processes) {
    Write-Host "Found processes using port 8080:" -ForegroundColor Yellow
    $port8080Processes | ForEach-Object {
        Write-Host "  - $($_.ProcessName) (PID: $($_.Id))" -ForegroundColor Yellow
    }
    
    Write-Host "Terminating port 8080 processes..." -ForegroundColor Yellow
    try {
        $port8080Processes | Stop-Process -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 2
    } catch {
        Write-Host "Warning: Error terminating some processes: $($_.Exception.Message)" -ForegroundColor Yellow
    }
} else {
    Write-Host "No processes found using port 8080 via network connections." -ForegroundColor Green
}

# Additional check: Find and terminate Java processes that might be Spring Boot apps
Write-Host "Checking for Java/Spring Boot processes..." -ForegroundColor Cyan
$javaProcesses = Find-JavaProcesses

if ($javaProcesses) {
    Write-Host "Found Java processes:" -ForegroundColor Yellow
    $javaProcesses | ForEach-Object {
        Write-Host "  - $($_.ProcessName) (PID: $($_.Id))" -ForegroundColor Yellow
    }
    
    # Ask user or auto-terminate suspicious Java processes
    Write-Host "Terminating Java processes that might conflict..." -ForegroundColor Yellow
    try {
        $javaProcesses | Stop-Process -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 2
    } catch {
        Write-Host "Warning: Error terminating some Java processes: $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

# Final verification using multiple methods
Write-Host "Final verification of port 8080..." -ForegroundColor Cyan
$remainingProcesses = Find-ProcessesUsingPort -Port 8080

if ($remainingProcesses) {
    Write-Host "Warning: Port 8080 is still in use after cleanup!" -ForegroundColor Red
    $remainingProcesses | ForEach-Object {
        Write-Host "  - Remaining: $($_.ProcessName) (PID: $($_.Id))" -ForegroundColor Red
    }
    
    Write-Host "Attempting final force termination..." -ForegroundColor Red
    try {
        $remainingProcesses | Stop-Process -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 3
        
        # Ultimate verification
        $finalCheck = Find-ProcessesUsingPort -Port 8080
        if ($finalCheck) {
            Write-Host "❌ ERROR: Port 8080 is still occupied after all attempts!" -ForegroundColor Red
            Write-Host "Remaining processes:" -ForegroundColor Red
            $finalCheck | ForEach-Object {
                Write-Host "  - $($_.ProcessName) (PID: $($_.Id))" -ForegroundColor Red
            }
            Write-Host "Please manually kill these processes or restart your system." -ForegroundColor Red
            Write-Host "To manually kill: taskkill /PID <PID> /F" -ForegroundColor Yellow
            exit 1
        }
    } catch {
        Write-Host "Error during final termination: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
}

Write-Host "✅ Port 8080 is now free and ready for use!" -ForegroundColor Green

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

# Check if kubectl is available (skip if NoPortForward flag is set)
if ($NoPortForward) {
    Write-Host "Skipping port forwarding (-NoPortForward flag set)" -ForegroundColor Yellow
} elseif (-not (Get-Command "kubectl" -ErrorAction SilentlyContinue)) {
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
        $portForwardJob = Start-Job -Name "port-forward-mariadb" -ScriptBlock {
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

if ($Background) {
    Write-Host "Starting backend in BACKGROUND mode..." -ForegroundColor Magenta
    Write-Host "Use 'Get-Job' to check status, 'Stop-Job' to stop" -ForegroundColor Yellow
    
    $backendJob = Start-Job -ScriptBlock {
        param($workingDir)
        Set-Location $workingDir
        & ".\gradlew.bat" bootRun --info
    } -ArgumentList (Get-Location)
    
    if ($backendJob) {
        Write-Host "✅ Backend started in background (Job ID: $($backendJob.Id))" -ForegroundColor Green
        Write-Host "Job Name: $($backendJob.Name)" -ForegroundColor DarkGray
        
        # Wait a moment and check job status
        Start-Sleep -Seconds 5
        $jobState = Get-Job -Id $backendJob.Id
        Write-Host "Job Status: $($jobState.State)" -ForegroundColor $(if ($jobState.State -eq "Running") { "Green" } else { "Red" })
        
        if ($jobState.State -eq "Failed") {
            Write-Host "❌ Backend failed to start. Job output:" -ForegroundColor Red
            Receive-Job -Job $jobState
        } else {
            Write-Host "✅ Backend is starting up. Check http://localhost:8080 in a few moments." -ForegroundColor Green
            Write-Host "To stop the backend: Stop-Job -Id $($backendJob.Id)" -ForegroundColor Yellow
        }
    } else {
        Write-Host "❌ Failed to start backend job" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "Starting backend in FOREGROUND mode..." -ForegroundColor Magenta
    Write-Host "Press Ctrl+C to stop the backend" -ForegroundColor Yellow
    Write-Host ""
    
    # Start the backend with bootRun --info in foreground
    try {
        & ".\gradlew.bat" bootRun --info
    } catch {
        Write-Host "Error starting backend: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    } finally {
        # Clean up port forwarding jobs when script exits
        Write-Host "`n=== Cleaning up background jobs ===" -ForegroundColor Cyan
        Get-Job | Where-Object { $_.State -eq "Running" -and $_.Name -like "*port-forward*" } | ForEach-Object {
            Write-Host "Stopping port forwarding job: $($_.Name) (ID: $($_.Id))" -ForegroundColor Yellow
            Stop-Job -Job $_
            Remove-Job -Job $_
        }
    }
}
