package com.androidkotlin.generatorprokt.domain.model

/**
 * 발전기 통신 명령어를 나타내는 sealed class
 */
sealed class SerialCommand(val value: Int) {
    // 제어 명령어
    sealed class Control(value: Int) : SerialCommand(value) {
        object CommGetInfo : Control(0x01)
        object CommSetInfo : Control(0x02)
        object CommStatusInfo : Control(0x03)
        // 필요한 제어 명령어 추가...
    }

    // 동작 명령어
    sealed class Action(value: Int) : SerialCommand(value) {
        object HeartBeat : Action(0x01)
        object InitialReq : Action(0x02)
        object FeedbackRequest : Action(0x03)
        object PowerDiagnosis : Action(0x04)
        object WarningCode : Action(0x05)
        object ErrorCode : Action(0x06)
        // 필요한 동작 명령어 추가...
    }
}
