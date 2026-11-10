import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class MyVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnThread: Thread? = null
    private val isRunning = AtomicBoolean(false)

    private var isFreezerActive = false
    private var isTeleportActive = false
    private var isBlinkerActive = false

    companion object {
        const val ACTION_TOGGLE_FREEZER = "ACTION_TOGGLE_FREEZER"
        const val ACTION_TOGGLE_TELEPORT = "ACTION_TOGGLE_TELEPORT"
        const val ACTION_TOGGLE_BLINKER = "ACTION_TOGGLE_BLINKER"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE_FREEZER -> isFreezerActive = !isFreezerActive
            ACTION_TOGGLE_TELEPORT -> isTeleportActive = !isTeleportActive
            ACTION_TOGGLE_BLINKER -> isBlinkerActive = !isBlinkerActive
            else -> startVpnService()
        }
        return START_STICKY
    }

    private fun startVpnService() {
        if (isRunning.get()) return

        createNotificationChannel()
        val notification: Notification = NotificationCompat.Builder(this, "VpnChannel")
            .setContentTitle("Herramienta de Red Activa")
            .setContentText("Servicio local en ejecución")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .build()
        startForeground(1, notification)

        val builder = Builder()
        builder.setSession("AppPruebaRed")
        builder.addAddress("10.0.0.2", 24)
        builder.addRoute("0.0.0.0", 0)
        vpnInterface = builder.establish()

        isRunning.set(true)

        vpnThread = Thread({
            try {
                val pfd = vpnInterface ?: return@Thread
                val inputStream = FileInputStream(pfd.fileDescriptor)
                val outputStream = FileOutputStream(pfd.fileDescriptor)
                val buffer = ByteArray(32767)

                while (isRunning.get()) {
                    val length = inputStream.read(buffer)
                    if (length > 0) {
                        if (isFreezerActive) continue
                        if (isTeleportActive) Thread.sleep(1500) // Retardo de Teleport
                        if (isBlinkerActive) Thread.sleep(500)   // Retardo de Blinker

                        outputStream.write(buffer, 0, length)
                    }
                }
            } catch (e: Exception) {
                // Manejo de excepciones de red
            }
        }, "VpnReaderThread")

        vpnThread?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning.set(false)
        vpnThread?.interrupt()
        try {
            vpnInterface?.close()
        } catch (e: IOException) {
            // Error al cerrar
        }
        vpnInterface = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "VpnChannel",
                "Canal de Servicio VPN",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }
}
