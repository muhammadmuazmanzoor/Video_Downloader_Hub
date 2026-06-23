package com.video.avd.ui.equalizer.video

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.video.avd.R

/**
 * Created by Harjot on 23-May-16.
 */
class AnalogControllerVideo : View {
    var midx = 0f
    var midy = 0f
    var textPaint: Paint? = null
    var circlePaint: Paint? = null
    var circlePaint2: Paint? = null
    var linePaint: Paint? = null
    var angle: String? = null
    var currdeg = 0f
    var deg = 3f
    var downdeg = 0f
    var progressColor = 0
    var lineColor = 0
    var mListener: onProgressChangedListenerVideo? = null
    var label: String? = null

    interface onProgressChangedListenerVideo {
        fun onProgressChanged(progress: Int)
    }

    fun setOnProgressChangedListener(listener: onProgressChangedListenerVideo?) {
        mListener = listener
    }

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    fun init() {
        // Enable hardware layer for better GPU performance and to prevent rendering ANRs
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        
        val textColor = context.resources.getColor(R.color.gSelector)
        textPaint = Paint()
        textPaint!!.color = context.resources.getColor(R.color.brand_text_primary)
        textPaint!!.style = Paint.Style.FILL
        textPaint!!.textSize = 33f
        textPaint!!.isFakeBoldText = true
        textPaint!!.textAlign = Paint.Align.CENTER
        // Enable anti-aliasing for smoother rendering
        textPaint!!.isAntiAlias = true
        
        circlePaint = Paint()
        circlePaint!!.color = textColor
        circlePaint!!.style = Paint.Style.FILL
        circlePaint!!.isAntiAlias = true
        
        circlePaint2 = Paint()
        circlePaint2!!.color = textColor
        //        circlePaint2.setColor(Color.parseColor("#FFA036"));
        circlePaint2!!.style = Paint.Style.FILL
        circlePaint2!!.isAntiAlias = true
        
        linePaint = Paint()
        linePaint!!.color = textColor
        //        linePaint.setColor(Color.parseColor("#FFA036"));
        linePaint!!.strokeWidth = 7f
        linePaint!!.isAntiAlias = true
        angle = "0.0"
        label = "Label"
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Cache calculations to avoid repeated work
        midx = (canvas.width / 2).toFloat()
        midy = canvas.height / 2.2f
        val radius = (Math.min(midx, midy) * (14.5.toFloat() / 16)).toInt()
        val radiusFloat = radius.toFloat()
        val deg2 = Math.max(3f, deg)
        val deg3 = Math.min(deg, 21f)
        
        // Cache color values to avoid parsing repeatedly
        val grayColor = Color.parseColor("#B8B8B8")
        val blackColor = Color.parseColor("#000000")
        
        // Pre-calculate constants
        val circleRadius = radiusFloat / 15
        val twoPi = (2 * Math.PI).toFloat()
        val pi24 = 1.0f / 24
        
        // Draw inactive circles (gray)
        circlePaint!!.color = grayColor
        for (i in deg2.toInt()..21) {
            val tmp = i * pi24
            val angle = twoPi * (1.0f - tmp)
            val sinAngle = Math.sin(angle.toDouble()).toFloat()
            val cosAngle = Math.cos(angle.toDouble()).toFloat()
            val x = midx + (radiusFloat * sinAngle)
            val y = midy + (radiusFloat * cosAngle)
            canvas.drawCircle(x, y, circleRadius, circlePaint!!)
        }
        
        // Draw active circles (colored)
        var i = 3
        while (i <= deg3) {
            val tmp = i * pi24
            val angle = twoPi * (1.0f - tmp)
            val sinAngle = Math.sin(angle.toDouble()).toFloat()
            val cosAngle = Math.cos(angle.toDouble()).toFloat()
            val x = midx + (radiusFloat * sinAngle)
            val y = midy + (radiusFloat * cosAngle)
            canvas.drawCircle(x, y, circleRadius, circlePaint2!!)
            i++
        }
        
        // Draw indicator line
        val tmp2 = deg * pi24
        val angle2 = twoPi * (1.0f - tmp2)
        val sinAngle2 = Math.sin(angle2.toDouble()).toFloat()
        val cosAngle2 = Math.cos(angle2.toDouble()).toFloat()
        val x1 = midx + (radiusFloat * (2f / 5) * sinAngle2)
        val y1 = midy + (radiusFloat * (2f / 5) * cosAngle2)
        val x2 = midx + (radiusFloat * (3f / 5) * sinAngle2)
        val y2 = midy + (radiusFloat * (3f / 5) * cosAngle2)
        
        // Draw outer circles
        circlePaint!!.color = grayColor
        canvas.drawCircle(midx, midy, radiusFloat * (13f / 15), circlePaint!!)
        circlePaint!!.color = blackColor
        canvas.drawCircle(midx, midy, radiusFloat * (11f / 15), circlePaint!!)

        // Draw label text
        textPaint!!.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val textMargin = (radiusFloat * 1.3)
        canvas.drawText(label!!, midx, (midy + textMargin).toFloat(), textPaint!!)
        canvas.drawLine(x1, y1, x2, y2, linePaint!!)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        mListener!!.onProgressChanged((deg - 2).toInt())
        if (e.action == MotionEvent.ACTION_DOWN) {
            val dx = e.x - midx
            val dy = e.y - midy
            downdeg = (Math.atan2(dy.toDouble(), dx.toDouble()) * 180 / Math.PI).toFloat()
            downdeg -= 90f
            if (downdeg < 0) {
                downdeg += 360f
            }
            downdeg = Math.floor((downdeg / 15).toDouble()).toFloat()
            return true
        }
        if (e.action == MotionEvent.ACTION_MOVE) {
            val dx = e.x - midx
            val dy = e.y - midy
            currdeg = (Math.atan2(dy.toDouble(), dx.toDouble()) * 180 / Math.PI).toFloat()
            currdeg -= 90f
            if (currdeg < 0) {
                currdeg += 360f
            }
            currdeg = Math.floor((currdeg / 15).toDouble()).toFloat()
            if (currdeg == 0f && downdeg == 23f) {
                deg++
                if (deg > 21) {
                    deg = 21f
                }
                downdeg = currdeg
            } else if (currdeg == 23f && downdeg == 0f) {
                deg--
                if (deg < 3) {
                    deg = 3f
                }
                downdeg = currdeg
            } else {
                deg += currdeg - downdeg
                if (deg > 21) {
                    deg = 21f
                }
                if (deg < 3) {
                    deg = 3f
                }
                downdeg = currdeg
            }
            angle = deg.toString()
            // Use postInvalidateOnAnimation for smoother updates that sync with VSYNC
            // This prevents excessive invalidate calls and reduces GPU load
            postInvalidateOnAnimation()
            return true
        }
        return e.action == MotionEvent.ACTION_UP || super.onTouchEvent(e)
    }

    var progress: Int
        get() = (deg - 2).toInt()
        set(param) {
            deg = (param + 2).toFloat()
        }
}