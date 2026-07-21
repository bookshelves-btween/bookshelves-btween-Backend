# 책장사이 — Backend

**책장사이**의 백엔드 서버. 독서현황 기록과 온라인 독서모임을 제공하는 iOS 앱의 API 서버다.

## 기술 스택

| 구분           | 사용                                                                  |
| -------------- | --------------------------------------------------------------------- |
| Language       | Java 21                                                               |
| Framework      | Spring Boot 4.1.0                                                     |
| Build          | Gradle (Groovy DSL)                                                   |
| DB             | MySQL                                                                 |
| Cache / 실시간 | Redis (검색 캐시 · 세션/토큰 · 모임 타이머 TTL · 채팅 Pub/Sub)        |
| 인증           | Spring Security + OAuth2 (구글 · 카카오)                              |
| 실시간 채팅    | WebSocket                                                             |
| 외부 연동      | 카카오 책 검색 API · 도서관 정보나루(KDC 분류) · Gemini(AI 토론·요약) |

## 사전 요구사항

- JDK 21
- 로컬 실행 시 MySQL, Redis 인스턴스

## 실행

```bash
# 빌드
./gradlew build

# 실행
./gradlew bootRun
```

기본 포트는 `8080`.

## 환경 변수

`application.yml`의 값은 모두 환경 변수로 주입한다. 로컬에서는 `.env` 등에 채워 넣는다(`.env`는 커밋 금지).

| 변수                                                              | 설명                 |
| ----------------------------------------------------------------- | -------------------- |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USERNAME` / `DB_PASSWORD` | MySQL 접속           |
| `REDIS_HOST` / `REDIS_PORT`                                       | Redis 접속           |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET`                       | 구글 OAuth2          |
| `KAKAO_CLIENT_ID` / `KAKAO_CLIENT_SECRET`                         | 카카오 OAuth2 로그인 |
| `KAKAO_REST_API_KEY`                                              | 카카오 책 검색 API   |
| `DATA4LIBRARY_AUTH_KEY`                                           | 도서관 정보나루 API  |
| `GEMINI_API_KEY`                                                  | Gemini API           |
| `JWT_SECRET`                                                      | JWT 서명 키 (HS256, 최소 32바이트 이상 랜덤 문자열 필수) |
| `CORS_ALLOWED_ORIGINS`                                            | 허용할 CORS Origin (콤마로 여러 개 지정, 환경별로 다르게 설정) |

## 코드 포맷

Google Java Format을 Spotless로 강제한다. 커밋 전 실행:

```bash
./gradlew spotlessApply   # 자동 포맷
./gradlew spotlessCheck   # 포맷 검사 (CI에서도 수행)
```

## 기여 가이드

브랜치 전략 · 커밋 컨벤션 · PR/리뷰 규칙은 [`CONTRIBUTING.md`](CONTRIBUTING.md) 참고.
