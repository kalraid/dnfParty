# Docker Hub Image Cleanup Script
# PowerShell Version

param(
    [int]$KeepVersions = 5,
    [switch]$DryRun
)

Write-Host "Starting Docker Hub Image Cleanup..." -ForegroundColor Green
Write-Host "Keeping last $KeepVersions versions of each image" -ForegroundColor Cyan
if ($DryRun) {
    Write-Host "DRY RUN MODE - No images will be deleted" -ForegroundColor Yellow
}

# Function to get Docker Hub image tags
function Get-DockerHubTags {
    param([string]$Repository)
    
    try {
        $response = Invoke-RestMethod -Uri "https://registry.hub.docker.com/v2/repositories/$Repository/tags/" -Method Get
        return $response.results | Sort-Object -Property last_updated -Descending
    } catch {
        Write-Host "Failed to get tags for $Repository`: $($_.Exception.Message)" -ForegroundColor Red
        return @()
    }
}

# Function to delete Docker Hub image tag
function Remove-DockerHubTag {
    param([string]$Repository, [string]$Tag)
    
    if ($DryRun) {
        Write-Host "[DRY RUN] Would delete: $Repository`:$Tag" -ForegroundColor Yellow
        return
    }
    
    try {
        # Note: This requires Docker Hub API token with delete permissions
        # You need to set DOCKER_HUB_TOKEN environment variable
        if (-not $env:DOCKER_HUB_TOKEN) {
            Write-Host "DOCKER_HUB_TOKEN not set. Cannot delete remote images." -ForegroundColor Red
            Write-Host "Set DOCKER_HUB_TOKEN environment variable with appropriate permissions." -ForegroundColor Yellow
            return
        }
        
        $headers = @{
            "Authorization" = "Bearer $env:DOCKER_HUB_TOKEN"
        }
        
        $response = Invoke-RestMethod -Uri "https://registry.hub.docker.com/v2/repositories/$Repository/tags/$Tag/" -Method Delete -Headers $headers
        Write-Host "Deleted: $Repository`:$Tag" -ForegroundColor Green
    } catch {
        Write-Host "Failed to delete $Repository``:$Tag`: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# Clean up frontend images
Write-Host "`nCleaning up frontend images..." -ForegroundColor Cyan
$frontendTags = Get-DockerHubTags -Repository "kimrie92/dfo-party-frontend"
if ($frontendTags.Count -gt 0) {
    Write-Host "Found $($frontendTags.Count) frontend tags" -ForegroundColor Gray
    
    # Keep latest and specified number of versions
    $tagsToKeep = $frontendTags | Select-Object -First $KeepVersions
    $tagsToDelete = $frontendTags | Select-Object -Skip $KeepVersions
    
    Write-Host "Keeping tags:" -ForegroundColor Green
    foreach ($tag in $tagsToKeep) {
        Write-Host "  - $($tag.name) (updated: $($tag.last_updated))" -ForegroundColor Green
    }
    
    if ($tagsToDelete.Count -gt 0) {
        Write-Host "`nDeleting old tags:" -ForegroundColor Yellow
        foreach ($tag in $tagsToDelete) {
            if ($tag.name -ne "latest") {  # Never delete latest tag
                Remove-DockerHubTag -Repository "kimrie92/dfo-party-frontend" -Tag $tag.name
            } else {
                Write-Host "Skipping latest tag: $($tag.name)" -ForegroundColor Gray
            }
        }
    } else {
        Write-Host "No old tags to delete" -ForegroundColor Gray
    }
} else {
    Write-Host "No frontend tags found" -ForegroundColor Gray
}

# Clean up backend images
Write-Host "`nCleaning up backend images..." -ForegroundColor Cyan
$backendTags = Get-DockerHubTags -Repository "kimrie92/dfo-party-backend"
if ($backendTags.Count -gt 0) {
    Write-Host "Found $($backendTags.Count) backend tags" -ForegroundColor Gray
    
    # Keep latest and specified number of versions
    $tagsToKeep = $backendTags | Select-Object -First $KeepVersions
    $tagsToDelete = $backendTags | Select-Object -Skip $KeepVersions
    
    Write-Host "Keeping tags:" -ForegroundColor Green
    foreach ($tag in $tagsToKeep) {
        Write-Host "  - $($tag.name) (updated: $($tag.last_updated))" -ForegroundColor Green
    }
    
    if ($tagsToDelete.Count -gt 0) {
        Write-Host "`nDeleting old tags:" -ForegroundColor Yellow
        foreach ($tag in $tagsToDelete) {
            if ($tag.name -ne "latest") {  # Never delete latest tag
                Remove-DockerHubTag -Repository "kimrie92/dfo-party-backend" -Tag $tag.name
            } else {
                Write-Host "Skipping latest tag: $($tag.name)" -ForegroundColor Gray
            }
        }
    } else {
        Write-Host "No old tags to delete" -ForegroundColor Gray
    }
} else {
    Write-Host "No backend tags found" -ForegroundColor Gray
}

Write-Host "`nDocker Hub cleanup completed!" -ForegroundColor Green

# Show usage information
Write-Host "`nUsage Examples:" -ForegroundColor Magenta
Write-Host "   .\cleanup-dockerhub.ps1                    # Keep last 5 versions (default)" -ForegroundColor White
Write-Host "   .\cleanup-dockerhub.ps1 -KeepVersions 3   # Keep last 3 versions" -ForegroundColor White
Write-Host "   .\cleanup-dockerhub.ps1 -DryRun           # Show what would be deleted without deleting" -ForegroundColor White
Write-Host "   .\cleanup-dockerhub.ps1 -KeepVersions 10 -DryRun  # Keep last 10, dry run" -ForegroundColor White

# Note about Docker Hub token
Write-Host "`nNote: To delete remote images, set DOCKER_HUB_TOKEN environment variable:" -ForegroundColor Cyan
Write-Host "   $env:DOCKER_HUB_TOKEN = 'your_docker_hub_token_here'" -ForegroundColor White
Write-Host "   Or add it to your config.env file" -ForegroundColor White
