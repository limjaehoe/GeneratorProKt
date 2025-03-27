package com.androidkotlin.generatorprokt.domain.model

/**
 * 422 통신 응답 결과
 */
sealed class SerialResponse {
    data class Success(
        val controlCommand: Int,
        val actionCommand: Int,
        val data: ByteArray?
    ) : SerialResponse() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Success

            if (controlCommand != other.controlCommand) return false
            if (actionCommand != other.actionCommand) return false
            if (data != null) {
                if (other.data == null) return false
                if (!data.contentEquals(other.data)) return false
            } else if (other.data != null) return false

            return true
        }

        override fun hashCode(): Int {
            var result = controlCommand
            result = 31 * result + actionCommand
            result = 31 * result + (data?.contentHashCode() ?: 0)
            return result
        }
    }

    data class Error(val exception: Exception) : SerialResponse()

    object Timeout : SerialResponse()
}