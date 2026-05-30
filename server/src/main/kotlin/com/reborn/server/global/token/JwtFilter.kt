package com.reborn.server.global.token

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtFilter(
    private val jwtProvider: JwtProvider,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = resolveToken(request)
        if (token != null && jwtProvider.validate(token)) {
            val userId = jwtProvider.getUserId(token)
            MDC.put(USER_ID_MDC_KEY, userId.toString())
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken(userId, null, listOf(SimpleGrantedAuthority("ROLE_USER")))
        }
        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(USER_ID_MDC_KEY)
            SecurityContextHolder.clearContext()
        }
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val bearer = request.getHeader(AUTHORIZATION_HEADER) ?: return null
        return if (bearer.startsWith(BEARER_PREFIX)) bearer.removePrefix(BEARER_PREFIX) else null
    }

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
        const val USER_ID_MDC_KEY = "userId"
    }
}
