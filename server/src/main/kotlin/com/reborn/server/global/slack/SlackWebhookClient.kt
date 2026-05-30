package com.reborn.server.global.slack

import com.reborn.server.global.async.AsyncConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Component
class SlackWebhookClient(
    restTemplateBuilder: RestTemplateBuilder,
    @Value("\${slack.webhook-url:}") private val webhookUrl: String,
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val restTemplate: RestTemplate = restTemplateBuilder
        .connectTimeout(Duration.ofSeconds(3))
        .readTimeout(Duration.ofSeconds(3))
        .build()

    @Async(AsyncConfig.SLACK_EXECUTOR)
    fun send(message: String) {
        if (webhookUrl.isBlank()) return
        runCatching {
            restTemplate.postForObject(webhookUrl, SlackPayload(text = message), String::class.java)
        }.onFailure {
            log.error("Slack webhook failed: {}", it.message)
        }
    }

    private data class SlackPayload(val text: String)
}
