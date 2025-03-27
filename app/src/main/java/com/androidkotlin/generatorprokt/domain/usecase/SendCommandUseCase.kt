package com.androidkotlin.generatorprokt.domain.usecase

import com.androidkotlin.generatorprokt.domain.model.SerialCommand
import com.androidkotlin.generatorprokt.domain.model.SerialPacket
import com.androidkotlin.generatorprokt.domain.model.SerialResponse
import com.androidkotlin.generatorprokt.domain.repository.Serial422Repository
import javax.inject.Inject

/**
 * 발전기에 명령을 전송하는 UseCase
 */
class SendCommandUseCase @Inject constructor(
    private val serial422Repository: Serial422Repository
) {
    suspend operator fun invoke(
        controlCommand: SerialCommand.Control,
        actionCommand: SerialCommand.Action,
        data: ByteArray? = null
    ): Result<SerialResponse> {
        val packet = SerialPacket(
            controlCommand = controlCommand,
            actionCommand = actionCommand,
            data = data
        )

        return serial422Repository.sendPacket(packet)
    }
}
