param(
    [string]$AwsProfile = 'bookshelf'
)

$ErrorActionPreference = 'Stop'
$terraformDir = Join-Path $PSScriptRoot '..\terraform'
$tfvars = Join-Path $terraformDir 'terraform.tfvars'

if (-not (Test-Path $tfvars)) {
    Copy-Item (Join-Path $terraformDir 'terraform.tfvars.example') $tfvars
    throw "Created $tfvars. Review the values, then run this script again."
}

$env:AWS_PROFILE = $AwsProfile
Push-Location $terraformDir
try {
    aws sts get-caller-identity | Out-Host
    terraform init
    terraform fmt -check
    terraform validate
    terraform plan -out=bookshelf.tfplan
    Write-Host ''
    Write-Host 'Plan saved as deployment/terraform/bookshelf.tfplan.' -ForegroundColor Green
    Write-Host 'Review the plan carefully. Run apply.ps1 only when the resources and expected cost are acceptable.'
}
finally {
    Pop-Location
}
