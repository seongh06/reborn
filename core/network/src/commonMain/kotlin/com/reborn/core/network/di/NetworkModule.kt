package com.reborn.core.network.di

import com.reborn.core.datastore.TokenLocalDataSource
import com.reborn.core.network.BuildConfig
import com.reborn.core.network.model.BaseResponse
import com.reborn.core.network.model.request.auth.RefreshRequest
import com.reborn.core.network.model.response.auth.RefreshResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.module


val networkModule = module {

    single {
        Json {
            isLenient = true
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    }

    single(named("noAuth")) {
        createHttpClient(get(), get())
    }

    // accessToken/refreshToken을 자동으로 붙이고, 401 응답 시 /api/auth/refresh로 조용히
    // 재발급받아 재시도한다(#121). 공기계 앱은 로그인을 하지 않으므로(#113) TokenLocalDataSource의
    // isAerometer 플래그를 먼저 확인해 그 경우엔 아예 헤더를 붙이지 않는다.
    single(named("auth")) {
        createAuthHttpClient(get(), get(), get())
    }
}


fun Scope.createHttpClient(
    engine: HttpClientEngineFactory<*>,
    json: Json,
) = HttpClient(engine) {
    install(HttpTimeout) {
        requestTimeoutMillis = 30_000L
        connectTimeoutMillis = 15_000L
        socketTimeoutMillis = 30_000L
    }

    install(ContentNegotiation) {
        json(json)
    }

    install(Logging) {
        level = if (BuildConfig.DEBUG) LogLevel.INFO else LogLevel.NONE

        logger = object : Logger {
            override fun log(message: String) {
                val logMessage = if (message.length > 1000) message.take(1000) + "…(truncated)" else message
                println("Ktor Log: $logMessage")
            }
        }
    }

    defaultRequest {
        url(BuildConfig.BASE_URL)
        header(HttpHeaders.ContentType, "application/json")
    }
}

fun Scope.createAuthHttpClient(
    engine: HttpClientEngineFactory<*>,
    json: Json,
    tokenLocalDataSource: TokenLocalDataSource,
) = createHttpClient(engine, json).config {
    install(Auth) {
        bearer {
            sendWithoutRequest { true }

            loadTokens {
                if (tokenLocalDataSource.isAerometerDevice()) return@loadTokens null
                val accessToken = tokenLocalDataSource.getAccessToken()
                val refreshToken = tokenLocalDataSource.getRefreshToken()
                if (accessToken.isNullOrBlank() || refreshToken.isNullOrBlank()) null
                else BearerTokens(accessToken, refreshToken)
            }

            refreshTokens {
                if (tokenLocalDataSource.isAerometerDevice()) return@refreshTokens null
                val currentRefreshToken = tokenLocalDataSource.getRefreshToken() ?: return@refreshTokens null

                // client는 이 Auth 플러그인이 적용되지 않은 별도 인스턴스라, 재발급 요청 자체가
                // 401을 받아도 재귀적으로 refreshTokens를 또 호출하지 않는다(Ktor가 보장).
                val response = client.post("/api/auth/refresh") {
                    markAsRefreshTokenRequest()
                    contentType(ContentType.Application.Json)
                    setBody(RefreshRequest(currentRefreshToken))
                }

                if (!response.status.isSuccess()) {
                    tokenLocalDataSource.clear()
                    return@refreshTokens null
                }

                val body = response.body<BaseResponse<RefreshResponse>>()
                val data = body.data
                if (!body.isSuccess || data == null) {
                    tokenLocalDataSource.clear()
                    return@refreshTokens null
                }

                tokenLocalDataSource.saveTokens(data.accessToken, data.refreshToken)
                BearerTokens(data.accessToken, data.refreshToken)
            }
        }
    }
}
