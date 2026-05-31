package com.reborn.server.global.async

import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskDecorator
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor
import java.util.concurrent.ThreadPoolExecutor

@Configuration
@EnableAsync
class AsyncConfig : AsyncConfigurer {

    @Bean(ASYNC_EXECUTOR)
    override fun getAsyncExecutor(): Executor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = CORE_POOL_SIZE
            maxPoolSize = MAX_POOL_SIZE
            queueCapacity = QUEUE_CAPACITY
            setThreadNamePrefix("async-")
            setTaskDecorator(MdcTaskDecorator())
            setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())
            initialize()
        }

    @Bean(SLACK_EXECUTOR)
    fun slackExecutor(): Executor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = 1
            maxPoolSize = 2
            queueCapacity = 50
            setThreadNamePrefix("slack-")
            setTaskDecorator(MdcTaskDecorator())
            setRejectedExecutionHandler { r, executor ->
                val queueSize = (executor as? ThreadPoolExecutor)?.queue?.size ?: "unknown"
                LoggerFactory.getLogger("SlackExecutor")
                    .warn("Slack notification dropped. Task: {}, Queue Size: {}", r.javaClass.simpleName, queueSize)
            }
            initialize()
        }

    companion object {
        const val ASYNC_EXECUTOR = "asyncExecutor"
        const val SLACK_EXECUTOR = "slackExecutor"
        private const val CORE_POOL_SIZE = 4
        private const val MAX_POOL_SIZE = 8
        private const val QUEUE_CAPACITY = 100
    }
}

private class MdcTaskDecorator : TaskDecorator {

    private val log = LoggerFactory.getLogger(MdcTaskDecorator::class.java)

    override fun decorate(runnable: Runnable): Runnable {
        val contextMap = MDC.getCopyOfContextMap()
        return Runnable {
            val previousContext = MDC.getCopyOfContextMap()
            try {
                if (contextMap != null) MDC.setContextMap(contextMap) else MDC.clear()
                runnable.run()
            } catch (e: Exception) {
                log.error("Async task failed", e)
                throw e
            } finally {
                if (previousContext != null) MDC.setContextMap(previousContext) else MDC.clear()
            }
        }
    }
}