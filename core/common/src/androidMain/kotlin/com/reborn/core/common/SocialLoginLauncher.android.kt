package com.reborn.core.common

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.kakao.sdk.user.UserApiClient

private const val TAG = "SocialLogin"

@Composable
actual fun rememberSocialLoginLauncher(
    onResult: (provider: String, token: String) -> Unit,
    onError: (Throwable) -> Unit,
): SocialLoginLauncher {
    val context = LocalContext.current

    return remember {
        object : SocialLoginLauncher {
            override fun launch(socialType: SocialType) {
                when (socialType) {
                    SocialType.KAKAO -> loginWithKakao(context, onResult, onError)
                    SocialType.GOOGLE -> onError(
                        UnsupportedOperationException("구글 로그인은 아직 준비 중입니다."),
                    )
                }
            }
        }
    }
}

// 카카오톡 앱 전환(loginWithKakaoTalk) 경로는 에뮬레이터/미연동 계정에서 실패 후
// loginWithKakaoAccount로 재시도하며 동의 화면이 두 번 뜨는 문제가 있어,
// 카카오계정(웹) 로그인 하나로 통일함 — 필요해지면 loginWithKakaoTalk 우선 시도를 다시 추가.
private fun loginWithKakao(
    context: Context,
    onResult: (String, String) -> Unit,
    onError: (Throwable) -> Unit,
) {
    Log.d(TAG, "카카오 로그인 시작")
    Toast.makeText(context, "[디버그] 카카오 로그인 시작", Toast.LENGTH_SHORT).show()
    UserApiClient.instance.loginWithKakaoAccount(context) { token, error ->
        when {
            error != null -> {
                Log.e(TAG, "카카오 로그인 실패: ${error.message}", error)
                Toast.makeText(context, "[디버그] 카카오 로그인 실패: ${error.message}", Toast.LENGTH_LONG).show()
                onError(error)
            }
            token != null -> {
                Log.d(TAG, "카카오 로그인 성공, accessToken 획득")
                Toast.makeText(context, "[디버그] 카카오 로그인 성공, 서버 호출 시작", Toast.LENGTH_SHORT).show()
                onResult("KAKAO", token.accessToken)
            }
        }
    }
}
