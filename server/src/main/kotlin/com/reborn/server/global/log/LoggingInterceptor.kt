package com.reborn.server.global.log

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class LoggingInterceptor : HandlerInterceptor {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis())
        val clientIp = request.getHeader("X-Forwarded-For")?.substringBefore(",")?.trim() ?: request.remoteAddr
        log.info(">>> {} {} from {}", request.method, request.requestURI, clientIp)
        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?,
    ) {
        val startTime = request.getAttribute(START_TIME_ATTR) as? Long
        val elapsed = if (startTime != null) System.currentTimeMillis() - startTime else -1L
        if (ex != null) {
            log.error("<<< {} {} {} ({}ms)", request.method, request.requestURI, response.status, elapsed, ex)
        } else {
            log.info("<<< {} {} {} ({}ms)", request.method, request.requestURI, response.status, elapsed)
        }
    }

    companion object {
        private const val START_TIME_ATTR = "startTime"
    }
}
