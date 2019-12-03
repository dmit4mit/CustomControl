package com.android.customcontrol

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.SeekBar

class CustomController @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0
) : SeekBar(context, attributeSet, defStyle) {

    private val a = attributeSet?.let {
        context.theme.obtainStyledAttributes(attributeSet, R.styleable.CustomController, defStyle, defStyleRes)
    }

    init {
        a?.recycle()
    }

    private var progressPaint: Paint = createBasePaint(Color.BLACK, 7F)
    private var progressFillPaint: Paint = createBasePaint(Color.YELLOW, 10F)

    private var controlRect = RectF()

    private fun createBasePaint(color: Int, strokeWidth: Float) = Paint().apply {
        isAntiAlias = true
        this.color = color
        this.strokeWidth = strokeWidth
        style = Paint.Style.STROKE
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.drawArc(controlRect, 180F, max.toFloat(), false, progressPaint)
        canvas?.drawArc(controlRect, 180F, progress.toFloat(), false, progressFillPaint)
        invalidate()
    }
}