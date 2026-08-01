package com.reborn.feature.aerometer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reborn.core.common.SensorAnalyzer
import com.reborn.core.domain.usecase.GetLocalDeviceIdUseCase
import com.reborn.core.domain.usecase.SendMetricUseCase
import com.reborn.feature.aerometer.model.AerometerIntent
import com.reborn.feature.aerometer.model.AerometerUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class AerometerEvent {
    data class ShowErrorSnackbar(val throwable: Throwable) : AerometerEvent()
    data class ShowSensorResult(val personCount: Int, val lux: Int) : AerometerEvent()
    data class ShowImageSaved(val path: String) : AerometerEvent()
    data object Exit : AerometerEvent()
}

class AerometerViewModel(
    private val sensorAnalyzer: SensorAnalyzer,
    private val getLocalDeviceIdUseCase: GetLocalDeviceIdUseCase,
    private val sendMetricUseCase: SendMetricUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<AerometerUiState>(AerometerUiState.Loading)
    val uiState: StateFlow<AerometerUiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<AerometerEvent>()
    val event = _event.asSharedFlow()

    private val _isSaveImageEnabled = MutableStateFlow(false)
    val isSaveImageEnabled: StateFlow<Boolean> = _isSaveImageEnabled.asStateFlow()

    private val backStack = mutableListOf<AerometerUiState>()
    private var scanJob: Job? = null

    fun onIntent(intent: AerometerIntent) {
        when (intent) {
            is AerometerIntent.LoadInitial -> checkInitialState()
            is AerometerIntent.NavigateToSetting -> navigateTo(AerometerUiState.Setting)
            is AerometerIntent.NavigateBack -> navigateBack()
            is AerometerIntent.ToggleSaveImage -> _isSaveImageEnabled.update { !it }
        }
    }

    private fun checkInitialState() {
        scanJob?.cancel()
        backStack.clear()
        _uiState.value = AerometerUiState.Loading
        scanJob = viewModelScope.launch {
            delay(1500)
            _uiState.value = AerometerUiState.Home
            while (true) {
                delay(60_000)
                try {
                    val result = sensorAnalyzer.analyze(saveImage = _isSaveImageEnabled.value)
                    _event.emit(AerometerEvent.ShowSensorResult(result.personCount, result.lux))
                    result.savedImagePath?.let { path ->
                        _event.emit(AerometerEvent.ShowImageSaved(path))
                    }
                    // 매번 다시 읽는다(리뷰 반영, #294) - 이 화면은 페어링 완료 후에만 진입하는 게
                    // 정상 흐름이라 사실상 항상 non-null이지만, 한 번만 읽어서 캐싱하면 그 전제가
                    // 깨지는 경로가 생겨도 영원히 null로 굳어버림 - 루프 주기(60초)마다 다시 읽는
                    // 비용은 무시할 수준이라 그냥 매번 최신값을 쓰는 쪽이 안전함.
                    // 로컬 분석/표시는 전송 성공 여부와 무관하게 이미 끝났으므로, 전송 실패는 화면에
                    // 에러로 띄우지 않고 조용히 넘어간다 - 다음 60초 주기에 다시 시도됨.
                    val deviceId = getLocalDeviceIdUseCase()
                    if (deviceId != null) {
                        sendMetricUseCase(deviceId, result.lux, result.personCount)
                            .onFailure { println("AerometerViewModel: 메트릭 전송 실패 - ${it.message}") }
                    }
                } catch (e: Exception) {
                    _event.emit(AerometerEvent.ShowErrorSnackbar(e))
                }
            }
        }
    }

    private fun navigateTo(next: AerometerUiState) {
        if (_uiState.value == next) return
        backStack.add(_uiState.value)
        _uiState.value = next
    }

    private fun navigateBack() {
        val previous = backStack.removeLastOrNull()
        if (previous != null) {
            _uiState.value = previous
        } else {
            viewModelScope.launch {
                _event.emit(AerometerEvent.Exit)
            }
        }
    }
}
