package com.androidkotlin.generatorprokt.data.device

import android.os.SystemClock
import android.util.Log
import android_serialport_api.SerialPort
import android_serialport_api.SerialPortFinder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Serial422Device @Inject constructor(
    private val ioDispatcher: CoroutineDispatcher
) {
    companion object {
        private const val TAG = "Serial422Device"
        private const val SERIAL_BAUDRATE = 115200
        private const val SERIAL_PORT_NAME = "ttyS3" // RS-485 포트
    }

    private var mSerialPort: SerialPort? = null
    private var mInputStream: InputStream? = null
    private var mOutputStream: OutputStream? = null
    private var readThread: ReadThread? = null
    private var isConnected = false

    // 데이터 수신 콜백
    var onDataReceived: ((ByteArray, Int) -> Unit)? = null

    /**
     * 시리얼 포트 연결
     */
    suspend fun connect(): Result<Unit> = withContext(ioDispatcher) {
        try {
            if (isConnected) {
                return@withContext Result.success(Unit)
            }

            Timber.d("시리얼 포트 연결 시도 중...")
            openSerialPort(SERIAL_PORT_NAME)

            if (mSerialPort != null) {
                isConnected = true
                startReadThread()
                Timber.d("시리얼 포트 연결 성공")
                return@withContext Result.success(Unit)
            } else {
                Timber.e("시리얼 포트 연결 실패: 포트를 열 수 없음")
                return@withContext Result.failure(Exception("Failed to open serial port"))
            }
        } catch (e: Exception) {
            Timber.e(e, "시리얼 포트 연결 중 오류 발생")
            return@withContext Result.failure(e)
        }
    }

    /**
     * 시리얼 포트 열기
     */
    private fun openSerialPort(name: String) {
        val serialPortFinder = SerialPortFinder()
        val devices = serialPortFinder.allDevices
        val devicesPath = serialPortFinder.allDevicesPath

        Timber.d("사용 가능한 시리얼 포트 검색 중...")
        Timber.d("발견된 장치 수: ${devices.size}")

        for (i in devices.indices) {
            val device = devices[i]
            val path = devicesPath[i]
            Timber.d("발견된 장치[$i]: $device, 경로: $path")

            if (device.contains(name)) {
                try {
                    // 시리얼 포트 파일에 권한 설정
                    setPermissions(path)

                    // 권한 설정 후 시리얼 포트 열기 시도
                    mSerialPort = SerialPort(File(path), SERIAL_BAUDRATE, 0)
                    Timber.d("시리얼 포트 열림: $path")
                    break
                } catch (e: IOException) {
                    Timber.e(e, "시리얼 포트 열기 실패: $path")
                }
            }
        }

        // 적합한 포트를 찾지 못한 경우 다른 일반적인 경로 시도
        if (mSerialPort == null) {
            val commonPaths = arrayOf(
                "/dev/ttyS3", "/dev/ttyS2", "/dev/ttyS1", "/dev/ttyS0",
                "/dev/ttyS4", "/dev/ttyUSB0", "/dev/ttyACM0"
            )

            for (path in commonPaths) {
                try {
                    Timber.d("일반 경로 시도: $path")
                    setPermissions(path)
                    mSerialPort = SerialPort(File(path), SERIAL_BAUDRATE, 0)
                    if (mSerialPort != null) {
                        Timber.d("시리얼 포트 열림(공통 경로): $path")
                        break
                    }
                } catch (e: Exception) {
                    Timber.e(e, "시리얼 포트 열기 실패(공통 경로): $path")
                }
            }
        }

        if (mSerialPort != null) {
            mInputStream = mSerialPort?.inputStream
            mOutputStream = mSerialPort?.outputStream
            Timber.d("입출력 스트림 생성 완료")
        } else {
            Timber.e("적합한 시리얼 포트를 찾을 수 없음")
        }
    }

    /**
     * 데이터 송신
     */
    suspend fun sendData(data: ByteArray): Result<Unit> = withContext(ioDispatcher) {
        try {
            if (!isConnected || mOutputStream == null) {
                return@withContext Result.failure(Exception("Serial port not connected"))
            }

            mOutputStream?.write(data)
            mOutputStream?.flush()
            Timber.d("데이터 전송 완료: ${bytesToHexString(data)}")

            return@withContext Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "데이터 전송 중 오류 발생")
            return@withContext Result.failure(e)
        }
    }

    /**
     * 연결 종료
     */
    suspend fun disconnect(): Result<Unit> = withContext(ioDispatcher) {
        try {
            readThread?.interrupt()
            readThread = null

            mInputStream?.close()
            mOutputStream?.close()
            mSerialPort?.closePort() // closePort() 메서드 호출

            mInputStream = null
            mOutputStream = null
            mSerialPort = null
            isConnected = false

            Timber.d("시리얼 포트 연결 종료")
            return@withContext Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "시리얼 포트 연결 종료 중 오류 발생")
            return@withContext Result.failure(e)
        }
    }

    /**
     * 연결 상태 확인
     */
    fun isConnected(): Boolean = isConnected

    /**
     * 읽기 스레드 시작
     */
    private fun startReadThread() {
        readThread?.interrupt()
        readThread = ReadThread().apply { start() }
    }

    /**
     * 데이터 수신을 위한 스레드
     */
    private inner class ReadThread : Thread() {
        override fun run() {
            val buffer = ByteArray(1024)

            while (!isInterrupted && isConnected) {
                try {
                    val readBytes = mInputStream?.read(buffer) ?: 0
                    if (readBytes > 0) {
                        Timber.d("데이터 수신: ${readBytes}바이트")
                        onDataReceived?.invoke(buffer, readBytes)
                    }
                    SystemClock.sleep(10) // CPU 부하 감소
                } catch (e: IOException) {
                    Timber.e(e, "데이터 읽기 중 오류 발생")
                    isConnected = false
                    break
                }
            }
        }
    }

    /**
     * 바이트 배열을 16진수 문자열로 변환
     */
    private fun bytesToHexString(bytes: ByteArray): String {
        return bytes.joinToString(" ") { byte ->
            String.format("%02X", byte)
        }
    }

    private fun setPermissions(path: String) {
        try {
            val process = Runtime.getRuntime().exec("su")
            val outputStream = process.outputStream
            val cmd = "chmod 666 $path\nexit\n"
            outputStream.write(cmd.toByteArray())
            outputStream.flush()
            process.waitFor()
            Timber.d("권한 설정 시도: $path")
        } catch (e: Exception) {
            Timber.e(e, "권한 설정 실패: $path")
        }
    }



}