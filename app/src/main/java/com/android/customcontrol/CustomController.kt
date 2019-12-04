package com.android.customcontrol

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.SeekBar
import java.lang.Integer.min
import java.util.*
import kotlin.math.*

class CustomController @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0
) : SeekBar(context, attributeSet, defStyle) {

    companion object {
        const val DEFAULT_SWEEP_ANGLE = 180
        const val DEFAULT_START_ANGLE = 180
        const val OFFSET = 5F
    }

    private val a = attributeSet?.let {
        context.theme.obtainStyledAttributes(attributeSet, R.styleable.CustomController, defStyle, defStyleRes)
    }

    private var progressPaint: Paint = createBasePaint(Color.BLACK, 15F)
    private var progressFillPaint: Paint = createBasePaint(Color.YELLOW, 12F)

    private var oval = RectF()

    private var sweepAngle = a?.getInteger(R.styleable.CustomController_sweep_angle, DEFAULT_SWEEP_ANGLE) ?: DEFAULT_SWEEP_ANGLE
        set(value) = field.run { value.rem(360) }

    private var startAngle = a?.getInteger(R.styleable.CustomController_start_angle, DEFAULT_START_ANGLE) ?: DEFAULT_START_ANGLE

    private var offset = OFFSET
    private var arcRadius = 0
    private var thumbX = 0
    private var thumbY = 0
    private var thumbRadius = 0
    private var isDrugging = false
    private var center = Point()

    init {
        thumb = resources.getDrawable(R.drawable.smile, null)
        thumbRadius = thumb.intrinsicWidth / 2
        offset += thumbRadius
        a?.recycle()
    }

    private fun createBasePaint(color: Int, strokeWidth: Float) = Paint().apply {
        isAntiAlias = true
        this.color = color
        this.strokeWidth = strokeWidth
        style = Paint.Style.STROKE
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                updateOnTouch(event)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDrugging = false
            }
        }
        return true
    }

    private fun updateOnTouch(event: MotionEvent) {
        if (event.inCircle(center.x, center.y, arcRadius + thumbRadius)
            && !event.inCircle(center.x, center.y, arcRadius - thumbRadius)) {
            isDrugging = true

            var angle = atan((center.y - event.y) / (center.x - event.x)).toDouble()
            val realArcR = arcRadius - thumbRadius

            if (event.x < center.x) {
                thumbX = (center.x - realArcR * cos(angle)).toInt()
                thumbY = (center.y - realArcR * sin(angle)).toInt()
            } else {
                angle = -angle
                Log.d("Control", "${Math.toDegrees(angle)}")
                thumbX = (center.x + realArcR * cos(angle)).toInt()
                thumbY = (center.y - realArcR * sin(angle)).toInt()
            }

        }
    }

    override fun onDraw(canvas: Canvas?) {
        val currentProgressAngle = (progress / max.toFloat()) * sweepAngle
        canvas?.drawArc(oval, startAngle.toFloat(), sweepAngle.toFloat(), false, progressPaint)
        canvas?.drawArc(oval, startAngle.toFloat(), currentProgressAngle, false, progressFillPaint)
        canvas?.drawCircle(center.x.toFloat(), center.y.toFloat(), 5F, progressPaint)
        drawThumb(canvas)
        invalidate()
    }


    private fun drawThumb(canvas: Canvas?) {
        thumb.setBounds(thumbX - thumb.intrinsicWidth / 2, thumbY - thumb.intrinsicHeight / 2,
            thumbX + thumb.intrinsicWidth / 2, thumbY + thumb.intrinsicHeight / 2)

        canvas?.let { thumb.draw(it) }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        calculatePositions(w, h)
    }

    private fun calculatePositions(w: Int, h: Int) {
        arcRadius = min(w, h) / 2
        oval.set(offset, offset, arcRadius * 2 - offset, arcRadius * 2 - offset)
        center.set((arcRadius), (arcRadius))
        thumbX = arcRadius * cos(Math.toRadians(startAngle.toDouble())).toInt() + center.x
        thumbY = arcRadius * sin(Math.toRadians(startAngle.toDouble())).toInt() + center.y

        Log.d("CustomController", "$thumbX  $thumbY")
    }

    fun MotionEvent.inCircle(cx: Int, cy: Int, r: Int) : Boolean {
        return (x - cx).pow(2) + (y - cy).pow(2) < (r * r)
    }
}