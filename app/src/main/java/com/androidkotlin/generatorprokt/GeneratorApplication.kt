package com.androidkotlin.generatorprokt

import android.app.Application
import android.util.Log
import com.hoho.android.usbserial.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class GeneratorApplication : Application() {
    override fun onCreate() {
        super.onCreate()

//        // Timber 초기화
//        if (BuildConfig.DEBUG) {
//            Timber.plant(Timber.DebugTree())
//        }

        // Timber 초기화 - 임포트 확인 및 강제 초기화
        Timber.plant(Timber.DebugTree())

        // 초기화 여부 테스트 로그
        Timber.d("Timber 초기화 완료")
        Log.d("GeneratorApp", "앱 시작 - Timber 초기화 완료")
    }
}