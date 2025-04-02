package com.androidkotlin.generatorprokt.presentation.main.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android_serialport_api.SerialPortFinder
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.androidkotlin.generatorprokt.R
import com.androidkotlin.generatorprokt.databinding.ActivityHomeBinding
import com.androidkotlin.generatorprokt.domain.model.MainMode
import com.androidkotlin.generatorprokt.presentation.main.state.DeviceStatus
import com.androidkotlin.generatorprokt.presentation.main.viewmodel.GeneratorStateViewModel
import com.androidkotlin.generatorprokt.presentation.main.state.GeneratorUiState
import com.androidkotlin.generatorprokt.presentation.main.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val viewModel: HomeViewModel by viewModels()
    private val generatorStateViewModel: GeneratorStateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.getRoot())

        // USB 권한 요청 인텐트 필터 및 레시버 등록
        setupUsbPermissionIntentFilter()

        setupUI()
        observeViewModel()

        // 시리얼 포트 확인 (디버깅용)
        checkSerialPorts()
    }

    private fun checkSerialPorts() {
        val portFinder = SerialPortFinder()
        val devices = portFinder.allDevices
        val paths = portFinder.allDevicesPath

        Log.e("SERIAL_TEST", "발견된 시리얼 장치 수: ${devices.size}")
        for (i in devices.indices) {
            Log.e("SERIAL_TEST", "장치[$i]: ${devices[i]}, 경로: ${paths[i]}")
            // 또는 Toast로도 확인
            Toast.makeText(this, "장치[$i]: ${devices[i]}, 경로: ${paths[i]}", Toast.LENGTH_SHORT).show()
        }

        // ttyS3 포트가 있는지 특별히 확인
        val hasTtyS3 = paths.any { it.contains("ttyS3") }
        Log.e("SERIAL_TEST", "ttyS3 포트 발견됨: $hasTtyS3")
        val hasTtyS4 = paths.any { it.contains("ttyS4") }
        Log.e("SERIAL_TEST", "ttyS4 포트 발견됨: $hasTtyS4")

        //Toast.makeText(this, "ttyS3 포트 발견됨: $hasTtyS3", Toast.LENGTH_LONG).show()
    }

    // USB 권한 요청에 필요한 인텐트 필터 설정
    private fun setupUsbPermissionIntentFilter() {
        // USB 장치 연결/분리 이벤트를 캐치하기 위한 인텐트 필터
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }

        // 브로드캐스트 리시버 등록
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbDeviceReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(usbDeviceReceiver, filter)
        }

        Timber.d("USB 장치 이벤트 리시버가 등록되었습니다")
    }

    // USB 장치 연결/분리 이벤트를 처리하는 브로드캐스트 리시버
    private val usbDeviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }

                    device?.let {
                        Timber.d("USB 장치가 연결되었습니다: ${it.deviceName}")
                        binding.tvStatus.text = "USB 장치가 연결되었습니다: ${it.deviceName}"
                        // 연결 시 자동으로 연결 시도
                        viewModel.connectDevice()
                    }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }

                    device?.let {
                        Timber.d("USB 장치가 분리되었습니다: ${it.deviceName}")
                        binding.tvStatus.text = "USB 장치가 분리되었습니다: ${it.deviceName}"
                    }
                }
            }
        }
    }

    private fun setupUI() {
        // 기본 버튼 설정
        binding.btnConnect.setOnClickListener {
            Timber.d("연결 버튼이 클릭되었습니다")
            binding.tvStatus.text = "연결 시도 중..."
            viewModel.connectDevice()
        }

        binding.btnHeartbeat.setOnClickListener {
            Timber.d("하트비트 버튼이 클릭되었습니다")
            binding.tvStatus.text = "하트비트 전송 중..."
            viewModel.sendHeartbeat()
        }

        binding.btnPowerDiagnosis.setOnClickListener {
            Timber.d("전원 진단 버튼이 클릭되었습니다")
            binding.tvStatus.text = "전원 진단 요청 중..."
            viewModel.requestPowerDiagnosis(0) // 3.3V 전원 진단 요청
        }

        binding.btnVersionInfo.setOnClickListener {
            Timber.d("버전 정보 버튼이 클릭되었습니다")
            binding.tvStatus.text = "버전 정보 요청 중..."
            viewModel.requestBoardVersion(0) // Main Board App 버전 요청
        }

        // kV 설정
        binding.btnSetKv.setOnClickListener {
            val kvValueText = binding.etKvValue.text.toString()
            if (kvValueText.isNotEmpty()) {
                try {
                    val kvValue = kvValueText.toInt()
                    if (kvValue in 40..150) {
                        viewModel.setKvValue(kvValue)
                    } else {
                        showToast("kV 값은 40에서 150 사이여야 합니다")
                    }
                } catch (e: NumberFormatException) {
                    showToast("유효한 숫자를 입력하세요")
                }
            } else {
                showToast("kV 값을 입력하세요")
            }
        }

        // mA 설정
        binding.btnSetMa.setOnClickListener {
            val maValueText = binding.etMaValue.text.toString()
            if (maValueText.isNotEmpty()) {
                try {
                    val maValue = maValueText.toInt()
                    if (maValue in 10..500) {
                        viewModel.setMaValue(maValue)
                    } else {
                        showToast("mA 값은 10에서 500 사이여야 합니다")
                    }
                } catch (e: NumberFormatException) {
                    showToast("유효한 숫자를 입력하세요")
                }
            } else {
                showToast("mA 값을 입력하세요")
            }
        }

        // 노출 시간 설정
        binding.btnSetTime.setOnClickListener {
            val timeValueText = binding.etTimeValue.text.toString()
            if (timeValueText.isNotEmpty()) {
                try {
                    val timeValue = timeValueText.toInt()
                    if (timeValue in 1..10000) {
                        viewModel.setTimeValue(timeValue)
                    } else {
                        showToast("노출 시간은 1에서 10000 밀리초 사이여야 합니다")
                    }
                } catch (e: NumberFormatException) {
                    showToast("유효한 숫자를 입력하세요")
                }
            } else {
                showToast("노출 시간을 입력하세요")
            }
        }

        // 포커스 설정
        binding.btnSetFocus.setOnClickListener {
            val smallFocus = binding.rbFocusSmall.isChecked
            Timber.d("포커스 설정: ${if (smallFocus) "Small" else "Large"}")
            generatorStateViewModel.setFocus(smallFocus)
        }

        // Ready 버튼
        binding.btnReady.setOnClickListener {
            try {
                // 필수 값 확인
                if (binding.etKvValue.text.isEmpty() || binding.etMaValue.text.isEmpty() || binding.etTimeValue.text.isEmpty()) {
                    binding.tvStatus.text = "조사 조건을 모두 입력해 주세요"
                    return@setOnClickListener
                }

                // 값 추출
                val kv = binding.etKvValue.text.toString().toDouble() / 10 // 예: 40 -> 4.0kV
                val mA = binding.etMaValue.text.toString().toDouble() / 10 // 예: 10 -> 1.0mA
                val ms = binding.etTimeValue.text.toString().toDouble()
                val focus = if (binding.rbFocusSmall.isChecked) 1 else 0

                // Ready 명령 전송
                generatorStateViewModel.setReadySwitch(kv, mA, ms, focus)
                binding.tvStatus.text = "Ready 명령 전송됨"
            } catch (e: Exception) {
                binding.tvStatus.text = "오류: ${e.message}"
                Timber.e(e, "Ready 설정 중 오류 발생")
            }
        }

        // Expose 버튼
        binding.btnExpose.setOnClickListener {
            generatorStateViewModel.setExpose(true)
            binding.tvStatus.text = "Expose 명령 전송됨"
        }

        // 로그 지우기 버튼
        binding.btnClearLog.setOnClickListener {
            binding.tvResponseData.text = ""
        }
    }

    private fun observeViewModel() {
        // 발전기 상태 관찰
        lifecycleScope.launch {
            generatorStateViewModel.currentMode.collectLatest { mode ->
                // GeneratorStateView 업데이트
                binding.generatorStateView.currentMode = mode
                binding.generatorStateView.invalidate() // 명시적 갱신 추가

                // 로그 출력
                Timber.d("발전기 상태 변경: ${mode.name} (0x${mode.hexValue.toString(16)})")

                // 상태에 따른 버튼 활성화/비활성화
                updateUIBasedOnMode(mode)
            }
        }

        // 피드백 값 관찰
        lifecycleScope.launch {
            generatorStateViewModel.kvFeedback.collectLatest { value ->
                value?.let {
                    binding.tvStatus.text = "kV 피드백: $it kV"
                }
            }
        }

        lifecycleScope.launch {
            generatorStateViewModel.maFeedback.collectLatest { value ->
                value?.let {
                    // 이미 다른 상태 메시지가 있을 수 있으므로 추가만 함
                    val currentText = binding.tvStatus.text.toString()
                    if (!currentText.contains("mA 피드백")) {
                        binding.tvStatus.text = "$currentText | mA 피드백: $it mA"
                    }
                }
            }
        }

        lifecycleScope.launch {
            generatorStateViewModel.timeFeedback.collectLatest { value ->
                value?.let {
                    // 로그에만 출력
                    Timber.d("시간 피드백: $it ms")
                }
            }
        }

        // 오류 코드 관찰
        lifecycleScope.launch {
            generatorStateViewModel.errorCode.collectLatest { errorCode ->
                errorCode?.let {
                    showErrorDialog("오류 발생: 코드 $it")
                }
            }
        }

        // 경고 코드 관찰
        lifecycleScope.launch {
            generatorStateViewModel.warningCode.collectLatest { warningCode ->
                warningCode?.let {
                    binding.tvStatus.text = "경고 발생: 코드 $it"
                }
            }
        }

        // Ready 스위치 상태 관찰
        lifecycleScope.launch {
            generatorStateViewModel.readySwitch.collectLatest { readyStatus ->
                readyStatus?.let {
                    if (it == 1) {
                        binding.tvStatus.text = "Ready 상태: ON"
                        binding.btnExpose.isEnabled = true
                    } else {
                        binding.tvStatus.text = "Ready 상태: OFF"
                        binding.btnExpose.isEnabled = false
                    }
                }
            }
        }

        // UI 상태 관찰
        lifecycleScope.launch {
            generatorStateViewModel.uiState.collectLatest { state ->
                when (state) {
                    is GeneratorUiState.Loading -> {
                        binding.tvStatus.text = "발전기 상태 로딩 중..."
                    }
                    is GeneratorUiState.Error -> {
                        binding.tvStatus.text = "발전기 오류: ${state.message}"
                        // 오류 시 경고 다이얼로그 표시 (중요 오류일 경우)
                        if (state.message.contains("긴급") || state.message.contains("오류")) {
                            showErrorDialog(state.message)
                        }
                    }
                    is GeneratorUiState.Exposing -> {
                        binding.tvStatus.text = "노출 중: ${state.message}"
                        // 노출 중일 때는 특별한 UI 표시 (예: 경고 표시)
                        binding.tvStatus.setTextColor(getColor(android.R.color.holo_red_dark))
                    }
                    is GeneratorUiState.Ready -> {
                        binding.tvStatus.text = "준비 완료: ${state.message}"
                        binding.tvStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                    }
                    is GeneratorUiState.Idle -> {
                        binding.tvStatus.text = "대기 중: ${state.message}"
                        binding.tvStatus.setTextColor(getColor(android.R.color.black))
                    }
                    is GeneratorUiState.Success -> {
                        binding.tvStatus.text = state.message
                        binding.tvStatus.setTextColor(getColor(android.R.color.holo_blue_dark))
                    }
                }
            }
        }

        // 장치 상태 관찰
        lifecycleScope.launch {
            viewModel.deviceStatus.collectLatest { status ->
                when (status) {
                    DeviceStatus.Disconnected -> {
                        binding.tvConnectionStatus.text = "연결 끊김"
                        binding.tvConnectionStatus.setTextColor(getColor(android.R.color.holo_red_dark))
                        Timber.d("장치 상태: 연결 끊김")

                        // 연결 끊김 상태에서는 연결 버튼 활성화, 다른 버튼 비활성화
                        binding.btnConnect.isEnabled = true
                        binding.btnHeartbeat.isEnabled = false
                        binding.btnPowerDiagnosis.isEnabled = false
                        binding.btnVersionInfo.isEnabled = false
                        binding.btnSetKv.isEnabled = false
                        binding.btnSetMa.isEnabled = false
                        binding.btnSetTime.isEnabled = false
                        binding.btnSetFocus.isEnabled = false
                        binding.btnReady.isEnabled = false
                        binding.btnExpose.isEnabled = false
                    }
                    DeviceStatus.Connected -> {
                        binding.tvConnectionStatus.text = "연결됨"
                        binding.tvConnectionStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                        Timber.d("장치 상태: 연결됨")

                        // 연결 상태에서는 연결 버튼 비활성화, 다른 버튼 활성화
                        binding.btnConnect.isEnabled = false
                        binding.btnHeartbeat.isEnabled = true
                        binding.btnPowerDiagnosis.isEnabled = true
                        binding.btnVersionInfo.isEnabled = true
                        binding.btnSetKv.isEnabled = true
                        binding.btnSetMa.isEnabled = true
                        binding.btnSetTime.isEnabled = true
                        binding.btnSetFocus.isEnabled = true
                        binding.btnReady.isEnabled = true
                        binding.btnExpose.isEnabled = false // 초기에는 비활성화

                        // 연결 후 시스템 상태 요청
                        generatorStateViewModel.requestSystemStatus()
                    }
                    DeviceStatus.Error -> {
                        binding.tvConnectionStatus.text = "오류"
                        binding.tvConnectionStatus.setTextColor(getColor(android.R.color.holo_orange_dark))
                        Timber.e("장치 상태: 오류")

                        // 오류 상태에서는 모든 버튼 활성화하여 재시도 허용
                        binding.btnConnect.isEnabled = true
                        binding.btnHeartbeat.isEnabled = false
                        binding.btnPowerDiagnosis.isEnabled = false
                        binding.btnVersionInfo.isEnabled = false
                        binding.btnSetKv.isEnabled = false
                        binding.btnSetMa.isEnabled = false
                        binding.btnSetTime.isEnabled = false
                        binding.btnSetFocus.isEnabled = false
                        binding.btnReady.isEnabled = false
                        binding.btnExpose.isEnabled = false
                    }
                }
            }
        }

        // 로그 메시지 관찰
        lifecycleScope.launch {
            viewModel.receivedData.collectLatest { logMessage ->
                addLogMessage(logMessage)
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun addLogMessage(logMessage: String) {
        val currentLog = binding.tvResponseData.text.toString()
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
        val newLog = if (currentLog.isEmpty()) {
            "[$timestamp] $logMessage"
        } else {
            "$currentLog\n[$timestamp] $logMessage"
        }
        binding.tvResponseData.text = newLog

        // 로그가 너무 길어지면 오래된 내용 삭제
        if (binding.tvResponseData.lineCount > 100) {
            val lines = newLog.split('\n')
            binding.tvResponseData.text = lines.takeLast(50).joinToString("\n")
        }

        // 자동 스크롤
        binding.tvResponseData.post {
            (binding.tvResponseData.parent as androidx.core.widget.NestedScrollView).fullScroll(androidx.core.widget.NestedScrollView.FOCUS_DOWN)
        }
    }

    /**
     * 모드에 따른 UI 업데이트
     */
    private fun updateUIBasedOnMode(mode: MainMode) {
        when (mode) {
            MainMode.EXPOSURE, MainMode.EXPOSURE_READY, MainMode.EXPOSURE_READY_DONE -> {
                // 노출 관련 모드일 때는 다른 설정 버튼을 비활성화
                binding.btnSetKv.isEnabled = false
                binding.btnSetMa.isEnabled = false
                binding.btnSetTime.isEnabled = false
                binding.btnSetFocus.isEnabled = false

                // Exposure 버튼 활성화 (READY_DONE 상태일 때만)
                binding.btnExpose.isEnabled = mode == MainMode.EXPOSURE_READY_DONE

                // 로그 추가
                if (mode == MainMode.EXPOSURE_READY_DONE) {
                    addLogMessage("Ready 완료: 조사 가능 상태")
                } else if (mode == MainMode.EXPOSURE) {
                    addLogMessage("조사 중...")
                }
            }
            MainMode.STANDBY -> {
                // 대기 모드일 때는 모든 설정 버튼 활성화
                binding.btnSetKv.isEnabled = true
                binding.btnSetMa.isEnabled = true
                binding.btnSetTime.isEnabled = true
                binding.btnSetFocus.isEnabled = true

                // Exposure 버튼 비활성화
                binding.btnExpose.isEnabled = false

                // 로그 추가
                addLogMessage("대기 모드 진입")
            }
            MainMode.ERROR, MainMode.EMERGENCY -> {
                // 오류 상태일 때는 모든 버튼 비활성화
                binding.btnSetKv.isEnabled = false
                binding.btnSetMa.isEnabled = false
                binding.btnSetTime.isEnabled = false
                binding.btnSetFocus.isEnabled = false
                binding.btnExpose.isEnabled = false

                // 오류 메시지 표시
                val errorMessage = if (mode == MainMode.ERROR) "시스템 오류 발생" else "긴급 정지 상태"
                showErrorDialog(errorMessage)

                // 로그 추가
                addLogMessage("오류 발생: $errorMessage")
            }
            else -> {
                // 기타 모드에서는 기본 상태로 설정
                binding.btnSetKv.isEnabled = true
                binding.btnSetMa.isEnabled = true
                binding.btnSetTime.isEnabled = true
                binding.btnSetFocus.isEnabled = true
                binding.btnExpose.isEnabled = false
            }
        }
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("발전기 알림")
            .setMessage(message)
            .setPositiveButton("확인", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // USB 장치 리시버 등록 해제
        try {
            unregisterReceiver(usbDeviceReceiver)
            Timber.d("USB 장치 이벤트 리시버가 등록 해제되었습니다")
        } catch (e: Exception) {
            Timber.e(e, "USB 장치 이벤트 리시버 등록 해제 중 오류 발생")
        }
    }
}