package tech.ifchan.pin

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.view.View

/**
 * 透明圆形动画
 */
class TransparentCircularRevealView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var mCircleCenterX = 0f
    private var mCircleCenterY = 0f
    private var mStartCircleRadius = 0f
    private var mCurrentCircleRadius = mStartCircleRadius
    private val mTransPaint: Paint
    private var isAnimEnd = true
    private var isStarted = false

    init {
        mTransPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mTransPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OUT)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null, Canvas.ALL_SAVE_FLAG)
        //        canvas.drawColor(Color.WHITE);
        // 25% Gray
        canvas.drawColor(Color.parseColor("#40000000"))
        if (isStarted) {
            canvas.drawCircle(mCircleCenterX, mCircleCenterY, mCurrentCircleRadius, mTransPaint)
        }
    }

    fun startCircularRevealAnim(x: Float, y: Float, r: Float) {
        isStarted = true
        mCircleCenterX = x
        mCircleCenterY = y
        mStartCircleRadius = r
        if (!isAnimEnd) {
            return
        }
        isAnimEnd = false
        val maxRadius = Math.sqrt((width * width + height * height).toDouble()).toFloat()
        val valueAnimator = ValueAnimator.ofFloat(mStartCircleRadius, maxRadius)
            .setDuration(800)
        valueAnimator.addUpdateListener { animation ->
            mCurrentCircleRadius = animation.animatedValue as Float
            if (mCurrentCircleRadius >= maxRadius) {
                //                    mCurrentCircleRadius = mStartCircleRadius;
                isAnimEnd = true
            }
            postInvalidate()
        }
        valueAnimator.start()
    }

    fun reset() {
        isStarted = false
        mCircleCenterX = 0f
        mCircleCenterY = 0f
        mStartCircleRadius = 0f
        mCurrentCircleRadius = 0f
        postInvalidate()
    }
}
