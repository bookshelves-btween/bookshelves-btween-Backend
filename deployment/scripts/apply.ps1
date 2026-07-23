param(
    [string]$AwsProfile = 'bookshelf'
)

$ErrorActionPreference = 'Stop'
$terraformDir = Join-Path $PSScriptRoot '..\terraform'
$planFile = Join-Path $terraformDir 'bookshelf.tfplan'

function Assert-NativeCommandSucceeded {
    param([string]$CommandName, [int]$ExitCode)

    if ($ExitCode -ne 0) {
        throw "$CommandName failed with exit code $ExitCode."
    }
}

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
    Assert-NativeCommandSucceeded -CommandName 'terraform apply' -ExitCode $LASTEXITCODE
    terraform output
    Assert-NativeCommandSucceeded -CommandName 'terraform output' -ExitCode $LASTEXITCODE
}
finally {
    Pop-Location
}
