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

    companion object {
        const val ASYNC_EXECUTOR = "asyncExecutor"
        private const val CORE_POOL_SIZE = 4
        private const val MAX_POOL_SIZE = 8
        private const val QUEUE_CAPACITY = 100
    }
}

private class MdcTaskDecorator : TaskDecorator {

    private val log = LoggerFactory.getLogger(MdcTaskDecorator::class.java)

    override fun decorate(runnable: Runnable): Runnable {
        val contextMap = MDC.getCopyOfContextMap().orEmpty()
        return Runnable {
            try {
                MDC.clear()
                if (contextMap.isNotEmpty()) MDC.setContextMap(contextMap)
                runnable.run()
            } catch (e: Exception) {
                log.error("Async task failed", e)
                throw e
            } finally {
                MDC.clear()
            }
        }
    }
}
