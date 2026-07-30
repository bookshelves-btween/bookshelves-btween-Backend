# 책장사이 AWS 배포 환경

이 디렉터리는 아직 애플리케이션 코드가 없는 상태에서도 AWS 개발 서버를 재현 가능하게 준비하고, 이후 Spring Boot 컨테이너를 같은 서버에 배포하기 위한 구성입니다.

## 기본 아키텍처

개발 환경은 작은 EC2에서 애플리케이션, Redis, Nginx를 실행하고 MySQL은 비공개 RDS로 분리합니다.

```text
Internet
  -> Elastic IP
  -> EC2 / Nginx :80, :443
      -> Spring Boot :8080 (외부 비공개)
      -> Redis :6379 (외부 비공개)
      -> RDS MySQL :3306 (EC2 보안 그룹에서만 접근)
```

Terraform이 만드는 리소스:

- 서울 리전 전용 VPC, Public Subnet, 두 개의 Private Database Subnet
- Internet Gateway와 Route Table
- HTTP/HTTPS와 선택적 SSH만 공개하는 EC2 Security Group
- EC2 Security Group에서만 3306을 허용하는 RDS Security Group
- Ubuntu 24.04, x86_64, 암호화된 gp3 볼륨을 사용하는 EC2
- 브라우저/CLI Session Manager 접속용 IAM Role
- 고정 주소인 Elastic IP
- Single-AZ RDS for MySQL 8.4와 7일 자동 백업
- Docker와 Docker Compose를 설치하고 임시 Nginx 페이지를 실행하는 cloud-init

현재 구성은 비용을 낮춘 개발 환경입니다. 실제 상용 운영 전에는 RDS 다중 AZ, ElastiCache, 삭제 방지, 최종 스냅샷, 모니터링 정책을 별도로 설계해야 합니다. 애플리케이션 런타임은 유효한 TLS 인증서가 없으면 배포되지 않으며 HTTP 요청을 HTTPS로 전환합니다.

현재 환경은 비용과 초기 운영 여유를 함께 고려해 `db.t4g.small`을 사용합니다. 트래픽과 연결 수를 측정해 필요하면 `db.t4g.medium` 이상으로 조정하고, 가용성이 필요하면 Multi-AZ를 활성화합니다.

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
aws_profile      = "bookshelf"
project_name     = "bookshelf"
environment      = "dev"
instance_type    = "t3.small"
root_volume_size = 30
db_instance_class        = "db.t4g.small"
db_engine_version        = "8.4"
db_name                  = "bookshelf"
db_master_username       = "bookshelf_admin"
db_allocated_storage     = 20
db_backup_retention_days = 7
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
- RDS for MySQL 8.4
- Redis 7.4
- 애플리케이션 로그는 파일이 아니라 표준 출력
- 애플리케이션 이미지는 GitHub Container Registry 등에서 다운로드
- 애플리케이션 이미지에는 Actuator 상태 확인에 사용할 `curl` 또는 `wget` 포함
- MySQL 연결은 AWS RDS CA truststore와 `VERIFY_IDENTITY`를 사용해 서버 인증서와 호스트명을 검증

서버에 저장소를 받은 후 런타임 디렉터리를 준비합니다.

```bash
sudo install -d -o ubuntu -g ubuntu /opt/bookshelf/runtime
cp -R deployment/runtime/. /opt/bookshelf/runtime/
cd /opt/bookshelf/runtime
cp .env.example .env
chmod 600 .env
nano .env
```

`.env`의 모든 `CHANGE_TO_*`, 예시 도메인, CORS 주소를 실제 값으로 변경합니다. `APP_IMAGE`는 CI가 빌드한 이미지의 `@sha256:...` digest까지 입력해야 하며 `latest` 태그는 허용하지 않습니다. 안전한 임의 문자열 예시:

```bash
openssl rand -base64 48
openssl rand -hex 64
```

RDS 접속 정보는 Terraform을 실행한 PC에서 확인합니다.

```powershell
cd .\deployment\terraform
terraform output -raw database_endpoint
terraform output -raw database_username
terraform output -raw database_password
```

출력된 값을 서버의 `.env`에 있는 `MYSQL_HOST`, `MYSQL_USER`, `MYSQL_PASSWORD`에 각각 입력합니다. 데이터베이스 비밀번호는 화면 공유, 채팅, Git에 남기지 않습니다.

`deploy.sh`는 AWS 공식 global RDS CA 번들을 HTTPS로 내려받고 Java truststore를 생성합니다. 생성 경로와 truststore 비밀번호는 `.env`의 `RDS_CA_BUNDLE_PATH`, `RDS_TRUSTSTORE_PATH`, `RDS_TRUSTSTORE_PASSWORD`로 지정합니다. 애플리케이션과 SQL 백업은 모두 해당 CA를 이용해 RDS 인증서와 엔드포인트 호스트명을 검증합니다.

공개 API 도메인의 TLS 인증서를 먼저 발급하고 서버의 `.env`에 호스트 경로를 입력합니다. 예를 들어 Let's Encrypt를 사용한다면 `TLS_CERT_PATH`는 `fullchain.pem`, `TLS_PRIVATE_KEY_PATH`는 `privkey.pem`의 절대 경로입니다. 인증서나 개인 키는 Git에 추가하지 않습니다.

비공개 GHCR 이미지를 사용하면 먼저 로그인합니다.

```bash
echo "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USER" --password-stdin
```

배포 실행:

```bash
cd /opt/bookshelf/runtime
bash scripts/deploy.sh
```

스크립트는 예제 값, 가변 애플리케이션 이미지, 누락된 TLS 파일이 있으면 중지합니다. 현재 서비스를 중단하기 전에 이미지를 모두 내려받고, 애플리케이션 상태 확인이 성공한 뒤 Nginx를 시작합니다. 마지막으로 `https://127.0.0.1/health`가 성공할 때까지 기다립니다.

## 9. 데이터 백업

RDS 자동 백업은 기본 7일간 보관됩니다. 추가 SQL 덤프가 필요하면 EC2에서 다음 스크립트를 실행합니다.

```bash
cd /opt/bookshelf/runtime
bash scripts/backup-mysql.sh
```

스크립트는 `.env`의 RDS 접속 정보를 사용하며 기본 경로는 `/opt/bookshelf/backups/mysql`, 기본 보관 기간은 7일입니다. EC2 디스크의 SQL 덤프는 보조 사본일 뿐이므로 이후 S3 업로드와 RDS 복구 테스트를 추가해야 합니다.

## 10. 스키마 관리와 Flyway

스키마를 만드는 주체는 Flyway 하나입니다. 애플리케이션은 로컬과 운영 모두 `ddl-auto: validate`로 동작하므로 Hibernate는 테이블을 만들지도 고치지도 않고, 엔티티와 실제 스키마가 다르면 기동을 중단합니다.

따라서 엔티티에 테이블이나 컬럼을 추가하면 `src/main/resources/db/migration`에 마이그레이션을 함께 작성해야 합니다. 빠뜨리면 로컬에서 바로 기동 실패로 드러납니다.

### 새 환경에 처음 배포할 때

빈 데이터베이스에 그대로 배포하면 됩니다. `V1__baseline_schema.sql`이 전체 스키마와 카테고리 마스터를 세우고, 이후 마이그레이션이 순서대로 적용됩니다. 별도 준비가 필요 없습니다.

### 기존 이력이 남은 데이터베이스를 베이스라인으로 전환할 때

이미 운영 중인 데이터베이스의 `flyway_schema_history`에 옛 마이그레이션 이력이 남아 있는 상태에서 마이그레이션 파일을 교체하면, Flyway가 기록된 체크섬과 새 파일을 대조해 기동을 중단시킵니다. `baseline-on-migrate: true`는 이력이 아예 없는 데이터베이스에만 적용되며 기존 이력의 체크섬을 재설정하지 않습니다.

전환에는 데이터베이스 재생성이 필요합니다. 데이터 손실을 감수할 수 있을 때만 가능한 절차이므로, 데이터가 쌓이기 전에 수행해야 합니다.

1. 데이터가 실제로 비어 있는지 확인합니다. 주요 테이블의 행 수를 직접 셉니다.

    ```bash
    cd /opt/bookshelf/runtime
    sudo bash -c 'set -a; . .env; set +a; mysql -h "$MYSQL_HOST" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" -t \
      -e "SELECT (SELECT COUNT(*) FROM member) AS cnt_member, (SELECT COUNT(*) FROM meeting) AS cnt_meeting, (SELECT COUNT(*) FROM chat_message) AS cnt_chat"'
    ```

2. 덤프를 남깁니다. 비어 있더라도 되돌릴 수 있는 사본을 확보합니다.

    ```bash
    bash scripts/backup-mysql.sh
    ```

3. 애플리케이션 컨테이너를 정지합니다. 스키마를 지우는 동안 애플리케이션이 붙어 있으면 계속 오류를 냅니다.

    ```bash
    docker compose --env-file .env stop app
    ```

4. 스키마를 드롭하고 다시 만듭니다. `flyway_schema_history`도 이때 함께 사라집니다.

5. 새 베이스라인이 포함된 이미지로 배포합니다. Flyway가 처음부터 전체 스키마를 세웁니다.

    ```bash
    bash scripts/deploy.sh
    ```

6. 적용 결과를 확인합니다. 로그에 마이그레이션 적용과 기동 성공이 함께 보여야 합니다.

    ```bash
    docker compose --env-file .env logs app | grep -E "Successfully applied|Started"
    ```

로컬 개발 환경도 같은 이유로 한 번 비워야 합니다. Hibernate가 만든 기존 로컬 데이터베이스에는 베이스라인 이력이 없어 그대로 두면 기동되지 않습니다.

## 11. 중요한 제한과 다음 단계

- HTTPS에는 실제 도메인과 신뢰 가능한 인증서가 필요합니다. 현재 Compose는 인증서 파일을 Nginx에 읽기 전용으로 연결하며 80번 요청을 443번으로 리다이렉트합니다. 인증서 자동 갱신은 별도로 구성해야 합니다.
- RDS MySQL은 Private Database Subnet에 배치하고 EC2 Security Group에서만 접근을 허용합니다. Redis 포트도 인터넷에 노출하지 않습니다.
- `terraform.tfstate`, `.env`, 개인 키는 Git에 올리지 않습니다.
- `.terraform.lock.hcl`은 공급자 버전을 팀에서 동일하게 재현하기 위해 Git에 포함합니다.
- 한 명만 로컬 Terraform State를 관리할 수 있습니다. 팀 공동 운영 전에는 S3 Backend와 State Locking을 구성해야 합니다.
- 개발 환경의 `terraform destroy`는 EC2와 RDS를 삭제하며 최종 RDS 스냅샷을 만들지 않습니다. 필요한 데이터는 먼저 수동 스냅샷이나 SQL 덤프로 보관해야 합니다.
- EC2를 중지해도 EBS와 공인 IPv4 비용은 남을 수 있습니다.

실제 Spring Boot 저장소가 생성되면 다음 단계는 Dockerfile, Actuator, Flyway, GitHub Actions를 이 환경에 연결하는 것입니다.
