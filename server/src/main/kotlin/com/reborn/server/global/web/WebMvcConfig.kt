package com.reborn.server.global.web

import com.reborn.server.global.log.LoggingInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

// CORS는 global/token/SecurityConfig의 corsConfigurationSource()가 전담한다.
// Spring Security가 활성화된 상태에서 CORS를 여기(WebMvcConfigurer)에도 중복 등록하면
// 어느 쪽 설정이 실제로 적용되는지 헷갈리고, Security 필터 체인이 이 설정보다 먼저
// 요청을 가로채 preflight(OPTIONS)에 헤더가 안 붙는 문제가 생길 수 있어 한 곳으로 일원화함.
@Configuration
class WebMvcConfig(
    private val loggingInterceptor: LoggingInterceptor,
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(loggingInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html")
    }
}
