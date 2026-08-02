package com.reborn.feature.intro.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.designsystem.theme.RebornTheme
import com.reborn.core.ui.ext.rebornDefault

enum class TermsType(val label: String) {
    CONSUMER("소비자 약관"),
    POLICY("이용 정책"),
    PRIVACY("개인정보 처리방침");

    companion object {
        fun fromRouteType(type: String): TermsType =
            entries.find { it.name == type } ?: CONSUMER
    }
}

private data class TermsSection(val title: String?, val body: String)

@Composable
fun TermsScreen(
    initialType: TermsType,
    onBackClick: () -> Unit
) {
    var selected by remember { mutableStateOf(initialType) }

    Column(
        modifier = Modifier.rebornDefault(Color.White, topPadding = false)
    ) {
        RebornTopAppBar(title = "약관", onBackClick = onBackClick)

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            TermsType.entries.forEach { type ->
                Text(
                    text = type.label,
                    style = RebornTheme.typography.titleSmall,
                    color = if (type == selected) RebornTheme.color.grayScale900 else RebornTheme.color.grayScale500,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { selected = type }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items = termsContent(selected)) { section ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    section.title?.let { title ->
                        Text(
                            text = title,
                            style = RebornTheme.typography.titleSmall,
                            color = RebornTheme.color.grayScale900
                        )
                    }
                    Text(
                        text = section.body,
                        style = RebornTheme.typography.bodyMedium,
                        color = RebornTheme.color.grayScale700
                    )
                }
            }
        }
    }
}

// 실제 게시 전 법률 검토 필요 - 2026-07-28 기준 코드베이스를 직접 읽고 작성한 초안
// (자세한 버전은 옵시디언 project/Re;Born/약관/ 참고)
private fun termsContent(type: TermsType): List<TermsSection> = when (type) {
    TermsType.CONSUMER -> listOf(
        TermsSection(null, "이 약관은 Re;Born(이하 \"회사\")이 제공하는 실내 환경 관리 서비스(이하 " +
            "\"서비스\")를 이용함에 있어 회사와 이용자 간의 권리·의무 및 책임사항을 정합니다."),
        TermsSection("회원가입", "카카오 또는 구글 소셜 로그인으로 가입하며, 별도의 자체 회원가입 절차는 " +
            "없습니다. 최초 로그인 시 자동으로 계정이 생성되고, 이후 장소를 새로 만들거나 다른 관리자의 " +
            "초대 코드를 입력해 기존 장소에 합류할 수 있습니다."),
        TermsSection("서비스의 내용", "실내 온도·습도·조도·재실 인원 모니터링, 등록된 IoT 기기의 원격 " +
            "제어, QR 코드를 통한 방문자 피드백 수집 및 관리자 알림, AI 스피커를 통한 음성 피드백 수집, " +
            "장소·기기·관리자 권한 관리를 제공합니다."),
        TermsSection("회원 탈퇴", "이용자는 설정 화면에서 언제든지 탈퇴를 요청할 수 있습니다. 다만 탈퇴를 " +
            "요청한 이용자가 하나 이상의 장소에서 유일한 관리자인 경우, 관리자 없는 장소가 발생하는 것을 " +
            "막기 위해 탈퇴가 제한됩니다. 이 경우 다른 이용자를 해당 장소의 관리자로 먼저 초대한 뒤 " +
            "탈퇴해야 합니다."),
        TermsSection("면책조항", "회사는 천재지변, 정전, 이용자의 네트워크 환경, 하드웨어 고장 등 회사의 " +
            "귀책사유가 아닌 사유로 발생한 서비스 장애에 대해 책임을 지지 않습니다. 센서 측정값의 " +
            "정확도는 완전히 보증되지 않으며 참고용 정보로 제공됩니다.")
    )
    TermsType.POLICY -> listOf(
        TermsSection(null, "서비스를 실제로 이용할 때 지켜야 할 구체적인 규칙을 안내합니다."),
        TermsSection("장소·기기 관리", "장소를 삭제하면 그 장소에 등록된 모든 기기 정보, 센서 기록, " +
            "피드백이 함께 삭제되며 되돌릴 수 없습니다. 현재 개별 기기 하나만 선택해서 등록 해제하는 " +
            "기능은 제공하지 않습니다."),
        TermsSection("공기계(카메라 활용) 모드", "공기계로 지정된 스마트폰은 재실 인원·조도 측정을 위해 " +
            "60초 간격으로 전면 카메라를 자동으로 사용합니다. 이 촬영은 백그라운드에서 자동 반복되며, " +
            "공기계 모드로 전환하기 전 이 동작에 대해 별도로 고지받고 동의해야 합니다. 처리 방식은 " +
            "개인정보 처리방침을 참고해 주세요."),
        TermsSection("IoT 기기 원격 제어", "원격 제어는 등록된 관리자만 실행할 수 있으며, 삼성 " +
            "SmartThings 클라우드를 경유해 전달되어 실제 반영까지 수 초의 지연이 있을 수 있습니다. " +
            "자동 제어(스케줄/규칙 기반)는 현재 저장만 가능하고 실제 자동 실행 기능은 준비 중입니다."),
        TermsSection("QR 피드백", "QR 코드를 통한 피드백 제출은 로그인이 필요 없는 익명 절차이며, " +
            "방문자의 이름·연락처를 수집하지 않습니다. 동일 세션에서의 중복 제출은 1시간 동안 " +
            "제한됩니다."),
        TermsSection("금지 행위", "타인의 계정·초대 코드·페어링 코드·기기 시리얼을 무단으로 사용하는 " +
            "행위, 서비스의 정상적인 운영을 방해하는 행위, 소스코드나 API를 무단으로 분석·이용하는 " +
            "행위는 금지됩니다.")
    )
    TermsType.PRIVACY -> listOf(
        TermsSection(null, "회사가 수집하는 개인정보와 처리 방식을 안내합니다."),
        TermsSection("수집 항목", "회원가입 시 이름, 이메일(선택), 프로필 이미지 URL, 알림 발송을 위한 " +
            "푸시 토큰을 수집합니다. 서비스 이용 중에는 실내 온도·습도·조도·재실 인원, 방문자 피드백 " +
            "내용이 수집됩니다. QR 피드백은 방문자의 이름·연락처·계정 정보를 요구하지 않는 익명 " +
            "제출입니다."),
        TermsSection("공기계 카메라 특별 고지", "공기계 모드로 전환한 기기는 실내 재실 인원과 조도를 " +
            "측정하기 위해 전면 카메라를 자동으로 사용합니다. 촬영된 이미지는 해당 기기 안에서만 " +
            "분석되며, 분석 결과(인원수·조도 수치)만 서버로 전송됩니다. 촬영 원본은 서버로 전송되거나 " +
            "저장되지 않으며, 이용자가 설정에서 허용한 경우에만 해당 기기 로컬 저장소에 보관됩니다."),
        TermsSection("AI 스피커 음성 피드백", "공기계 카메라와 달리, AI 스피커의 음성 피드백은 녹음된 " +
            "음성이 외부 AI 서비스로 전송되어 텍스트 요약으로 변환됩니다. 변환된 텍스트만 피드백으로 " +
            "저장되며, 원본 음성 파일을 서버에 영구 저장하지 않습니다."),
        TermsSection("제3자 제공", "로그인 시 카카오·구글에 인증을 요청하고, 알림 발송을 위해 Firebase " +
            "Cloud Messaging을 이용하며, IoT 기기 원격 제어를 위해 삼성 SmartThings와 연동합니다. " +
            "이 경우를 제외하고 개인정보를 제3자에게 제공하지 않습니다."),
        TermsSection("보관 기간 및 이용자 권리", "로그인 갱신 토큰은 14일간 보관됩니다. 이용자는 언제든 " +
            "앱 설정에서 본인의 프로필 정보를 열람·수정할 수 있고, 회원 탈퇴를 통해 개인정보 삭제를 " +
            "요청할 수 있습니다.")
    )
}
