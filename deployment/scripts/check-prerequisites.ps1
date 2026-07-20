param(
    [string]$AwsProfile = 'bookshelf'
)

$ErrorActionPreference = 'Stop'

$required = @('terraform', 'aws', 'git', 'ssh-keygen')
$missing = @()

foreach ($name in $required) {
    $command = Get-Command $name -ErrorAction SilentlyContinue
    if ($null -eq $command) {
        $missing += $name
        Write-Host "[MISSING] $name" -ForegroundColor Red
    }
    else {
        Write-Host "[OK]      $name -> $($command.Source)" -ForegroundColor Green
    }
}

if ($missing.Count -gt 0) {
    Write-Host ''
    Write-Host 'Install missing tools from the official pages:'
    Write-Host 'Terraform: https://developer.hashicorp.com/terraform/install'
    Write-Host 'AWS CLI v2: https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html'
    exit 1
}

Write-Host ''
Write-Host "Checking AWS authentication for profile '$AwsProfile'..."
aws sts get-caller-identity --profile $AwsProfile

Write-Host ''
Write-Host 'Prerequisites are ready.' -ForegroundColor Green
