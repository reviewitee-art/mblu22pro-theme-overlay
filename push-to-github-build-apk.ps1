param(
    [string]$GitHubToken = "",
    [string]$RepoName = "mblu22pro-theme-overlay"
)

[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
Set-Location -Path $PSScriptRoot

Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host "  MEIZU MBLU 22 PRO - GITHUB ACTIONS APK BUILDER                  " -ForegroundColor Cyan
Write-Host "==================================================================" -ForegroundColor Cyan

if ([string]::IsNullOrWhiteSpace($GitHubToken)) {
    $GitHubToken = (Read-Host "Enter GitHub Token").Trim()
}

$headers = @{
    "Authorization" = "Bearer $GitHubToken"
    "Accept"        = "application/vnd.github+json"
    "User-Agent"    = "Mblu22Pro-APK-Builder"
}

try {
    $user = Invoke-RestMethod -Uri "https://api.github.com/user" -Headers $headers -Method Get
    $owner = $user.login
    Write-Host "[OK] Connected to GitHub account: $owner" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Invalid token: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

try {
    $null = Invoke-RestMethod -Uri "https://api.github.com/repos/$owner/$RepoName" -Headers $headers -Method Get
    Write-Host "[OK] Found repository: https://github.com/$owner/$RepoName" -ForegroundColor Green
} catch {
    Write-Host "-> Creating repository '$RepoName'..." -ForegroundColor Cyan
    $body = @{
        name        = $RepoName
        description = "Meizu mblu 22 Pro iOS 18 and HyperOS System UI Overlay APK"
        private     = $false
        auto_init   = $true
    } | ConvertTo-Json
    $null = Invoke-RestMethod -Uri "https://api.github.com/user/repos" -Headers $headers -Method Post -Body $body -ContentType "application/json"
    Start-Sleep -Seconds 3
    Write-Host "[OK] Created repository: https://github.com/$owner/$RepoName" -ForegroundColor Green
}

$allFiles = Get-ChildItem -Path $PSScriptRoot -Recurse -File | Where-Object {
    $_.FullName -notmatch "\\\.git\\" -and $_.FullName -notmatch "\\build\\" -and $_.FullName -notmatch "\\output-apk\\"
}

$normalFiles = $allFiles | Where-Object { $_.FullName -notmatch "build-apk\.yml$" }
$workflowFiles = $allFiles | Where-Object { $_.FullName -match "build-apk\.yml$" }
$orderedFiles = @($normalFiles) + @($workflowFiles)

foreach ($file in $orderedFiles) {
    if ($null -eq $file) { continue }
    $relPath = $file.FullName.Substring($PSScriptRoot.Length + 1).Replace("\", "/")
    $bytes = [System.IO.File]::ReadAllBytes($file.FullName)
    $b64 = [Convert]::ToBase64String($bytes)

    $apiUrl = "https://api.github.com/repos/$owner/$RepoName/contents/$relPath"
    $sha = $null
    try {
        $existing = Invoke-RestMethod -Uri $apiUrl -Headers $headers -Method Get
        $sha = $existing.sha
    } catch {}

    $payload = @{
        message = "Upload $relPath"
        content = $b64
    }
    if ($sha) { $payload["sha"] = $sha }

    $jsonPayload = $payload | ConvertTo-Json
    Write-Host "  Uploading: $relPath" -ForegroundColor Gray
    $null = Invoke-RestMethod -Uri $apiUrl -Headers $headers -Method Put -Body $jsonPayload -ContentType "application/json"
}

Write-Host ""
Write-Host "==================================================================" -ForegroundColor Green
Write-Host "  SUCCESS! GITHUB ACTIONS IS NOW BUILDING YOUR APK!               " -ForegroundColor Green
Write-Host "==================================================================" -ForegroundColor Green
Write-Host "Actions URL: https://github.com/$owner/$RepoName/actions" -ForegroundColor Cyan
