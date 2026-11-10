import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton

class FloatingControlService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null

    private var isFreezerActive = false
    private var isTeleportActive = false
    private var isBlinkerActive = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_control, null)

        val LAYOUT_FLAG = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            LAYOUT_FLAG,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.x = 100
        params.y = 200

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        // Arrastrar ventana flotante
        floatingView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(floatingView, params)
                    true
                }
                else -> false
            }
        }

        val btnFreezer = floatingView?.findViewById<ImageButton>(R.id.btnFreezer)
        val btnTeleport = floatingView?.findViewById<ImageButton>(R.id.btnTeleport)
        val btnBlinker = floatingView?.findViewById<ImageButton>(R.id.btnBlinker)

        btnFreezer?.setOnClickListener {
            isFreezerActive = !isFreezerActive
            btnFreezer.setColorFilter(if (isFreezerActive) Color.parseColor("#00E676") else Color.parseColor("#FF5252"))
            sendVpnCommand(MyVpnService.ACTION_TOGGLE_FREEZER)
        }

        btnTeleport?.setOnClickListener {
            isTeleportActive = !isTeleportActive
            btnTeleport.setColorFilter(if (isTeleportActive) Color.parseColor("#00E676") else Color.parseColor("#448AFF"))
            sendVpnCommand(MyVpnService.ACTION_TOGGLE_TELEPORT)
        }

        btnBlinker?.setOnClickListener {
            isBlinkerActive = !isBlinkerActive
            btnBlinker.setColorFilter(if (isBlinkerActive) Color.parseColor("#00E676") else Color.parseColor("#69F0AE"))
            sendVpnCommand(MyVpnService.ACTION_TOGGLE_BLINKER)
        }

        windowManager?.addView(floatingView, params)
    }

    private fun sendVpnCommand(action: String) {
        val intent = Intent(this, MyVpnService::class.java).apply {
            this.action = action
        }
        startService(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (floatingView != null) {
            windowManager?.removeView(floatingView)
        }
    }
}
