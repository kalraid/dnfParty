# 프론트엔드 개발 서버 시작 스크립트
# 환경변수를 설정한 후 Vite를 실행합니다.

# 한글 인코딩 설정
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "=== DFO Party Frontend Development Server Start ===" -ForegroundColor Green

# config.env 파일에서 환경변수 로드
$configPath = Join-Path $PSScriptRoot "..\config.env"
if (Test-Path $configPath) {
    Write-Host "Found config.env file. Loading environment variables..." -ForegroundColor Yellow
    
    Get-Content $configPath | ForEach-Object {
        if ($_ -match '^([^#][^=]+)=(.*)$') {
            $key = $matches[1].Trim()
            $value = $matches[2].Trim()
            
            if ($key -like "VITE_*") {
                [Environment]::SetEnvironmentVariable($key, $value, "Process")
                Write-Host "  $key = $value" -ForegroundColor Cyan
            }
        }
    }
} else {
    Write-Host "config.env file not found. Using default values." -ForegroundColor Yellow
    [Environment]::SetEnvironmentVariable("VITE_API_BASE_URL", "http://localhost:8080/api", "Process")
    [Environment]::SetEnvironmentVariable("VITE_WS_BASE_URL", "http://localhost:8080", "Process")
    Write-Host "  VITE_API_BASE_URL = http://localhost:8080/api" -ForegroundColor Cyan
    Write-Host "  VITE_WS_BASE_URL = http://localhost:8080" -ForegroundColor Cyan
}

Write-Host "`nEnvironment variables set successfully!" -ForegroundColor Green
Write-Host "Starting Vite development server..." -ForegroundColor Green

# Vite 개발 서버 시작
npm run dev
