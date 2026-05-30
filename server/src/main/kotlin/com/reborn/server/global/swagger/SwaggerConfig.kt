package com.reborn.server.global.swagger

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {

    @Bean
    fun openAPI(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("ReBorn API")
                .description("스마트 실내 환경 모니터링 및 IoT 제어 플랫폼 API 문서")
                .version("v1.0.0"),
        )
        .components(
            Components().addSecuritySchemes(
                SECURITY_SCHEME_NAME,
                SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT AccessToken을 입력하세요. (Bearer 접두사 제외)"),
            ),
        )

    companion object {
        private const val SECURITY_SCHEME_NAME = "bearerAuth"
    }
}
