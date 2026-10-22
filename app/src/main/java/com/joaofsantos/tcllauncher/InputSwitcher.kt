package com.joaofsantos.tcllauncher

import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.widget.Toast

/** System integration hook. Session B supplies the HDMI implementation. */
interface InputSwitcher {
    fun switchTo(inputId: String)
}

/** Opens a passthrough input through the platform TV contract (HDMI 1 on this TCL). */
class TvInputSwitcher(private val context: Context) : InputSwitcher {
    override fun switchTo(inputId: String) {
        val uri = TvContract.buildChannelUriForPassthroughInput(inputId)
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            Toast.makeText(context, "Não foi possível abrir HDMI 1", Toast.LENGTH_SHORT).show()
        }
    }
}
