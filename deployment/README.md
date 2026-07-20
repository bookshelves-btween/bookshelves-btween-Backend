# 책장사이 AWS 배포 환경

이 디렉터리는 아직 애플리케이션 코드가 없는 상태에서도 AWS 개발 서버를 재현 가능하게 준비하고, 이후 Spring Boot 컨테이너를 같은 서버에 배포하기 위한 구성입니다.

## 기본 아키텍처

초기 MVP에서는 비용과 운영 난도를 낮추기 위해 한 대의 EC2에서 Docker Compose로 애플리케이션, MySQL, Redis, Nginx를 실행합니다.

```text
Internet
  -> Elastic IP
  -> EC2 / Nginx :80, :443
      -> Spring Boot :8080 (외부 비공개)
      -> MySQL :3306 (외부 비공개)
      -> Redis :6379 (외부 비공개)
```

Terraform이 만드는 리소스:

- 서울 리전 전용 VPC와 Public Subnet
- Internet Gateway와 Route Table
- HTTP/HTTPS만 공개하는 Security Group
- Ubuntu 24.04, x86_64, 암호화된 gp3 볼륨을 사용하는 EC2
- 브라우저/CLI Session Manager 접속용 IAM Role
- 고정 주소인 Elastic IP
- Docker와 Docker Compose를 설치하고 임시 Nginx 페이지를 실행하는 cloud-init

현재 구성은 단일 서버 개발 환경입니다. 실제 상용 운영 전에는 MySQL을 RDS로, Redis를 ElastiCache로 분리하고 다중 AZ, 백업, HTTPS, 모니터링 정책을 별도로 설계해야 합니다.

## 1. 비용과 계정 보안

Terraform 실행 전에 반드시 다음을 완료합니다.

1. AWS 루트 계정 MFA 설정
2. 루트 계정 대신 IAM Identity Center 관리자 계정 사용
3. AWS Budgets 월간 비용 알림 생성
4. 콘솔 리전이 서울 `ap-northeast-2`인지 확인

EC2, EBS, 데이터 전송, 공인 IPv4에 비용이 발생할 수 있습니다. Terraform `apply`는 예산 상한을 설정하지 않습니다.

## 2. 로컬 도구 설치

현재 Windows PC에 다음 도구가 필요합니다.

- Terraform 1.10 이상
- AWS CLI v2
- Git
- OpenSSH

공식 설치 페이지:

- Terraform: <https://developer.hashicorp.com/terraform/install>
- AWS CLI v2: <https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html>

설치 후 새 PowerShell을 열고 확인합니다.

```powershell
terraform version
aws --version
git --version
```

## 3. AWS 로그인

IAM Identity Center를 사용하는 경우 프로필을 생성합니다.

```powershell
aws configure sso --profile bookshelf
aws sso login --profile bookshelf
aws sts get-caller-identity --profile bookshelf
```

마지막 명령에서 본인의 AWS Account와 ARN이 출력되어야 합니다. 루트 Access Key를 생성하거나 코드에 AWS Access Key를 넣지 않습니다.

도구와 인증을 한 번에 확인하려면 저장소 루트에서 실행합니다.

```powershell
powershell -ExecutionPolicy Bypass -File .\deployment\scripts\check-prerequisites.ps1
```

## 4. Terraform 변수 준비

첫 `plan.ps1` 실행은 `terraform.tfvars.example`을 `terraform.tfvars`로 복사하고 중지합니다.

```powershell
powershell -ExecutionPolicy Bypass -File .\deployment\scripts\plan.ps1
```

생성된 `deployment/terraform/terraform.tfvars`를 확인합니다. 기본값은 다음과 같습니다.

```hcl
aws_region       = "ap-northeast-2"
project_name     = "bookshelf"
environment      = "dev"
instance_type    = "t3.medium"
root_volume_size = 30
admin_cidr       = ""
ssh_public_key   = ""
```

기본 설정은 SSH 22번 포트를 열지 않고 AWS Systems Manager Session Manager를 사용합니다.

SSH가 꼭 필요하다면 키를 생성합니다.

```powershell
ssh-keygen -t ed25519 -f "$HOME\.ssh\bookshelf_aws"
Get-Content "$HOME\.ssh\bookshelf_aws.pub"
```

`terraform.tfvars`에 공개 키 한 줄과 현재 공인 IP `/32`만 입력합니다. 개인 키는 절대로 입력하거나 Git에 추가하지 않습니다.

## 5. 생성 계획 검토

변수를 확인한 뒤 다시 실행합니다.

```powershell
powershell -ExecutionPolicy Bypass -File .\deployment\scripts\plan.ps1
```

이 스크립트는 다음 작업만 수행합니다.

```text
terraform init
terraform fmt -check
terraform validate
terraform plan -out=bookshelf.tfplan
```

아직 AWS 리소스를 생성하지 않습니다. 출력에서 `Plan: ... to add`와 EC2 유형, EBS 크기, Elastic IP, Security Group 규칙을 확인합니다.

## 6. AWS 리소스 생성

계획과 예상 비용을 확인한 경우에만 실행합니다.

```powershell
powershell -ExecutionPolicy Bypass -File .\deployment\scripts\apply.ps1
```

스크립트에서 `APPLY`를 정확히 입력해야 실제 리소스가 생성됩니다.

완료 후 다음 출력이 표시됩니다.

- `instance_id`
- `public_ip`
- `bootstrap_url`
- `ssm_session_command`
- `ssh_command`

cloud-init이 Docker를 설치하는 데 몇 분이 걸릴 수 있습니다. `bootstrap_url`을 브라우저에서 열어 "책장사이 dev 서버 준비 완료"가 보이면 인프라 배포가 완료된 것입니다.

## 7. 서버 접속과 부트스트랩 확인

AWS 콘솔에서는 다음 경로로 접속할 수 있습니다.

```text
EC2 -> Instances -> bookshelf-dev-server -> Connect -> Session Manager
```

서버에서 진행 상황을 확인합니다.

```bash
sudo tail -f /var/log/cloud-init-output.log
docker --version
docker compose version
docker ps
curl http://127.0.0.1
```

## 8. Spring Boot 구현 후 실제 런타임 배포

`runtime/compose.yml`은 다음을 전제로 준비되어 있습니다.

- Java 21 / Spring Boot 3
- Spring Boot Actuator의 `/actuator/health`
- Flyway 마이그레이션
- MySQL 8.4
- Redis 7.4
- 애플리케이션 로그는 파일이 아니라 표준 출력
- 애플리케이션 이미지는 GitHub Container Registry 등에서 다운로드

서버에 저장소를 받은 후 런타임 디렉터리를 준비합니다.

```bash
sudo install -d -o ubuntu -g ubuntu /opt/bookshelf/runtime
cp -R deployment/runtime/. /opt/bookshelf/runtime/
cd /opt/bookshelf/runtime
cp .env.example .env
chmod 600 .env
nano .env
```

`.env`의 모든 `CHANGE_TO_*`, `your-github-org`, CORS 주소를 실제 값으로 변경합니다. 안전한 임의 문자열 예시:

```bash
openssl rand -base64 48
openssl rand -hex 64
```

비공개 GHCR 이미지를 사용하면 먼저 로그인합니다.

```bash
echo "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USER" --password-stdin
```

배포 실행:

```bash
cd /opt/bookshelf/runtime
bash scripts/deploy.sh
```

스크립트는 예제 비밀번호가 남아 있으면 중지하고, Compose 구문을 확인한 뒤 이미지를 내려받아 실행합니다. 마지막으로 `http://127.0.0.1/health`가 성공할 때까지 기다립니다.

## 9. 데이터 백업

단일 EC2의 MySQL은 직접 백업해야 합니다.

```bash
cd /opt/bookshelf/runtime
bash scripts/backup-mysql.sh
```

기본 경로는 `/opt/bookshelf/backups/mysql`, 기본 보관 기간은 7일입니다. EC2와 같은 디스크에만 보관하면 인스턴스 장애를 견딜 수 없으므로 이후 S3 업로드와 복구 테스트를 추가해야 합니다.

## 10. 중요한 제한과 다음 단계

- HTTPS는 도메인이 준비된 뒤 Nginx/ACM/인증서 구성을 추가합니다.
- MySQL과 Redis 포트는 인터넷에 노출하지 않습니다.
- `terraform.tfstate`, `.env`, 개인 키는 Git에 올리지 않습니다.
- `.terraform.lock.hcl`은 공급자 버전을 팀에서 동일하게 재현하기 위해 Git에 포함합니다.
- 한 명만 로컬 Terraform State를 관리할 수 있습니다. 팀 공동 운영 전에는 S3 Backend와 State Locking을 구성해야 합니다.
- `terraform destroy`는 EC2와 로컬 DB 볼륨을 삭제합니다. 백업 없이 실행하면 복구할 수 없습니다.
- EC2를 중지해도 EBS와 공인 IPv4 비용은 남을 수 있습니다.

실제 Spring Boot 저장소가 생성되면 다음 단계는 Dockerfile, Actuator, Flyway, GitHub Actions를 이 환경에 연결하는 것입니다.
