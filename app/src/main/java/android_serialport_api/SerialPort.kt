package android_serialport_api

import android.util.Log
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class SerialPort(device: File, baudrate: Int, flags: Int) {
    companion object {
        private const val TAG = "SerialPort"

        // 네이티브 라이브러리 로드
        init {
            try {
                Log.e(TAG, "시리얼 포트 라이브러리 로드 시도")
                System.loadLibrary("serial_port")
                Log.e(TAG, "시리얼 포트 라이브러리 로드 성공")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "네이티브 라이브러리 로드 실패", e)
            }
        }
    }

    private var mFd: FileDescriptor? = null
    val inputStream: InputStream
    val outputStream: OutputStream

    init {
        // 장치 파일이 존재하고 읽기/쓰기 가능한지 확인
        if (!device.canRead() || !device.canWrite()) {
            try {
                // root 권한으로 파일 권한 변경 시도
                val process = Runtime.getRuntime().exec("su")
                val outputStream = process.outputStream
                val cmd = "chmod 666 ${device.absolutePath}\nexit\n"
                outputStream.write(cmd.toByteArray())
                outputStream.flush()
                process.waitFor()
                Log.d(TAG, "권한 설정 시도: ${device.absolutePath}")
            } catch (e: Exception) {
                throw SecurityException("${device.absolutePath} 장치에 접근 권한이 없습니다.", e)
            }
        }

        // 네이티브 메서드 호출로 시리얼 포트 열기
        mFd = open(device.absolutePath, baudrate, flags)
        if (mFd == null) {
            throw IOException("시리얼 포트를 열 수 없습니다: ${device.absolutePath}")
        }

        inputStream = FileInputStream(mFd)
        outputStream = FileOutputStream(mFd)
    }

    // 시리얼 포트 닫기
    fun closePort() {
        try {
            inputStream.close()
            outputStream.close()
        } catch (e: IOException) {
            Log.e(TAG, "스트림 닫기 오류", e)
        }

        // 네이티브 close 메서드 호출
        close() // JNI 메서드 호출
    }

    // JNI 메서드
    private external fun open(path: String, baudrate: Int, flags: Int): FileDescriptor
    external fun close() // 네이티브 구현에 맞게 그대로 유지
}