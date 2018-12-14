package tech.ifchan.pin

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 透明圆形动画
 */
class TransparentSelectRevealView @JvmOverloads constructor(
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

    private lateinit var bitmap: Bitmap
    private var srcRect: Rect? = null
    private lateinit var desRect: Rect

    private var callBack: CallBack? = null

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
            canvas.drawBitmap(
                getCroppedBitmap(
                    bitmap,
                    mCircleCenterX,
                    mCircleCenterY,
                    mCurrentCircleRadius
                ), srcRect, desRect, null
            )
//            canvas.drawCircle(mCircleCenterX, mCircleCenterY, mCurrentCircleRadius, mTransPaint)
        }
    }

    fun startCircularRevealAnim(x: Float, y: Float, r: Float, b: Bitmap) {
        bitmap = b
        desRect = Rect(0, 0, width, height)
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
                setOnTouchListener(object : OnTouchListener {  // After animation is done.
                    var complete = false
                    var downX: Float = 0f
                    var downY: Float = 0f
                    var upX: Float = 0f
                    var upY: Float = 0f
                    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
                        if (p0 == null || p1 == null) {
                            return false
                        }
                        when (p1.action) {
                            MotionEvent.ACTION_DOWN -> {
                                Log.d("@ifchan", "Down")
                                complete = false
                                downX = p1.rawX - p0.left
                                downY = p1.rawY - p0.top
                            }
                            MotionEvent.ACTION_MOVE -> {
                                Log.d("@ifchan", "Move")
                                upX = p1.rawX - p0.left
                                upY = p1.rawY - p0.top
                                convertAndSetTwoRects(
                                    downX.toInt(),
                                    downY.toInt(),
                                    upX.toInt(),
                                    upY.toInt()
                                )
                                invalidate()
                            }
                            MotionEvent.ACTION_UP -> {
                                Log.d("@ifchan", "Up")
                                complete = true
                                upX = p1.rawX - p0.left
                                upY = p1.rawY - p0.top
                            }
                            else -> {
                            }
                        }
                        if (complete) {
                            Log.d(
                                "@ifchan",
                                "downX=" + downX + "downY=" + downY + "upX=" + upX + "upY" + upY
                            )
                            convertAndSetTwoRects(
                                downX.toInt(),
                                downY.toInt(),
                                upX.toInt(),
                                upY.toInt()
                            )
                            invalidate()
                            // crop the bitmap
                            val croppedBitmap = Bitmap.createBitmap(
                                bitmap,
                                min(downX, upX).toInt(),
                                min(downY, upY).toInt(),
                                abs(downX - upX).toInt(),
                                abs(downY - upY).toInt()
                            )
                            srcRect?.let { callBack?.onSelectCompleted(croppedBitmap, it) }
                            reset()
                        }

                        return false
                    }
                })
            }
            postInvalidate()
        }
        valueAnimator.start()
    }

    private fun safetifyCoordinate(m: MotionEvent, which: Int) {

    }

    private fun convertAndSetTwoRects(downX: Int, downY: Int, upX: Int, upY: Int) {
        srcRect = Rect(min(downX, upX), min(downY, upY), max(downX, upX), max(downY, upY))
        desRect = srcRect as Rect
    }

    private fun getCroppedBitmap(bitmap: Bitmap, x: Float, y: Float, r: Float): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val color = 0xff424242
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)

        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color.toInt()
        canvas.drawCircle(x, y, r, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        return output
    }

    fun reset() {
        isStarted = false
        mCircleCenterX = 0f
        mCircleCenterY = 0f
        mStartCircleRadius = 0f
        mCurrentCircleRadius = 0f
        srcRect = null
        desRect = Rect(0, 0, width, height)
        postInvalidate()
    }

    fun setCallBack(c: CallBack) {
        callBack = c
    }

    interface CallBack {
        fun onSelectCompleted(bitmap: Bitmap, rect: Rect)
    }
}
