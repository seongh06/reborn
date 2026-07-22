package com.reborn.server.global.token

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtProvider: JwtProvider,
    private val jwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint,
    private val jwtAccessDeniedHandler: JwtAccessDeniedHandler,
    @param:Value("\${cors.allowed-origins}") private val allowedOrigins: Array<String>,
) {

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val origins: List<String> = allowedOrigins.toList()

        val configuration = CorsConfiguration()
        configuration.allowedOriginPatterns = origins
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true
        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/api/**", configuration)
        source.registerCorsConfiguration("/ws/**", configuration)
        return source
    }

    @Bean
    fun filterChain(http: HttpSecurity, corsConfigurationSource: CorsConfigurationSource): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource) }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling {
                it.authenticationEntryPoint(jwtAuthenticationEntryPoint)
                it.accessDeniedHandler(jwtAccessDeniedHandler)
            }
            .authorizeHttpRequests { auth ->
                auth

                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

                    .requestMatchers("/api/auth/logout").authenticated()
                    .requestMatchers(HttpMethod.PATCH, "/api/auth/fcm").authenticated()
                    .requestMatchers("/api/auth/**").permitAll()

                    // Arduino 기기 인증 (Device Key 헤더) — JWT 불필요
                    .requestMatchers("/api/metric/collect", "/api/metric/current").permitAll()

                    // 공기계 앱은 별도 로그인을 하지 않음 — 페어링 코드 자체가 유일한 인가 수단(JWT 불필요)
                    .requestMatchers(HttpMethod.POST, "/api/device/pairing").permitAll()

                    // QR 웹페이지에서 비로그인 방문자가 제출 — 조회/상태변경은 anyRequest().authenticated()로 보호
                    .requestMatchers(HttpMethod.POST, "/api/feedback").permitAll()

                    // AI 스피커(#142)는 X-Device-Id로만 인증하고 JWT를 보내지 않는다 — "/api/feedback"
                    // permitAll은 하위 경로("/voice")에 매칭되지 않아 누락돼 있었음.
                    .requestMatchers(HttpMethod.POST, "/api/feedback/voice").permitAll()

                    // 판매용 기기 시리얼 배치 발급(#147) 및 발급+장소 등록 동시 처리(#150) — 장소/ADMIN과
                    // 무관, X-Operator-Key로 자체 인가. 정확한 경로만 매칭되므로 두 경로 모두 명시해야 함.
                    .requestMatchers(HttpMethod.POST, "/api/device/serials").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/device/serials/register").permitAll()

                    // WebSocket 핸드셰이크(HTTP) 자체는 permitAll — 실제 인증은 STOMP CONNECT 프레임에서
                    // WebSocketAuthChannelInterceptor가 담당 (관리자: JWT / 공기계: deviceKey+appToken)
                    .requestMatchers("/ws/control/**").permitAll()

                    // SmartThings가 사용자 동의 후 브라우저를 직접 리다이렉트하는 엔드포인트 — JWT 불필요.
                    // state 파라미터(Redis 저장, #130)가 유일한 인가 수단이며 /authorize는 그대로 JWT 필요.
                    .requestMatchers("/api/smartthings/oauth/callback").permitAll()

                    .anyRequest().authenticated()
            }
            .addFilterBefore(JwtFilter(jwtProvider), UsernamePasswordAuthenticationFilter::class.java)
            .build()
}