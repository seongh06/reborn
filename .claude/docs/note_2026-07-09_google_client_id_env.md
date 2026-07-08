# 📌 운영 환경변수 추가 기록

**이슈 #98: Google 소셜 로그인 SDK 연동**

⚠️ 아래 작업은 Claude가 직접 실행하지 않았으며, 운영자가 홈서버(`gram-server`)에 직접 SSH 접속하여 수동으로 적용했습니다.

## 배경

`GoogleAuthClient.verify()`가 idToken의 `aud`를 서버 설정값과 비교해 검증하는데(#64, CodeRabbit 반영으로 fail-closed 전환 — `oauth.google.client-id` 미설정 시 항상 실패), 클라이언트(core:common `SocialLoginLauncher.android.kt`)에서 Google Sign-In이 실제로 idToken을 발급받기 시작하면서 운영 서버에도 대응하는 `GOOGLE_CLIENT_ID`가 필요해졌습니다.

## 적용한 변경

`server/deployment/env/.env`(홈서버 로컬 파일, git 미추적)에 아래 항목 추가:

```
GOOGLE_CLIENT_ID=<Google Cloud Console에서 발급한 Web 애플리케이션 타입 클라이언트 ID>
```

- 이 값은 안드로이드 앱의 `local.properties`(`GOOGLE_WEB_CLIENT_ID`, git 미추적)와 **반드시 동일한 값**이어야 함 — 클라이언트가 요청한 idToken의 `aud`와 서버가 검증하는 `aud`가 일치해야 하기 때문. Android 전용 클라이언트 ID(패키지명+SHA-1로 등록한 것)는 이 용도로 쓰지 않음.
- 적용 후 이미지 재빌드 없이 컨테이너 재시작만으로 반영됨:
  ```bash
  docker compose -f server/deployment/docker/docker-compose.yml --project-directory . up -d backend-app
  ```

## 참고

- 이 파일 자체는 실제 클라이언트 ID 값을 담지 않음(공개 저장소이므로) — 실제 값은 홈서버 `.env`와 Google Cloud Console에서만 확인 가능
- 비슷한 패턴: `migration_2026-07-09_user_email_nullable.sql`(#92, 운영 DB 수동 마이그레이션 기록)
