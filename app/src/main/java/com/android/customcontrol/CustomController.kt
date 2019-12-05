package com.android.customcontrol

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.widget.SeekBar
import java.lang.Integer.min
import kotlin.math.*

class CustomController @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0
) : SeekBar(context, attributeSet, defStyle) {

    interface onProgressListener {
        fun onProgressChanged(progress: Float)
    }

    companion object {
        const val DEFAULT_SWEEP_ANGLE = 180
        const val DEFAULT_START_ANGLE = 180

        const val DEFAULT_STROKE_BASE_WIDTH = 50F
        const val DEFAULT_STROKE_FILL_WIDTH = 45F
        const val OFFSET = 5F
    }

    var onProgressChangedListener: (onProgressListener)? = null

    private val a = attributeSet?.let {
        context.theme.obtainStyledAttributes(attributeSet, R.styleable.CustomController, defStyle, defStyleRes)
    }

    private var progressPaint: Paint = createBasePaint(Color.BLACK, DEFAULT_STROKE_BASE_WIDTH)
    private var progressFillPaint: Paint = createBasePaint(Color.YELLOW, DEFAULT_STROKE_FILL_WIDTH)

    private var oval = RectF()

    private var sweepAngle = a?.getInteger(R.styleable.CustomController_sweep_angle, DEFAULT_SWEEP_ANGLE) ?: DEFAULT_SWEEP_ANGLE
        set(value) = field.run { value.rem(360) }

    private var startAngle = a?.getInteger(R.styleable.CustomController_start_angle, DEFAULT_START_ANGLE) ?: DEFAULT_START_ANGLE

    private val stopAngle
        get() = startAngle + sweepAngle
    private var isThumbShown = true
    private var offset = OFFSET
    private var arcRadius = 0
    private var thumbRadius = 0
    private var isDrugging = false
    private var center = Point()

    init {
        thumb = resources.getDrawable(R.drawable.smile, null)
        thumbRadius = DEFAULT_STROKE_BASE_WIDTH.toInt()
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
        val touchAngle = getAngleDegAtPosition(event.x, event.y).toInt()
        var isTouchInBar = false
        if (touchAngle in (startAngle..stopAngle)) {
            isTouchInBar = true
        }
        if (stopAngle.rem(360) < startAngle && touchAngle < stopAngle.rem(360)) {
            isTouchInBar = true
        }
        if (event.inCircle(center.x, center.y, arcRadius + thumbRadius)
            && !event.inCircle(center.x, center.y, arcRadius - thumbRadius)
            && isTouchInBar) {
            var progressAngleFromStart = touchAngle - startAngle

            if (touchAngle < startAngle) {
                progressAngleFromStart = 360 + touchAngle - startAngle
            }

            val progressRatio = progressAngleFromStart / sweepAngle.toFloat()
            progress = (progressRatio * max).toInt()
            onProgressChangedListener?.onProgressChanged(progress.toFloat())
        }
    }

    override fun onDraw(canvas: Canvas?) {
        val currentProgressAngleFromStart = (progress / max.toFloat()) * sweepAngle

        canvas?.drawArc(oval, startAngle.toFloat(), sweepAngle.toFloat(), false, progressPaint)
        canvas?.drawArc(oval, startAngle.toFloat(), currentProgressAngleFromStart, false, progressFillPaint)
        canvas?.drawCircle(center.x.toFloat(), center.y.toFloat(), 5F, progressPaint)
        val pos = getPosAtAngle(currentProgressAngleFromStart + startAngle)
        drawThumb(canvas, pos)
        invalidate()
    }

    private fun getAngleDegAtPosition(x: Float, y: Float) : Float {
        var angle = 0.0


        if (x > center.x && y > center.y) {
            angle = atan((y - center.y) / (x - center.x)).toDouble()

        } else if (x <= center.x && y > center.y) {
            angle = Math.PI / 2 + atan((center.x - x) / (y - center.y))

        } else if (x < center.x && y < center.y) {
            angle = Math.PI + atan((center.y - y) / (center.x - x))

        } else if (x >= center.x && y < center.y) {
            angle = Math.PI * 3/2 + atan((x - center.x) / (center.y - y))
        }

        return Math.toDegrees(angle).toFloat()
    }

    private fun getPosAtAngle(angle: Float) : Point {
        val angleRad = Math.toRadians(angle.rem(360).toDouble())

        var pos = Point()
        when (angle.rem(360)) {
            in 0f..90f -> {
                pos.x = (center.x + arcRadius * cos(angleRad)).toInt()
                pos.y = (center.y + arcRadius * sin(angleRad)).toInt()
            }

            in 91f..180f -> {
                val betta = angleRad - Math.PI / 2
                pos.x = (center.x - arcRadius * sin(betta)).toInt()
                pos.y = (center.y + arcRadius * cos(betta)).toInt()
            }

            in 181f..270f -> {
                val betta = angleRad - Math.PI
                pos.x = (center.x - arcRadius * cos(betta)).toInt()
                pos.y = (center.y - arcRadius * sin(betta)).toInt()
            }

            in 271f..359f -> {
                val betta = angleRad + Math.PI / 2
                pos.x = (center.x + arcRadius * sin(betta)).toInt()
                pos.y = (center.y - arcRadius * cos(betta)).toInt()
            }
        }
        return pos
    }


    private fun drawThumb(canvas: Canvas?, pos: Point) {

        thumb.setBounds(pos.x - thumb.intrinsicWidth / 2, pos.y - thumb.intrinsicHeight / 2,
            pos.x + thumb.intrinsicWidth / 2, pos.y + thumb.intrinsicHeight / 2)

        canvas?.let { thumb.draw(it) }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        calculatePositions(w, h)
    }

    private fun calculatePositions(w: Int, h: Int) {
        arcRadius = min(w, h) / 2 - offset.toInt()
        center.set(min(w, h) / 2, min(w, h) / 2)

        progressFillPaint.apply {
            val gradient = SweepGradient(center.x.toFloat(), center.y.toFloat(), Color.YELLOW, Color.RED)
            shader = gradient
        }

        oval.set(center.x - arcRadius.toFloat(), center.y - arcRadius.toFloat(),
            center.x + arcRadius.toFloat(), center.y + arcRadius.toFloat())
    }

    fun MotionEvent.inCircle(cx: Int, cy: Int, r: Int) : Boolean {
        return (x - cx).pow(2) + (y - cy).pow(2) < (r * r)
    }

    private fun log(s: String) {
        Log.d("Controller", s)
    }
}