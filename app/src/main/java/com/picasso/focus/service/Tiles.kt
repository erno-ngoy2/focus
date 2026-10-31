package com.picasso.focus.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.picasso.focus.FocusModeActivity
import com.picasso.focus.data.FocusController
import com.picasso.focus.data.FocusState
import com.picasso.focus.data.Fmt
import com.picasso.focus.data.Prefs
import com.picasso.focus.net.SystemControls

private fun TileService.launch(i: Intent) {
    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (Build.VERSION.SDK_INT >= 34) {
        startActivityAndCollapse(PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
    } else {
        @Suppress("DEPRECATION")
        startActivityAndCollapse(i)
    }
}

/** Tuile « Focus » : lance / arrête le mode concentration depuis le panneau rapide. */
class FocusTileService : TileService() {
    override fun onStartListening() = refresh()

    private fun refresh() {
        val t = qsTile ?: return
        val on = FocusState.session.value.active
        t.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        t.label = "Focus"
        if (Build.VERSION.SDK_INT >= 29) t.subtitle = if (on) "Actif" else "${Prefs.focusMinutes} min"
        t.updateTile()
    }

    override fun onClick() {
        if (FocusState.session.value.active) {
            FocusController.stop(this)
            refresh()
        } else {
            FocusController.start(this, Prefs.focusMinutes)
            launch(Intent(this, FocusModeActivity::class.java))
        }
    }
}

/** Tuile « Hotspot » : affiche la conso et ouvre les réglages du point d'accès. */
class HotspotTileService : TileService() {
    override fun onStartListening() {
        val t = qsTile ?: return
        val n = FocusState.net.value
        t.state = if (n.hotspotOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        t.label = "Hotspot"
        if (Build.VERSION.SDK_INT >= 29) t.subtitle = if (n.hotspotOn) Fmt.bytes(n.total) else "Inactif"
        t.updateTile()
    }

    override fun onClick() {
        val i = SystemControls.hotspotIntents().firstOrNull { it.resolveActivity(packageManager) != null }
            ?: Intent(android.provider.Settings.ACTION_SETTINGS)
        launch(i)
    }
}
