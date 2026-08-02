package com.reborn.core.domain.usecase

import com.reborn.core.domain.repository.PlaceRepository
import com.reborn.core.model.Place

data class PlaceResolution(val places: List<Place>, val selected: Place?)

// Home/Data/기기 등록 화면이 공유하는 "지금 선택된 장소"를 해석한다(#166) - 선택한 적 없거나
// 선택했던 장소가 그 사이 삭제됐으면 첫 번째 장소로 자동 폴백하고, 그 결과를 곧바로 다시 저장해서
// 다른 화면들도 같은 장소를 보도록 동기화한다(선택된 장소를 읽는 모든 곳이 이 UseCase를 거치면
// 삭제된 장소를 계속 가리키는 상태가 스스로 치유된다).
class ResolveSelectedPlaceUseCase(
    private val placeRepository: PlaceRepository,
) {
    suspend operator fun invoke(): Result<PlaceResolution> =
        placeRepository.getList().map { places ->
            val selectedId = placeRepository.getSelectedPlaceId()
            val resolved = places.find { it.placeId == selectedId } ?: places.firstOrNull()
            if (resolved != null && resolved.placeId != selectedId) {
                placeRepository.selectPlace(resolved.placeId)
            }
            PlaceResolution(places = places, selected = resolved)
        }
}
