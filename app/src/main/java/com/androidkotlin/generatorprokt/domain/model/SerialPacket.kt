package com.androidkotlin.generatorprokt.domain.model

/**
 * 422 통신 패킷 구조
 */
data class SerialPacket(
    val controlCommand: SerialCommand.Control,
    val actionCommand: SerialCommand.Action,
    val data: ByteArray? = null,
    val targetId: Byte = TARGET_MAIN,
    val sourceId: Byte = SOURCE_CONSOLE
) {
    companion object {
        const val PROTOCOL_FIRST: Byte = 0x23
        const val PROTOCOL_SECOND: Byte = 0x50
        const val PROTOCOL_END: Byte = 0x21

        const val TARGET_MAIN: Byte = 0x24    // Main
        const val TARGET_CONSOLE: Byte = 0x25 // Console
        const val TARGET_PC: Byte = 0x26      // PC

        const val SOURCE_MAIN: Byte = 0x24    // Main
        const val SOURCE_CONSOLE: Byte = 0x25 // Console
        const val SOURCE_PC: Byte = 0x26      // PC
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SerialPacket

        if (controlCommand != other.controlCommand) return false
        if (actionCommand != other.actionCommand) return false
        if (data != null) {
            if (other.data == null) return false
            if (!data.contentEquals(other.data)) return false
        } else if (other.data != null) return false
        if (targetId != other.targetId) return false
        if (sourceId != other.sourceId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = controlCommand.hashCode()
        result = 31 * result + actionCommand.hashCode()
        result = 31 * result + (data?.contentHashCode() ?: 0)
        result = 31 * result + targetId
        result = 31 * result + sourceId
        return result
    }
}