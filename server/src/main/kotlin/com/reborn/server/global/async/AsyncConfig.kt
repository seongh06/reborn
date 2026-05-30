package com.reborn.server.global.async

import org.slf4j.MDC
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskDecorator
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

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
    override fun decorate(runnable: Runnable): Runnable {
        val contextMap = MDC.getCopyOfContextMap().orEmpty()
        return Runnable {
            try {
                if (contextMap.isNotEmpty()) MDC.setContextMap(contextMap)
                runnable.run()
            } finally {
                MDC.clear()
            }
        }
    }
}
