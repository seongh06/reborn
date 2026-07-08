package com.reborn.server.global.handler

import com.reborn.server.global.model.CommonErrorCode
import com.reborn.server.global.slack.SlackWebhookClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class GlobalExceptionHandlerTest {

    @Mock
    private lateinit var slackWebhookClient: SlackWebhookClient

    @InjectMocks
    private lateinit var handler: GlobalExceptionHandler

    @Test
    fun `BusinessAlertException의 커스텀 메시지를 응답에 그대로 담는다`() {
        val exception = BusinessAlertException(CommonErrorCode.UNAUTHORIZED, "카카오 계정에 이메일 제공 동의가 필요합니다.")

        val response = handler.handleBusinessAlertException(exception)

        assertThat(response.statusCode.value()).isEqualTo(CommonErrorCode.UNAUTHORIZED.code)
        assertThat(response.body?.message).isEqualTo("카카오 계정에 이메일 제공 동의가 필요합니다.")
    }

    @Test
    fun `커스텀 메시지가 없으면 errorCode의 기본 메시지를 사용한다`() {
        val exception = BusinessAlertException(CommonErrorCode.UNAUTHORIZED)

        val response = handler.handleBusinessAlertException(exception)

        assertThat(response.body?.message).isEqualTo(CommonErrorCode.UNAUTHORIZED.message)
    }
}
