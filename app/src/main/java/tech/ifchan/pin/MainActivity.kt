package tech.ifchan.pin

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

private const val REQUEST_CODE_OVERLAY = 1001

class MainActivity : AppCompatActivity() {
    private var isStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // set window background
//        val rootView = toggle.rootView
//        rootView.setBackgroundColor(Color.BLACK)

        requestPermission()
        init()
        initBasicView()
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                startActivityForResult(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    ), REQUEST_CODE_OVERLAY
                )
            }
        }
    }

    @SuppressLint("NewApi")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OVERLAY) {
            if (!Settings.canDrawOverlays(this)) {
                finish()
            }
        }
    }

    private fun init() {
        isStarted = isFloatingServiceRunning()
    }

    private fun initBasicView() {
        toggle.setOnClickListener {
            val intent = Intent(this@MainActivity, FloatingService::class.java)
            val bundle = Bundle()
            if (!isStarted) {
                bundle.putSerializable("Key", FloatingService.Control.START)
                intent.putExtras(bundle)
                startService(intent)
            } else {
                bundle.putSerializable("Key", FloatingService.Control.STOP)
                intent.putExtras(bundle)
                startService(intent)
            }
            isStarted = !isStarted
        }
    }

    private fun isFloatingServiceRunning(): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (FloatingService::class.java.name.equals(service.service.className)) {
                return true
            }
        }
        return false
    }
}
