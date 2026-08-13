# 기여 가이드 (Contributing)

책장사이 백엔드 협업 규칙. 브랜치 전략 · 커밋 · PR · 코드 리뷰 · 코드 스타일을 정의한다.

---

## 1. 브랜치 전략

| 브랜치       | 역할                       | 분기 원본 | 머지 대상 |
| ------------ | -------------------------- | --------- | --------- |
| `main`       | 운영 배포 브랜치           | —         | —         |
| `develop`    | 개발 통합 브랜치           | `main`    | `main`    |
| `feature/*`  | 기능 추가                  | `develop` | `develop` |
| `fix/*`      | 버그 수정                  | `develop` | `develop` |
| `refactor/*` | 동작 변화 없는 구조 개선   | `develop` | `develop` |
| `chore/*`    | 설정·문서·의존성 등의 작업 | `develop` | `develop` |
| `docs/*`     | 문서만 변경                | `develop` | `develop` |
| `release/*`  | 별도 QA가 필요한 배포 준비 | `develop` | `main`    |

**브랜치 이름:** 일반 작업은 `<타입>/<이슈번호>-<간단설명>` (kebab-case), 배포 준비는
`release/<yyyy-mm-dd>` 형식을 사용한다.

```text
feature/12-kakao-book-search
fix/260-method-not-allowed-500
refactor/264-book-detail-description-full
chore/278-improve-swagger-docs
docs/301-update-api-guide
release/2026-08-13
```

- 설명은 영어 kebab-case로 작성한다.
- 버그 수정도 운영 브랜치에서 직접 분기하지 않고 `develop`에서 작업한다.
- 일반 배포는 `develop`에서 `main`으로 PR을 생성한다. 별도 QA 기간이 필요할 때만
  `release/<yyyy-mm-dd>` 브랜치를 사용한다.

---

## 2. 커밋 컨벤션 — Conventional Commits

형식: `<type>(<scope>): <description>` — type과 scope는 영어 식별자를 사용한다. description은
한국어 또는 영어 중 한 언어로 일관되게 작성한다.

```text
feat(ai): 오늘의 추천 후보를 KDC 800·100으로 한정
fix(notification): 알림 읽음 처리 멱등성 개선
refactor(book): 상세조회 description 원문 그대로 반환
docs(contributing): 협업 컨벤션 갱신
feat(auth): replace fake-signup with memberId-based fake-login
```

**type 목록**

| type       | 용도                            |
| ---------- | ------------------------------- |
| `feat`     | 새 기능                         |
| `fix`      | 버그 수정                       |
| `docs`     | 문서만 변경                     |
| `style`    | 포맷·세미콜론 등 동작 무관 변경 |
| `refactor` | 기능 변화 없는 코드 개선        |
| `test`     | 테스트 추가·수정                |
| `chore`    | 빌드·설정·의존성 등 잡무        |
| `build`    | 빌드 시스템·의존성 변경         |
| `ci`       | CI 설정 변경                    |
| `perf`     | 성능 개선                       |

- **description:** 변경 내용을 간결하게 작성하고 마침표를 붙이지 않는다.
  - 한국어는 `~ 추가`, `~ 수정`, `~ 개선`, `~ 제거`처럼 명사형으로 끝낸다.
  - 영어는 명령형 현재시제로 작성하고 소문자로 시작한다 (`add`, `fix`, `replace`).
- **scope:** 선택. 도메인 단위 권장 (`book`, `meeting`, `auth`, `chat`, `ai`, `notification`).
- 커밋 본문(body)이 필요하면 제목 아래 한 줄 띄우고 상세 설명.
- GitHub가 생성하는 `Merge pull request ...` 커밋은 위 형식의 예외다.

---

## 3. 이슈 · PR · 머지

### 단위 규칙

- **1 이슈 → 1 PR → N 커밋.** 하나의 작업 단위는 이슈로 만들고, 그 이슈를 해결하는 브랜치에서 여러 커밋을 쌓아 PR 하나로 올린다.
- 이슈/PR 템플릿은 자동으로 채워진다(`.github/`). 빈칸을 채워 작성한다.
- PR 본문에 `Closes #<이슈번호>`를 넣어 머지 시 이슈가 자동 종료되게 한다.

### 이슈 제목

`[Type] 한글 제목` 형식을 사용한다.

| Type         | 사용 기준               | 연결 브랜치  |
| ------------ | ----------------------- | ------------ |
| `[Feature]`  | 새 기능                 | `feature/*`  |
| `[Bug]`      | 잘못된 동작 수정        | `fix/*`      |
| `[Refactor]` | 동작 변화 없는 구조 개선 | `refactor/*` |
| `[Chore]`    | 설정·문서·의존성 작업   | `chore/*`    |

이슈 본문에는 최소한 **배경/목적**, **작업 내용**, **완료 조건**을 작성한다. 필요한 경우 검증 계획과
참고 자료를 추가한다.

### PR 제목과 본문

- 일반 PR 제목은 연결된 이슈와 같은 prefix를 사용한다.
- 배포 PR 제목은 `[Deploy] <source> -> main 배포`로 작성한다. `<source>`에는 `develop` 또는
  실제 `release/*` 브랜치명을 넣는다.
- PR 본문에는 개요, 작업 내용, 영향 범위, 관련 이슈, 실제 검증 결과를 작성한다.
- 배포 PR은 여러 이슈를 포함하므로 `Closes` 대신 포함된 PR·이슈를 나열할 수 있다.

### 머지 방식 — Merge commit (`--no-ff`)

- GitHub PR 머지 버튼에서 **"Create a merge commit"**을 사용한다. (Squash / Rebase 사용 금지)
- 작업 커밋들이 그대로 보존되고, PR 단위가 머지 커밋으로 묶여 히스토리에 남는다.
- 작업 브랜치 → `develop`, 배포 시 `develop` 또는 `release/*` → `main` 순서로 반영한다.

### 머지 조건

- 일반 작업 PR은 사람 리뷰어 또는 CodeRabbit 등 AI 리뷰어의 리뷰를 **최소 1회** 받아야 한다.
- `develop` → `main` 배포 PR은 `develop`에 포함된 개별 작업 PR에서 리뷰가 완료된 것으로 간주하여
  별도의 코드 리뷰 없이 머지할 수 있다.
- AI 리뷰가 등록된 경우, 모든 리뷰에 대해 **수정하거나 미반영 사유를 댓글로 작성한 후 스레드를 Resolve**해야 한다.
- CI `./gradlew spotlessCheck build`가 통과해야 한다.
- 충돌이 없어야 하며, 충돌 발생 시 PR 작성자가 최신 base 브랜치를 반영해 해결한다.

즉, **CodeRabbit 리뷰가 완료되고 모든 리뷰 의견이 처리되었다면 사람의 별도 승인 없이도 머지할 수 있습니다.**

---

## 4. 코드 리뷰 컨벤션

**작성자**

- PR은 작게. 리뷰 가능한 크기로 쪼갠다.
- 셀프 리뷰 후 올린다. 무엇을·왜 바꿨는지 PR 본문에 설명한다.

**리뷰어**

- 로직·설계·엣지케이스에 집중한다. 포맷/스타일 지적은 하지 않는다 — Spotless가 강제하므로 리뷰 대상이 아니다.
- 근거와 대안을 함께 제시하고, 사람이 아니라 코드를 대상으로 코멘트한다.

**작성자 응답**

- 모든 코멘트에 반영 또는 답변으로 응답한 뒤 재요청한다.
- 리뷰 반영 커밋도 Conventional Commits 형식을 따른다.

---

## 5. 코드 스타일

- **Google Java Format**을 **Spotless**로 강제한다. 사람이 눈으로 맞추지 않는다.
- 커밋/PR 전에 실행:

```bash
./gradlew spotlessApply   # 자동 포맷
./gradlew spotlessCheck   # 검사 (CI에서도 수행 — 실패 시 머지 불가)
```

- 에디터 기본값은 `.editorconfig`가 맞춰준다(들여쓰기 등).
