package com.reborn.core.network.di

import com.reborn.core.network.BuildConfig
import io.ktor.client.HttpClient
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
        createHttpClient(get(), get(), withAuth = false)
    }

    single(named("auth")) {
        createHttpClient(get(), get(), withAuth = true)
    }
}


fun Scope.createHttpClient(
    engine: HttpClientEngineFactory<*>,
    json: Json,
    withAuth: Boolean
) = HttpClient(engine) {
    install(HttpTimeout) {
        requestTimeoutMillis = 900_000L
        connectTimeoutMillis = 900_000L
        socketTimeoutMillis = 900_000L
    }

    install(ContentNegotiation) {
        json(json)
    }

    install(Logging) {
        level = if (BuildConfig.DEBUG) LogLevel.INFO else LogLevel.NONE

        logger = object : Logger {
            override fun log(message: String) {
                if (message.length > 1000) return
                println("Ktor Log: $message")
            }
        }
    }

/*    if (withAuth) {
        install(Auth) {
            bearer {
                loadTokens {
                    val repository = get<AuthRepository>()
                    val token = repository.getValidAccessToken()
                    if (token != null) BearerTokens(token, "") else null
                }
            }
        }
    }*/

    defaultRequest {
        url(BuildConfig.BASE_URL)
        header(HttpHeaders.ContentType, "application/json")
    }
}