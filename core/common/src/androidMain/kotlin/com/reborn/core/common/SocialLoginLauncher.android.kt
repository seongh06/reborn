package com.reborn.core.common

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "SocialLogin"

@Composable
actual fun rememberSocialLoginLauncher(
    onResult: (provider: String, token: String) -> Unit,
    onError: (Throwable) -> Unit,
): SocialLoginLauncher {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    return remember {
        object : SocialLoginLauncher {
            override fun launch(socialType: SocialType) {
                when (socialType) {
                    SocialType.KAKAO -> loginWithKakao(context, onResult, onError)
                    SocialType.GOOGLE -> loginWithGoogle(context, coroutineScope, onResult, onError)
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

private fun loginWithGoogle(
    context: Context,
    coroutineScope: CoroutineScope,
    onResult: (String, String) -> Unit,
    onError: (Throwable) -> Unit,
) {
    Log.d(TAG, "구글 로그인 시작")
    Toast.makeText(context, "[디버그] 구글 로그인 시작", Toast.LENGTH_SHORT).show()

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    coroutineScope.launch {
        try {
            val credential = CredentialManager.create(context)
                .getCredential(context, request)
                .credential

            if (credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                onError(IllegalStateException("예상하지 못한 Credential 타입입니다: ${credential.type}"))
                return@launch
            }

            val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            Log.d(TAG, "구글 로그인 성공, idToken 획득")
            Toast.makeText(context, "[디버그] 구글 로그인 성공, 서버 호출 시작", Toast.LENGTH_SHORT).show()
            onResult("GOOGLE", idToken)
        } catch (e: GetCredentialException) {
            Log.e(TAG, "구글 로그인 실패: ${e.message}", e)
            Toast.makeText(context, "[디버그] 구글 로그인 실패: ${e.message}", Toast.LENGTH_LONG).show()
            onError(e)
        } catch (e: GoogleIdTokenParsingException) {
            Log.e(TAG, "구글 idToken 파싱 실패: ${e.message}", e)
            onError(e)
        }
    }
}
