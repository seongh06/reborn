package com.reborn.core.network.di

import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider

// Ktor의 Auth { bearer { loadTokens {...} } }는 loadTokens() 결과를 최초 1회만 호출해서
// 인메모리에 캐시하고, 이후 요청마다 다시 부르지 않는다 - "auth" HttpClient가 앱 프로세스
// 생명주기 동안 유지되는 싱글턴이라, 로그아웃/탈퇴 후 DataStore를 비우거나 재로그인으로
// 새 토큰을 저장해도 Ktor는 예전 토큰을 계속 씀. 게다가 자동 재발급(refreshTokens)은 401
// 응답에서만 발동하는데, 탈퇴된 userId로 요청하면 서버는 JWT 서명은 유효하다고 보고 인증은
// 통과시킨 뒤 404("존재하지 않는 회원 정보입니다")를 돌려줘서 재발급 로직 자체가 안 걸린다(#251).
// 토큰이 명시적으로 바뀌는 시점(로그인 성공/로그아웃/탈퇴)마다 이걸 호출해 캐시를 비워야
// 다음 요청에서 loadTokens()가 DataStore의 최신 값을 다시 읽는다.
class AuthTokenCacheInvalidator(
    private val authClient: HttpClient,
) {
    fun invalidate() {
        authClient.authProvider<BearerAuthProvider>()?.clearToken()
    }
}
