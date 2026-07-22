param(
    [string]$AwsProfile = 'bookshelf'
)

$ErrorActionPreference = 'Stop'
$terraformDir = Join-Path $PSScriptRoot '..\terraform'
$tfvars = Join-Path $terraformDir 'terraform.tfvars'
$planFile = Join-Path $terraformDir 'bookshelf.tfplan'

function Assert-NativeCommandSucceeded {
    param([string]$CommandName, [int]$ExitCode)

    if ($ExitCode -ne 0) {
        throw "$CommandName failed with exit code $ExitCode."
    }
}

if (-not (Test-Path $tfvars)) {
    Copy-Item (Join-Path $terraformDir 'terraform.tfvars.example') $tfvars
    throw "Created $tfvars. Review the values, then run this script again."
}

$env:AWS_PROFILE = $AwsProfile
Push-Location $terraformDir
try {
    if (Test-Path $planFile) {
        Remove-Item $planFile -Force
    }

    aws sts get-caller-identity | Out-Host
    Assert-NativeCommandSucceeded -CommandName 'aws sts get-caller-identity' -ExitCode $LASTEXITCODE
    terraform init
    Assert-NativeCommandSucceeded -CommandName 'terraform init' -ExitCode $LASTEXITCODE
    terraform fmt -check
    Assert-NativeCommandSucceeded -CommandName 'terraform fmt -check' -ExitCode $LASTEXITCODE
    terraform validate
    Assert-NativeCommandSucceeded -CommandName 'terraform validate' -ExitCode $LASTEXITCODE
    terraform plan -out=bookshelf.tfplan
    Assert-NativeCommandSucceeded -CommandName 'terraform plan' -ExitCode $LASTEXITCODE
    Write-Host ''
    Write-Host 'Plan saved as deployment/terraform/bookshelf.tfplan.' -ForegroundColor Green
    Write-Host 'Review the plan carefully. Run apply.ps1 only when the resources and expected cost are acceptable.'
}
finally {
    Pop-Location
}
