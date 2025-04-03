package com.androidkotlin.generatorprokt.data.device

import android.os.SystemClock
import android.util.Log
import android_serialport_api.SerialPort
import android_serialport_api.SerialPortFinder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
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
        private const val SERIAL_PORT_NAME = "ttyS4" // RS-485 포트
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

                // 여기에 로그 추가
                Timber.d("woghl1129: ReadThread 시작 전 상태 확인 - 연결됨=${isConnected}")

                startReadThread()

                // 여기에 로그 추가
                Timber.d("woghl1129: ReadThread 시작 후 상태 확인 - thread=${readThread?.state}, 살아있음=${readThread?.isAlive}")

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
                "/dev/ttyS4", "/dev/ttyS2", "/dev/ttyS1", "/dev/ttyS0",
                "/dev/ttyS3", "/dev/ttyUSB0", "/dev/ttyACM0"
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
//    suspend fun sendData(data: ByteArray): Result<Unit> = withContext(ioDispatcher) {
//        try {
//            if (!isConnected || mOutputStream == null) {
//                return@withContext Result.failure(Exception("Serial port not connected"))
//            }
//
//            mOutputStream?.write(data)
//            mOutputStream?.flush()
//            Timber.d("데이터 전송 완료: ${bytesToHexString(data)}")
//
//            return@withContext Result.success(Unit)
//        } catch (e: Exception) {
//            Timber.e(e, "데이터 전송 중 오류 발생")
//            return@withContext Result.failure(e)
//        }
//    }

    suspend fun sendData(data: ByteArray): Result<Unit> = withContext(ioDispatcher) {
        try {
            // 데이터 유효성 검사
            if (data.isEmpty()) {
                Timber.e("전송할 데이터가 없습니다")
                return@withContext Result.failure(IllegalArgumentException("데이터가 비어있습니다"))
            }

            // 연결 상태 확인
            if (!isConnected) {
                Timber.e("시리얼 포트가 연결되어 있지 않습니다")
                return@withContext Result.failure(IOException("시리얼 포트 연결 안됨"))
            }

            // 로깅: 전송 데이터 16진수 표현
            val hexData = data.joinToString(" ") { String.format("%02X", it) }
            Timber.d("데이터 전송 준비: $hexData")

            // 데이터 쓰기 전 버퍼 비우기
            mOutputStream?.let { outputStream ->
                try {
                    // available() 메서드 대신 다른 방식으로 버퍼 클리어
                    outputStream.flush()
                } catch (e: Exception) {
                    Timber.w("버퍼 클리어 중 예외 발생: ${e.message}")
                }

                // 데이터 쓰기
                try {
                    outputStream.write(data)
                    outputStream.flush()
                    Timber.d("데이터 전송 완료: ${data.size}바이트")
                } catch (e: IOException) {
                    Timber.e(e, "데이터 쓰기 중 오류 발생")
                    return@withContext Result.failure(e)
                }
            } ?: run {
                Timber.e("출력 스트림이 초기화되지 않았습니다")
                return@withContext Result.failure(IllegalStateException("출력 스트림 없음"))
            }

            // 전송 후 짧은 대기 (선택적)
            delay(10)

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "데이터 전송 중 예외 발생")
            Result.failure(e)
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
        Timber.d("woghl1129: startReadThread() 호출됨 - 이전 스레드 상태=${readThread?.state}")
        readThread?.interrupt()
        readThread = ReadThread().apply {
            Timber.d("woghl1129: 새 ReadThread 생성")
            start()
            Timber.d("woghl1129: ReadThread.start() 호출 완료")
        }
    }

    /**
     * 데이터 수신을 위한 스레드
     */
    private inner class ReadThread : Thread() {
        override fun run() {
            val buffer = ByteArray(1024)
            Timber.d("ReadThread 시작됨")

            while (!isInterrupted && isConnected) {
                try {
                    // 데이터 가용성 확인
                    if (mInputStream?.available() ?: 0 > 0) {
                        val readBytes = mInputStream?.read(buffer) ?: 0
                        if (readBytes > 0) {
                            Timber.d("데이터 수신: ${readBytes}바이트")
                            onDataReceived?.invoke(buffer, readBytes)
                        }
                    } else {
                        // 데이터가 없으면 짧게 대기
                        SystemClock.sleep(1)
                    }
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