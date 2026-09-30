package lsafer.edgeseek.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

/**
 * Quick Settings Tile to toggle EdgeSeek edge overlay gesture detection ON or OFF.
 * Works seamlessly across Android 7.0 (API 24) through Android 15 (API 35).
 */
class EdgeSeekTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()

        // 1. Verify Display Over Other Apps (SYSTEM_ALERT_WINDOW) permission
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "EdgeSeek requires overlay permission to run", Toast.LENGTH_LONG).show()
            val permissionIntent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this, 0, permissionIntent, PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(permissionIntent)
            }
            return
        }

        // 2. Toggle EdgeSeek state in preferences
        val prefs = getSharedPreferences("edgeseek", Context.MODE_PRIVATE)
        val isRunning = prefs.getBoolean("service_enabled", false)
        val newState = !isRunning

        prefs.edit().putBoolean("service_enabled", newState).apply()

        // 3. Notify EdgeSeek to start or pause edge listeners
        val broadcastIntent = Intent("lsafer.edgeseek.ACTION_TOGGLE").apply {
            setPackage(packageName)
            putExtra("enabled", newState)
        }
        sendBroadcast(broadcastIntent)

        // 4. Update the tile visuals immediately
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return

        // If overlay permission is missing, show as unavailable
        if (!Settings.canDrawOverlays(this)) {
            tile.state = Tile.STATE_UNAVAILABLE
            tile.label = "EdgeSeek"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Permission needed"
            }
            tile.updateTile()
            return
        }

        val prefs = getSharedPreferences("edgeseek", Context.MODE_PRIVATE)
        val isRunning = prefs.getBoolean("service_enabled", false)

        tile.state = if (isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "EdgeSeek"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (isRunning) "Active" else "Off"
        }

        tile.updateTile()
    }
}
