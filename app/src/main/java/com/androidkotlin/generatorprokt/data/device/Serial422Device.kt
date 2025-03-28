package com.androidkotlin.generatorprokt.data.device

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.SystemClock
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * USB 시리얼 통신을 위한 디바이스 클래스
 */
@Singleton
class Serial422Device @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ioDispatcher: CoroutineDispatcher
) {
    companion object {
        private const val TAG = "Serial422Device"
        private const val SERIAL_BAUDRATE = 115200
        private const val ACTION_USB_PERMISSION = "com.androidkotlin.generatorprokt.USB_PERMISSION"

        // 필요에 따라 VID/PID 설정 (특정 장치를 찾기 위한 값)
        private const val USB_VENDOR_ID = 0x2542   // 예시 값, 실제 장치에 맞게 변경
        private const val USB_PRODUCT_ID = 0x1020  // 예시 값, 실제 장치에 맞게 변경
    }

    private val usbManager: UsbManager by lazy {
        context.getSystemService(Context.USB_SERVICE) as UsbManager
    }

    private var usbDevice: UsbDevice? = null
    private var usbConnection: UsbDeviceConnection? = null
    private var usbSerialPort: UsbSerialPort? = null
    private var readThread: ReadThread? = null
    private var isConnected = false

    // 데이터 수신 콜백
    var onDataReceived: ((ByteArray, Int) -> Unit)? = null

    // USB 권한 요청에 대한 브로드캐스트 리시버
    private val usbPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION == intent.action) {
                synchronized(this) {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.let {
                            // 권한이 부여되면 연결 진행
                            Timber.d("USB permission granted for device ${it.deviceName}")
                            connectToDevice(it)
                        }
                    } else {
                        Timber.e("USB permission denied for device ${device?.deviceName}")
                    }
                }
            }
        }
    }

    /**
     * 시리얼 포트 연결
     */
    suspend fun connect(): Result<Unit> = withContext(ioDispatcher) {
        try {
            if (isConnected) {
                Timber.d("이미 연결되어 있습니다.")
                return@withContext Result.success(Unit)
            }

            // USB 권한 브로드캐스트 리시버 등록
            val filter = IntentFilter(ACTION_USB_PERMISSION)
            // Android API 수준에 따라 적절한 방식으로 Receiver 등록
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    usbPermissionReceiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                context.registerReceiver(usbPermissionReceiver, filter)
            }

            // 연결할 USB 장치 찾기
            val usbDevices = usbManager.deviceList

            Timber.d("USB 장치 탐색 시작")
            Timber.d("발견된 장치 수: ${usbDevices.size}")

            // 모든 USB 장치 정보 로깅
            if (usbDevices.isEmpty()) {
                Timber.e("연결된 USB 장치가 없습니다! USB 장치가 연결되어 있는지 확인하세요.")
            } else {
                usbDevices.forEach { (name, device) ->
                    Timber.d("USB 장치: $name, VID: 0x${device.vendorId.toString(16)}, PID: 0x${device.productId.toString(16)}, 인터페이스 수: ${device.interfaceCount}")

                    // 각 인터페이스 정보 출력
                    for (i in 0 until device.interfaceCount) {
                        val intf = device.getInterface(i)
                        Timber.d("  인터페이스 #$i: 엔드포인트 수: ${intf.endpointCount}, 클래스: ${intf.interfaceClass}")
                    }
                }
            }

            if (usbDevices.isEmpty()) {
                return@withContext Result.failure(Exception("USB 장치가 감지되지 않았습니다. 장치가 연결되어 있는지 확인하세요."))
            }

            // 시리얼 장치 찾기 - 모든 가능한 드라이버 찾기
            val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
            Timber.d("시리얼 드라이버 찾기: ${availableDrivers.size}개 찾음")

            if (availableDrivers.isEmpty()) {
                return@withContext Result.failure(Exception("사용 가능한 USB 시리얼 장치를 찾을 수 없습니다."))
            }

            // 사용 가능한 모든 드라이버 정보 로깅
            availableDrivers.forEachIndexed { index, driver ->
                Timber.d("시리얼 드라이버 #$index: 장치=${driver.device.deviceName}, " +
                        "VID=0x${driver.device.vendorId.toString(16)}, " +
                        "PID=0x${driver.device.productId.toString(16)}, " +
                        "포트 수=${driver.ports.size}")
            }

            // 첫 번째 사용 가능한 드라이버 선택
            val driver = availableDrivers[0]
            usbDevice = driver.device
            Timber.d("선택된 드라이버: 장치=${driver.device.deviceName}")

            usbDevice?.let { device ->
                if (usbManager.hasPermission(device)) {
                    // 이미 권한이 있는 경우 바로 연결 시도
                    Timber.d("이미 장치에 대한 권한이 있습니다. 연결을 시도합니다.")
                    val connected = connectToDevice(device)
                    if (connected) {
                        Timber.d("장치에 성공적으로 연결되었습니다.")
                        return@withContext Result.success(Unit)
                    } else {
                        Timber.e("장치 연결에 실패했습니다.")
                        return@withContext Result.failure(Exception("장치 연결에 실패했습니다."))
                    }
                } else {
                    // 권한 요청
                    Timber.d("장치에 대한 권한이 없습니다. 권한을 요청합니다.")
                    val permissionIntent = PendingIntent.getBroadcast(
                        context, 0, Intent(ACTION_USB_PERMISSION),
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                            PendingIntent.FLAG_IMMUTABLE
                        else 0
                    )
                    usbManager.requestPermission(device, permissionIntent)

                    // 주의: 권한 요청은 비동기적으로 처리되므로 여기서는 대기 상태 반환
                    // 실제 연결은 usbPermissionReceiver에서 처리됨
                    Timber.d("권한 요청이 전송되었습니다. 결과를 기다립니다.")
                    return@withContext Result.success(Unit)
                }
            } ?: run {
                Timber.e("USB 장치를 선택할 수 없습니다.")
                return@withContext Result.failure(Exception("USB 장치를 선택할 수 없습니다."))
            }

        } catch (e: Exception) {
            Timber.e(e, "USB 시리얼 포트 연결 중 오류 발생")
            return@withContext Result.failure(e)
        }
    }

    /**
     * USB 장치에 연결
     */
    private fun connectToDevice(device: UsbDevice): Boolean {
        try {
            Timber.d("장치 ${device.deviceName}에 연결 시도 중...")

            // 드라이버 찾기
            val driver = UsbSerialProber.getDefaultProber().probeDevice(device)
            if (driver == null) {
                Timber.e("장치 ${device.deviceName}에 대한 드라이버를 찾을 수 없습니다.")
                return false
            }

            Timber.d("장치 ${device.deviceName}에 대한 드라이버를 찾았습니다. 포트 수: ${driver.ports.size}")

            // 연결 열기
            val connection = usbManager.openDevice(device)
            if (connection == null) {
                Timber.e("장치 ${device.deviceName}에 대한 연결을 열 수 없습니다.")
                return false
            }

            Timber.d("장치 ${device.deviceName}에 대한 연결을 열었습니다.")

            // 첫 번째 포트 사용
            if (driver.ports.isEmpty()) {
                Timber.e("장치 ${device.deviceName}에 사용 가능한 포트가 없습니다.")
                return false
            }

            val port = driver.ports[0]
            try {
                Timber.d("포트 열기 시도 중...")
                port.open(connection)
                Timber.d("포트를 성공적으로 열었습니다.")

                Timber.d("포트 파라미터 설정 중: $SERIAL_BAUDRATE bps, 8N1")
                port.setParameters(SERIAL_BAUDRATE, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
                Timber.d("포트 파라미터가 성공적으로 설정되었습니다.")
            } catch (e: Exception) {
                Timber.e(e, "포트 열기 또는 파라미터 설정 중 오류 발생")
                try {
                    port.close()
                } catch (closeException: Exception) {
                    Timber.e(closeException, "포트 닫기 중 오류 발생")
                }
                return false
            }

            // 필드 저장
            usbDevice = device
            usbConnection = connection
            usbSerialPort = port
            isConnected = true

            // 읽기 스레드 시작
            Timber.d("읽기 스레드 시작 중...")
            startReadThread()
            Timber.d("장치 ${device.deviceName}에 성공적으로 연결되었습니다.")

            return true
        } catch (e: Exception) {
            Timber.e(e, "장치 ${device.deviceName}에 연결 중 예외 발생")
            return false
        }
    }

    /**
     * 데이터 송신
     */
    suspend fun sendData(data: ByteArray): Result<Unit> = withContext(ioDispatcher) {
        try {
            if (!isConnected || usbSerialPort == null) {
                return@withContext Result.failure(Exception("USB serial port not connected"))
            }

            usbSerialPort?.write(data, 5000) // 5초 타임아웃
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error sending data to USB serial port")
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

            usbSerialPort?.close()
            usbSerialPort = null

            usbConnection?.close()
            usbConnection = null

            usbDevice = null
            isConnected = false

            try {
                context.unregisterReceiver(usbPermissionReceiver)
            } catch (e: IllegalArgumentException) {
                // 리시버가 등록되지 않은 경우 무시
            }

            Timber.d("USB serial port disconnected")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error disconnecting USB serial port")
            Result.failure(e)
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
                    val readBytes = usbSerialPort?.read(buffer, 100) ?: 0
                    if (readBytes > 0) {
                        onDataReceived?.invoke(buffer, readBytes)
                    }
                    SystemClock.sleep(10) // CPU 부하 감소
                } catch (e: IOException) {
                    Timber.e(e, "Error reading from USB serial port")
                    isConnected = false
                    break
                }
            }
        }
    }
}