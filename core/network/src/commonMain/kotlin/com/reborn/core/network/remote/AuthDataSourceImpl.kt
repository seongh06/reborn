package com.reborn.core.network.remote

import com.reborn.core.network.datasource.AuthDataSource
import com.reborn.core.network.model.ApiResponse
import com.reborn.core.network.model.request.auth.FcmTokenUpdateRequest
import com.reborn.core.network.model.request.auth.LoginRequest
import com.reborn.core.network.model.request.auth.UpdateProfileRequest
import com.reborn.core.network.model.response.auth.LoginResponse
import com.reborn.core.network.model.response.auth.MeResponse
import com.reborn.core.network.util.asApiResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

// "auth" 클라이언트(#121) 사용 - Authorization 헤더는 Auth 플러그인이 자동으로 붙인다.
// login은 아직 토큰이 없어 자연히 헤더 없이 나간다.
class AuthDataSourceImpl(
private val httpClient: HttpClient
): AuthDataSource {

    override suspend fun login(request: LoginRequest): ApiResponse<LoginResponse> = runCatching {
        httpClient.post("/api/auth/login") {
            setBody(request)
        }
    }.asApiResponse()

    override suspend fun logout(): ApiResponse<Unit?> = runCatching {
        httpClient.post("/api/auth/logout")
    }.asApiResponse()

    override suspend fun updateFcmToken(request: FcmTokenUpdateRequest): ApiResponse<Unit?> = runCatching {
        httpClient.patch("/api/auth/fcm") {
            setBody(request)
        }
    }.asApiResponse()

    override suspend fun getMe(): ApiResponse<MeResponse> = runCatching {
        httpClient.get("/api/auth/me")
    }.asApiResponse()

    override suspend fun updateProfile(request: UpdateProfileRequest): ApiResponse<MeResponse> = runCatching {
        httpClient.patch("/api/auth/me") {
            setBody(request)
        }
    }.asApiResponse()

    // defaultRequest가 붙이는 "application/json" 헤더와 무관하게 OutgoingContent(멀티파트 바디)의
    // Content-Type이 실제 전송 시 우선 적용되는 Ktor 표준 동작을 이용 - submitFormWithBinaryData가
    // 그 OutgoingContent(MultiPartFormDataContent)를 만들어준다.
    override suspend fun updateProfileImage(bytes: ByteArray, fileName: String, mimeType: String): ApiResponse<MeResponse> = runCatching {
        httpClient.submitFormWithBinaryData(
            url = "/api/auth/me/profile-image",
            formData = formData {
                append(
                    key = "image",
                    value = bytes,
                    headers = Headers.build {
                        append(HttpHeaders.ContentType, mimeType)
                        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                    }
                )
            }
        )
    }.asApiResponse()

    override suspend fun withdraw(): ApiResponse<Unit?> = runCatching {
        httpClient.delete("/api/auth/me")
    }.asApiResponse()
}