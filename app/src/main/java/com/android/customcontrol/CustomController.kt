package com.android.customcontrol

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.SeekBar
import java.lang.Integer.min
import kotlin.math.*

data class DPoint(var x: Double = 0.0, var y: Double = 0.0)

class CustomController @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0
) : SeekBar(context, attributeSet, defStyle) {

    interface OnProgressListener {
        fun onProgressChanged(progress: Float)
    }

    companion object {
        const val DEFAULT_SWEEP_ANGLE = 180
        const val DEFAULT_START_ANGLE = 180

        const val DEFAULT_STROKE_BASE_WIDTH = 70F
        const val DEFAULT_STROKE_FILL_WIDTH = 60F
        const val OFFSET = 10F
    }

    var onProgressChangedListener: (OnProgressListener)? = null

    private val a = attributeSet?.let {
        context.theme.obtainStyledAttributes(attributeSet, R.styleable.CustomController, defStyle, defStyleRes)
    }

    private var sweepAngle = a.retrieveAttrOrDefault(DEFAULT_SWEEP_ANGLE)
        {getInteger(R.styleable.CustomController_sweep_angle, it)}
        set(value) = field.run { value.rem(360) }

    private var startAngle = a.retrieveAttrOrDefault(DEFAULT_START_ANGLE)
        {getInteger(R.styleable.CustomController_start_angle, it)}

    private var strokeBaseWidth = a.retrieveAttrOrDefault(DEFAULT_STROKE_BASE_WIDTH)
        {getFloat(R.styleable.CustomController_stroke_base_width, it)}

    private var strokeFillWidth = a.retrieveAttrOrDefault(DEFAULT_STROKE_FILL_WIDTH)
        {getFloat(R.styleable.CustomController_stroke_fill_width, it)}

    private var offset = a.retrieveAttrOrDefault(OFFSET) {getFloat(R.styleable.CustomController_offset, it)}
    private var baseColor = a.retrieveAttrOrDefault(Color.BLACK) {getInteger(R.styleable.CustomController_base_color, it)}
    private var progressFillColor = a.retrieveAttrOrDefault(Color.BLACK) {getInteger(R.styleable.CustomController_fill_color, it)}
    private var useSmooth = a.retrieveAttrOrDefault(true) {getBoolean(R.styleable.CustomController_use_smooth, it)}
    private var textColor = a.retrieveAttrOrDefault(Color.BLACK) {getInteger(R.styleable.CustomController_text_color, it)}
    private val textSize = a.retrieveAttrOrDefault(200f) {getFloat(R.styleable.CustomController_text_size, it)}

    private var progressPaint: Paint = createBasePaint(baseColor, strokeBaseWidth)
    private var progressFillPaint: Paint = createBasePaint(progressFillColor, strokeFillWidth)

    private var textPaint = Paint().apply {
        color = textColor
        style = Paint.Style.FILL
        textSize = this@CustomController.textSize
    }

    private var progressForThumb = progress.toFloat()
        get() = if (useSmooth) field else progress.toFloat()
    private var oval = RectF()
    private val stopAngle get() = startAngle + sweepAngle
    private var arcRadius = 0
    private var thumbRadius = 0
    private var isDrugging = false
    private var center = Point()

    init {
        thumb = a?.getDrawable(R.styleable.CustomController_thumb) ?: resources.getDrawable(R.drawable.ic_happy, null)
        thumbRadius = DEFAULT_STROKE_BASE_WIDTH.toInt()
        offset += thumbRadius
        a?.recycle()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                updateOnTouch(event)
                isDrugging = true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDrugging = false
            }
        }
        return true
    }

    private fun updateOnTouch(event: MotionEvent) {
        val touchAngle = getAngleDegAtPosition(event.x, event.y).toInt()
        var isTouchInBarArea = false

        if (touchAngle in (startAngle..stopAngle) || (stopAngle.rem(360) in (touchAngle + 1)..startAngle)) {
            isTouchInBarArea = true
        }
        if (isDrugging && (progress == 0 && touchAngle !in startAngle..startAngle + 5
                    || progress == max  && touchAngle !in stopAngle.rem(360) - 5..stopAngle.rem(360))) {
            return
        }

        if (event.inCircle(center.x, center.y, arcRadius + thumbRadius)
            && !event.inCircle(center.x, center.y, arcRadius - thumbRadius)
            && isTouchInBarArea) {

            var progressAngleFromStart = touchAngle - startAngle
            if (touchAngle < startAngle) progressAngleFromStart += 360

            val progressRatio = progressAngleFromStart / sweepAngle.toFloat()

            val newProgress = (progressRatio * max)
//            if (isDrugging && (progress == 0 && newProgress.toInt() != 1 || progress == max  && newProgress.toInt() != max - 1)) {
//                return
//            }
            progress = round(newProgress).toInt()
            progressForThumb = newProgress
            onProgressChangedListener?.onProgressChanged(progress.toFloat())
        }
    }

    override fun onDraw(canvas: Canvas?) {
        val currentProgressAngleFromStart = (progressForThumb / max.toFloat()) * sweepAngle

        canvas?.drawArc(oval, startAngle.toFloat(), sweepAngle.toFloat(), false, progressPaint)
        canvas?.drawArc(oval, startAngle.toFloat(), currentProgressAngleFromStart, false, progressFillPaint)
        canvas?.drawText("$progress", (width - textPaint.measureText("$progress", 0, "$progress".length)) / 2f,
            height / 2f - (textPaint.descent() + textPaint.ascent() / 2), textPaint)
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

        } else if (x < center.x && y <= center.y) {
            angle = Math.PI + atan((center.y - y) / (center.x - x))

        } else if (x >= center.x && y <= center.y) {
            angle = Math.PI * 3/2 + atan((x - center.x) / (center.y - y))
        }

        return Math.toDegrees(angle).toFloat()
    }

    private fun getPosAtAngle(angle: Float) : DPoint {
        val angleRad = Math.toRadians(angle.rem(360).toDouble())
        val pos = DPoint()

        when (angle.rem(360)) {
            in 0f..90f -> {
                pos.x = (center.x + arcRadius * cos(angleRad))
                pos.y = (center.y + arcRadius * sin(angleRad))
            }

            in 91f..180f -> {
                val betta = angleRad - Math.PI / 2
                pos.x = (center.x - arcRadius * sin(betta))
                pos.y = (center.y + arcRadius * cos(betta))
            }

            in 181f..270f -> {
                val betta = angleRad - Math.PI
                pos.x = (center.x - arcRadius * cos(betta))
                pos.y = (center.y - arcRadius * sin(betta))
            }

            in 271f..360f -> {
                val betta = angleRad + Math.PI / 2
                pos.x = (center.x + arcRadius * sin(betta))
                pos.y = (center.y - arcRadius * cos(betta))
            }
        }

        return pos
    }

    private fun drawThumb(canvas: Canvas?, pos: DPoint) {
        thumb.setBounds(pos.x.toInt() - strokeBaseWidth.toInt(), pos.y.toInt() - strokeBaseWidth.toInt(),
            pos.x.toInt() + strokeBaseWidth.toInt(), pos.y.toInt() + strokeBaseWidth.toInt())

        canvas?.let { thumb.draw(it) }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        arcRadius = min(w, h) / 2 - offset.toInt()
        center.set(min(w, h) / 2, min(w, h) / 2)
        oval.set(center.x - arcRadius.toFloat(), center.y - arcRadius.toFloat(),
            center.x + arcRadius.toFloat(), center.y + arcRadius.toFloat())

        applySizeDependentStyles()
    }

    private fun createBasePaint(color: Int, strokeWidth: Float) = Paint().apply {
        isAntiAlias = true
        this.color = color
        this.strokeWidth = strokeWidth
        style = Paint.Style.STROKE
    }

    private fun applySizeDependentStyles() {
        progressFillPaint.apply {
            val gradient = SweepGradient(center.x.toFloat(), center.y.toFloat(), Color.YELLOW, Color.RED)
            shader = gradient
        }
    }
}