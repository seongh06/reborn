package com.reborn.server.global.log

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
class RequestIdFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val raw = request.getHeader(REQUEST_ID_HEADER)
        val requestId = if (raw != null && VALID_ID_PATTERN.matches(raw)) raw else UUID.randomUUID().toString()
        MDC.put(REQUEST_ID_KEY, requestId)
        response.setHeader(REQUEST_ID_HEADER, requestId)
        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(REQUEST_ID_KEY)
        }
    }

    companion object {
        const val REQUEST_ID_HEADER = "X-Request-Id"
        const val REQUEST_ID_KEY = "requestId"
        private val VALID_ID_PATTERN = Regex("^[A-Za-z0-9\\-_]{1,64}$")
    }
}
