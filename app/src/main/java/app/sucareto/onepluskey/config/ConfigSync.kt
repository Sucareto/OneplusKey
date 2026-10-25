package app.sucareto.onepluskey.config

import android.app.BroadcastOptions
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle

object ConfigSync {
    fun push(context: Context, prefs: SharedPreferences) {
        val bundle = Bundle()
        prefs.all.forEach { (key, value) ->
            when (value) {
                is Boolean -> bundle.putBoolean(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Float -> bundle.putFloat(key, value)
                is String -> bundle.putString(key, value)
            }
        }
        val intent = Intent(Protocol.ACTION_CONFIG)
            .setPackage(Protocol.SYSTEM_PACKAGE)
            .putExtra(Protocol.EXTRA_CONFIG, bundle)
        val options = BroadcastOptions.makeBasic().setShareIdentityEnabled(true).toBundle()
        runCatching { context.sendBroadcast(intent, null, options) }
    }
}
