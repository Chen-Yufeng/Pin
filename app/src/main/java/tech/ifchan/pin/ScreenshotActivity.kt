package tech.ifchan.pin

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Rect
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.IBinder

class ScreenshotActivity : Activity() {
    private lateinit var floatingService: FloatingService
    private val mConnection = MyServiceConnection()
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var mediaProjection: MediaProjection
    private val REQUEST_CODE = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindService(
            Intent(this@ScreenshotActivity, FloatingService::class.java),
            mConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    private fun requestMediaProjection() {
        mediaProjectionManager =
                applicationContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
                floatingService.setMediaProjection(mediaProjection)

                // get status bar and navigation bar height
                val rect = Rect()
                val window = window
                window.decorView.getWindowVisibleDisplayFrame(rect)
                val statusBarHeight = rect.top
                val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
                var navigationBarHeight: Int
                if (resourceId > 0) {
                    navigationBarHeight = resources.getDimensionPixelSize(resourceId)
                } else {
                    navigationBarHeight = 0
                }
                floatingService.setBarHeight(statusBarHeight, navigationBarHeight)
            }
        }
        this.finish()
    }

    private inner class MyServiceConnection() : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            if (p1 != null) {
                val binder = p1 as FloatingService.MyBinder
                floatingService = binder.getService()
                requestMediaProjection()
            } else {
                this@ScreenshotActivity.finish()
            }
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
        }

    }
}
