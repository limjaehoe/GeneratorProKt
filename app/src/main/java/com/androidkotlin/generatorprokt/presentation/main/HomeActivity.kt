package com.androidkotlin.generatorprokt.presentation.main

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.androidkotlin.generatorprokt.databinding.ActivityHomeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)


        setupUI()
        observeViewModel()


    }

    private fun setupUI() {
        binding.btnConnect.setOnClickListener {
            viewModel.connectDevice()
        }

        binding.btnHeartbeat.setOnClickListener {
            viewModel.sendHeartbeat()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is GeneratorUiState.Idle -> {
                        binding.tvStatus.text = "대기 중"
                    }
                    is GeneratorUiState.Loading -> {
                        binding.tvStatus.text = "로딩 중..."
                    }
                    is GeneratorUiState.Success -> {
                        binding.tvStatus.text = state.message
                    }
                    is GeneratorUiState.Error -> {
                        binding.tvStatus.text = "오류: ${state.message}"
                        Timber.e("Error state: ${state.message}")
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.deviceStatus.collectLatest { status ->
                when (status) {
                    DeviceStatus.Disconnected -> {
                        binding.tvConnectionStatus.text = "연결 끊김"
                        binding.tvConnectionStatus.setTextColor(getColor(android.R.color.holo_red_dark))
                    }
                    DeviceStatus.Connected -> {
                        binding.tvConnectionStatus.text = "연결됨"
                        binding.tvConnectionStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                    }
                    DeviceStatus.Error -> {
                        binding.tvConnectionStatus.text = "오류"
                        binding.tvConnectionStatus.setTextColor(getColor(android.R.color.holo_orange_dark))
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.receivedData.collectLatest { logMessage ->
                val currentLog = binding.tvResponseData.text.toString()
                val newLog = if (currentLog.isEmpty()) {
                    logMessage
                } else {
                    "$currentLog\n$logMessage"
                }
                binding.tvResponseData.text = newLog

                // 로그가 너무 길어지면 오래된 내용 삭제
                if (binding.tvResponseData.lineCount > 100) {
                    val lines = newLog.split('\n')
                    binding.tvResponseData.text = lines.takeLast(50).joinToString("\n")
                }

                // 자동 스크롤
                binding.tvResponseData.post {
                    (binding.tvResponseData.parent as androidx.core.widget.NestedScrollView).smoothScrollTo(
                        0, binding.tvResponseData.height
                    )
                }
            }
        }
    }
}