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

                    // QR 웹페이지에서 비로그인 방문자가 제출 — 조회/상태변경은 anyRequest().authenticated()로 보호
                    .requestMatchers(HttpMethod.POST, "/api/feedback").permitAll()

                    // WebSocket 핸드셰이크(HTTP) 자체는 permitAll — 실제 인증은 STOMP CONNECT 프레임에서
                    // WebSocketAuthChannelInterceptor가 담당 (관리자: JWT / 공기계: deviceKey+appToken)
                    .requestMatchers("/ws/control/**").permitAll()

                    .anyRequest().authenticated()
            }
            .addFilterBefore(JwtFilter(jwtProvider), UsernamePasswordAuthenticationFilter::class.java)
            .build()
}