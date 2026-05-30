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
        log.info(">>> {} {} from {}", request.method, request.requestURI, request.remoteAddr)
        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?,
    ) {
        val elapsed = System.currentTimeMillis() - (request.getAttribute(START_TIME_ATTR) as? Long ?: 0L)
        log.info("<<< {} {} {} ({}ms)", request.method, request.requestURI, response.status, elapsed)
    }

    companion object {
        private const val START_TIME_ATTR = "startTime"
    }
}
