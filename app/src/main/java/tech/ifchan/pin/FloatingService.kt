package tech.ifchan.pin

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.*
import android.view.WindowManager.LayoutParams.*
import android.widget.ImageButton

class FloatingService : Service() {
    private lateinit var myBinder: IBinder

    private lateinit var imageButton: ImageButton
    private lateinit var layoutParams: WindowManager.LayoutParams
    private lateinit var windowManager: WindowManager
    private var startId: Int = 0
    private lateinit var gestureDetector: GestureDetector
    private lateinit var circularView: TransparentCircularRevealView
    private lateinit var circularLayoutParams: WindowManager.LayoutParams
    private var centerX: Int = 0
    private var centerY: Int = 0

    private var mediaProjection: MediaProjection? = null

    private var width: Int = 0
    private var height: Int = 0
    private var dpi: Int = 0
//    private var statusBarHeight = 0

    public enum class Control {
        START, STOP
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        this.startId = startId
        val bundle = intent?.extras
        if (bundle != null) {
            val control = bundle.getSerializable("Key")
            if (control != null) {
                when (control) {
                    Control.START -> {
                        showFloatingWindow()
                    }
                    Control.STOP -> {
                        stop()
                    }
                    else -> {
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        myBinder = MyBinder()
        imageButton = ImageButton(applicationContext)
        layoutParams = WindowManager.LayoutParams()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        width = displayMetrics.widthPixels
        height = displayMetrics.heightPixels
        dpi = displayMetrics.densityDpi

        super.onCreate()
    }

    private fun showFloatingWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            return
        }

        imageButton.setImageResource(R.drawable.scissor)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE
        }
        layoutParams.flags = FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCH_MODAL
        layoutParams.format = PixelFormat.RGBA_8888
        layoutParams.gravity = Gravity.TOP or Gravity.LEFT
        layoutParams.height = dpToPx(32.toFloat()).toInt()
        layoutParams.width = dpToPx(32.toFloat()).toInt()
        layoutParams.x = centerX
        layoutParams.y = centerY
        windowManager.addView(imageButton, layoutParams)

        // Circular View
        circularView = TransparentCircularRevealView(applicationContext)
        circularLayoutParams = WindowManager.LayoutParams()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            circularLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            circularLayoutParams.type = WindowManager.LayoutParams.TYPE_PHONE
        }
        circularLayoutParams.flags = FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCH_MODAL
        circularLayoutParams.format = PixelFormat.RGBA_8888
        circularLayoutParams.gravity = Gravity.TOP or Gravity.LEFT
        circularLayoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        circularLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        circularLayoutParams.x = 0
        circularLayoutParams.y = 0
        circularView.visibility = View.GONE
        windowManager.addView(circularView, circularLayoutParams)

        gestureDetector = GestureDetector(this, SingleTapConfirm())
        imageButton.setOnTouchListener(object : View.OnTouchListener {
            private var x: Int = 0
            private var y: Int = 0
            override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
                if (p1 == null) {
                    return false
                }
                // whether single tap or not.
                if (gestureDetector.onTouchEvent(p1)) {
                    // simple tap
                    if (mediaProjection == null) {
                        openScreenshotActivity()
                        return false
                    } else {
                        imageButton.visibility = View.GONE
                        // TODO: imageButton needs to be GONE imadiately!

                        val imageReader =
                            ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1)
                        mediaProjection?.createVirtualDisplay(   // why use '?.'?
                            "Pin",
                            width,
                            height,
                            dpi,
                            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                            imageReader.surface,
                            null,
                            null
                        )
                        // TODO: change more elegent
                        SystemClock.sleep(100)
                        val image = imageReader.acquireLatestImage()
                        if (image == null) {
                            return false
                        }
                        val buffer = image.planes[0].buffer
                        val pixelStride = image.planes[0].pixelStride
                        val rowStride = image.planes[0].rowStride
                        val rowPadding = rowStride - pixelStride * image.width
                        val bitmap = Bitmap.createBitmap(image.width + rowPadding / pixelStride, image.height, Bitmap.Config.ARGB_8888)
                        bitmap.copyPixelsFromBuffer(buffer)  // Now we get bitmap.
                        image.close()
                        // remove system bar?

                        circularView.visibility = View.VISIBLE
                        circularView.startCircularRevealAnim(
                            centerX.toFloat(),
                            centerY.toFloat(),
                            0f, bitmap
                            )
                    }
                } else {
                    when (p1.action) {
                        MotionEvent.ACTION_DOWN -> {
                            x = p1.rawX.toInt()
                            y = p1.rawY.toInt()
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val nowX = p1.rawX.toInt()
                            val nowY = p1.rawY.toInt()
                            val movedX = nowX - x
                            val movedY = nowY - y
                            x = nowX
                            y = nowY
                            layoutParams.x += movedX
                            layoutParams.y += movedY
                            centerX = layoutParams.x
                            centerY = layoutParams.y
//                            Log.d("@ifchan", "(" + centerX + ", " + centerY + ")")

                            //update
                            windowManager.updateViewLayout(imageButton, layoutParams)
                        }
                        else -> {
                        }
                    }
                }
                return false
            }

        })
    }

    private fun stop() {
        windowManager.removeViewImmediate(imageButton)
        stopSelf(startId)
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)
    }

    private fun openScreenshotActivity() {
        val intent = Intent(this@FloatingService, ScreenshotActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    fun setMediaProjection(mp: MediaProjection) {
        mediaProjection = mp
    }

    override fun onBind(intent: Intent): IBinder {
        return myBinder
    }

    // TODO: figure out.
    private class SingleTapConfirm : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            return true
        }
    }

    inner class MyBinder : Binder() {
        fun getService(): FloatingService {
            return this@FloatingService
        }
    }
}
