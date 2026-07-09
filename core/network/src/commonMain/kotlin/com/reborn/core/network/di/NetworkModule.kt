package com.reborn.core.network.di

import com.reborn.core.network.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
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

    // 요청마다 호출부에서 Authorization 헤더를 직접 실어 보냄 (자동 첨부 인터셉터는 추후 추가 예정)
    single(named("auth")) {
        createHttpClient(get(), get())
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