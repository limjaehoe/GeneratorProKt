package com.androidkotlin.generatorprokt.domain.usecase

import com.androidkotlin.generatorprokt.domain.repository.Serial422Repository
import javax.inject.Inject

/**
 * 시리얼 포트에 연결하는 UseCase
 */
class ConnectSerialUseCase @Inject constructor(
    private val serial422Repository: Serial422Repository
) {
    suspend operator fun invoke(): Result<Unit> {
        return serial422Repository.connect()
    }
}
