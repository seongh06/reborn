# 📌 Git Commit Message Convention

이 프로젝트는 대형 작업(Epic)을 서브 이슈로 나누어 진행하며, AI(Claude Code, CodeRabbitAI) 협업 효율을 극대화하기 위해 엄격한 커밋 메시지 규칙을 적용함.

## 1. 기본 구조 및 규칙
- 모든 커밋 메시지는 문장 형태가 아닌 **담백한 명사형**으로 종결함.
- 브랜치 및 서브 이슈 식별 번호를 타입 뒤에 명시함.

```text
{영역}/{타입}#{Epic번호}-{Sub이슈번호}: {작업 내용 명사형 종결}
```

## 2. 구성 요소

| 요소 | 설명 | 예시 |
|------|------|------|
| 영역 | 작업 대상 모듈 (대문자) | `SERVER`, `APP` |
| 타입 | 작업 종류 (대문자) | `FEAT`, `FIX`, `REFACTOR`, `CHORE`, `DOCS`, `TEST` |
| Epic번호 | 상위 이슈(Epic) 번호 | `1`, `2`, `3` |
| Sub이슈번호 | 하위 이슈 순번 | `1`, `2`, `3` |

## 3. 타입별 정의 및 예시

| 타입 | 정의 | 예시 |
|------|------|------|
| `FEAT` | 새로운 기능 구현 | `SERVER/FEAT#1-4: JWT 인프라 및 보안 필터 구현` |
| `FIX` | 버그 수정 | `SERVER/FIX#2-1: 토큰 만료 예외 처리 오류 수정` |
| `REFACTOR` | 기능 변경 없는 코드 구조 개선 | `SERVER/REFACTOR#1-5: SecurityConfig 구조 개선` |
| `CHORE` | 빌드 설정, 의존성, 환경변수 등 | `SERVER/CHORE#1-1: JJWT 라이브러리 의존성 추가` |
| `DOCS` | 문서 생성 및 수정 | `SERVER/DOCS#1-2: Swagger 설정 가이드라인 문서 작성` |
| `TEST` | 테스트 코드 추가 및 수정 | `SERVER/TEST#1-4: JwtProvider 단위 테스트 작성` |
