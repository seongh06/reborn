package com.reborn.core.domain

import kotlinx.coroutines.flow.Flow

interface UseCase<in Params, out Result> {
    operator fun invoke(params: Params): Flow<Result>
}

interface NoParamsUseCase<out Result> {
    operator fun invoke(): Flow<Result>
}
