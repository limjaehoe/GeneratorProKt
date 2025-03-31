package com.androidkotlin.generatorprokt.presentation.common.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import com.androidkotlin.generatorprokt.domain.model.MainMode
import timber.log.Timber

/**
 * 발전기 상태(MAIN_MODE)를 시각적으로 보여주는 커스텀 뷰
 */
class GeneratorStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textSize = 40f
    }

    private val textBounds = Rect()

    // 현재 발전기 상태
    var currentMode: MainMode = MainMode.NONE
        set(value) {
            field = value
            Timber.d("GeneratorStateView - 모드 변경: $value (${value.hexValue})")
            //invalidate() // 상태가 변경되면 뷰 다시 그리기
            postInvalidate()
        }

    // 상태별 색상 정의
    private val modeColors = mapOf(
        MainMode.NONE to Color.GRAY,
        MainMode.BOOT to Color.BLUE,
        MainMode.INIT to Color.CYAN,
        MainMode.STANDBY to Color.GREEN,
        MainMode.EXPOSURE_READY to Color.YELLOW,
        MainMode.EXPOSURE_READY_DONE to Color.rgb(255, 200, 0), // 진한 노랑
        MainMode.EXPOSURE to Color.RED,
        MainMode.EXPOSURE_DONE to Color.rgb(255, 100, 100), // 연한 빨강
        MainMode.EXPOSURE_RELEASE to Color.rgb(200, 100, 100),
        MainMode.RESET to Color.rgb(150, 150, 255),
        MainMode.TECHNICAL_MODE to Color.rgb(150, 150, 255),
        MainMode.EMERGENCY to Color.rgb(255, 0, 0),
        MainMode.ERROR to Color.rgb(255, 0, 0)
    )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 로그로 onDraw가 호출되었는지 확인
        Timber.d("GeneratorStateView - onDraw 호출: 현재 모드 = $currentMode")
        Timber.d("GeneratorStateView - onDraw 크기: 너비=$width, 높이=$height")
        Timber.d("GeneratorStateView - 모드 그리기: ${currentMode.name}, 16진수: 0x${currentMode.hexValue.toString(16)}")


        // 배경 색상 설정
        val bgColor = modeColors[currentMode] ?: Color.GRAY
        paint.color = bgColor

        // 배경 그리기
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // 텍스트 색상 설정 (배경색에 따라 조정)
        val textColor = if (isDarkColor(bgColor)) Color.WHITE else Color.BLACK
        paint.color = textColor

        // 발전기 상태 텍스트 표시
        val stateText = "상태: ${currentMode.name} (0x${currentMode.hexValue.toString(16).uppercase()})"
        paint.getTextBounds(stateText, 0, stateText.length, textBounds)

        val x = (width - textBounds.width()) / 2f
        val y = (height + textBounds.height()) / 2f
        canvas.drawText(stateText, x, y, paint)

        // 추가 설명 텍스트 표시 (있는 경우)
        if (currentMode.korDescription.isNotEmpty()) {
            val descText = currentMode.korDescription
            paint.textSize = 30f
            paint.getTextBounds(descText, 0, descText.length, textBounds)

            val descX = (width - textBounds.width()) / 2f
            val descY = y + textBounds.height() + 20f
            canvas.drawText(descText, descX, descY, paint)
        }
    }

    /**
     * 색상이 어두운지 판단하는 헬퍼 메서드
     */
    private fun isDarkColor(color: Int): Boolean {
        val darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness >= 0.5
    }

    // 올바른 측정을 보장하기 위해 이 메서드 추가
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        Timber.d("GeneratorStateView - onMeasure 호출됨: " +
                "너비=${MeasureSpec.getSize(widthMeasureSpec)}, " +
                "높이=${MeasureSpec.getSize(heightMeasureSpec)}")
    }

}