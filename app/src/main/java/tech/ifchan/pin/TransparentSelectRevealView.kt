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
        canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
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
        Log.d("@ifchan", "Bitmap.height=${b.height} width=${b.width}")
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
                    var downX: Int = 0
                    var downY: Int = 0
                    var upX: Int = 0
                    var upY: Int = 0
                    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
                        if (p0 == null || p1 == null) {
                            return false
                        }
                        when (p1.action) {
                            MotionEvent.ACTION_DOWN -> {
                                Log.d("@ifchan", "Down")
                                complete = false
                                downX = checkCoordinate(p1.rawX.toInt() - p0.left, 0)
                                downY = checkCoordinate(p1.rawY.toInt() - p0.top, 1)
                            }
                            MotionEvent.ACTION_MOVE -> {
                                Log.d("@ifchan", "Move")
                                upX = checkCoordinate(p1.rawX.toInt() - p0.left, 0)
                                upY = checkCoordinate(p1.rawY.toInt() - p0.top, 1)
                                convertAndSetTwoRects( downX, downY, upX, upY )
                                invalidate()
                            }
                            MotionEvent.ACTION_UP -> {
                                Log.d("@ifchan", "Up")
                                complete = true
                                upX = checkCoordinate(p1.rawX.toInt() - p0.left, 0)
                                upY = checkCoordinate(p1.rawY.toInt() - p0.top, 1)
                            }
                            else -> {
                            }
                        }
                        if (complete) {
                            Log.d(
                                "@ifchan",
                                "downX=" + downX + "downY=" + downY + "upX=" + upX + "upY" + upY
                            )
                            convertAndSetTwoRects( downX, downY, upX, upY )
                            invalidate()
                            // crop the bitmap
                            val croppedBitmap = Bitmap.createBitmap(
                                bitmap,
                                min(downX, upX),
                                min(downY, upY),
                                abs(downX - upX),
                                abs(downY - upY)
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

    private fun convertAndSetTwoRects(downX: Int, downY: Int, upX: Int, upY: Int) {
        // Prevent rect from exceeding bitmap
        srcRect = Rect(
            min(checkCoordinate(downX, 0), checkCoordinate(upX, 0)),
            min(checkCoordinate(downY, 1), checkCoordinate(upY, 1)),
            max(checkCoordinate(downX, 0), checkCoordinate(upX, 0)),
            max(checkCoordinate(downY, 1), checkCoordinate(upY, 1))
        )
        desRect = srcRect as Rect
    }

    private fun checkCoordinate(raw: Int, flag: Int): Int {
        when (flag) {
            0 -> {
                return when {
                    raw < 0 -> 0
                    raw > bitmap.width -> bitmap.width
                    else -> raw
                }
            }
            1 -> {
                return when {
                    raw < 0 -> 0
                    raw > bitmap.height -> bitmap.height
                    else -> raw
                }
            }
            else -> {
                return 0
            }
        }
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
