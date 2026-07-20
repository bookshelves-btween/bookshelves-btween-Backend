param(
    [string]$AwsProfile = 'bookshelf'
)

$ErrorActionPreference = 'Stop'
$terraformDir = Join-Path $PSScriptRoot '..\terraform'
$planFile = Join-Path $terraformDir 'bookshelf.tfplan'

if (-not (Test-Path $planFile)) {
    throw 'No saved Terraform plan found. Run plan.ps1 first.'
}

$confirmation = Read-Host 'This creates billable AWS resources. Type APPLY to continue'
if ($confirmation -cne 'APPLY') {
    Write-Host 'Cancelled. No AWS resources were changed.'
    exit 0
}

$env:AWS_PROFILE = $AwsProfile
Push-Location $terraformDir
try {
    terraform apply bookshelf.tfplan
    terraform output
}
finally {
    Pop-Location
}
