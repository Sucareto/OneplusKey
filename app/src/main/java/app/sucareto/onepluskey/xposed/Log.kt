package app.sucareto.onepluskey.xposed

import io.github.libxposed.api.XposedInterface

internal object Log {
    private const val TAG = "OnePlusKey"

    @Volatile
    private var xposed: XposedInterface? = null

    fun attach(xposed: XposedInterface) {
        this.xposed = xposed
    }

    fun i(message: String) {
        android.util.Log.i(TAG, message)
        runCatching { xposed?.log(android.util.Log.INFO, TAG, message) }
    }

    fun w(message: String, throwable: Throwable? = null) {
        android.util.Log.w(TAG, message, throwable)
        runCatching { xposed?.log(android.util.Log.WARN, TAG, message, throwable) }
    }
}
