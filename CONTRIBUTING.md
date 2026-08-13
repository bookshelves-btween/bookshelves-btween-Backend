# 기여 가이드 (Contributing)

책장사이 백엔드 협업 규칙. 브랜치 전략 · 커밋 · PR · 코드 리뷰 · 코드 스타일을 정의한다.

---

## 1. 브랜치 전략 — Git Flow

| 브랜치      | 역할                                                 | 분기 원본 | 머지 대상          |
| ----------- | ---------------------------------------------------- | --------- | ------------------ |
| `main`      | 배포(프로덕션). 항상 배포 가능 상태. 릴리스마다 태그 | —         | —                  |
| `develop`   | 개발 통합 브랜치. 모든 기능이 여기로 모임            | `main`    | —                  |
| `feature/*` | 기능 개발                                            | `develop` | `develop`          |
| `release/*` | 릴리스 준비(버전 확정·QA)                            | `develop` | `main` + `develop` |
| `hotfix/*`  | 운영 긴급 수정                                       | `main`    | `main` + `develop` |

**브랜치 이름:** `<타입>/<이슈번호>-<간단설명>` (kebab-case)

```
feature/12-kakao-book-search
feature/20-meeting-state-scheduler
hotfix/45-oauth-token-refresh
```

---

## 2. 커밋 컨벤션 — Conventional Commits

형식: `<type>(<scope>): <description>` — 영어로 작성.

```text
feat(book): add ISBN normalization on lazy persistence
fix(auth): handle expired kakao access token
docs(readme): document required env vars
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

- **description:** 명령형 현재시제, 소문자 시작, 마침표 없음 (`add`, `fix` — not `added`, `Fixes`).
- **scope:** 선택. 도메인 단위 권장 (`book`, `meeting`, `auth`, `chat`, `ai`, `notification`).
- 커밋 본문(body)이 필요하면 제목 아래 한 줄 띄우고 상세 설명.

---

## 3. 이슈 · PR · 머지

### 단위 규칙

- **1 이슈 → 1 PR → N 커밋.** 하나의 작업 단위는 이슈로 만들고, 그 이슈를 해결하는 브랜치에서 여러 커밋을 쌓아 PR 하나로 올린다.
- 이슈/PR 템플릿은 자동으로 채워진다(`.github/`). 빈칸을 채워 작성한다.
- PR 본문에 `Closes #<이슈번호>`를 넣어 머지 시 이슈가 자동 종료되게 한다.

### 머지 방식 — Merge commit (`--no-ff`)

- GitHub PR 머지 버튼에서 **"Create a merge commit"**을 사용한다. (Squash / Rebase 사용 금지)
- feature의 커밋들이 그대로 보존되고, PR 단위가 머지 커밋으로 묶여 히스토리에 남는다.
- `feature/*` → `develop`, `release/*`·`hotfix/*` → `main`(+`develop` 반영).

### 머지 조건

- 사람 리뷰어 또는 CodeRabbit 등 AI 리뷰어의 리뷰를 **최소 1회** 받아야 한다.
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
