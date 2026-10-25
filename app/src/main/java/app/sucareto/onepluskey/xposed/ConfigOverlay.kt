package app.sucareto.onepluskey.xposed

import android.content.SharedPreferences

internal class ConfigOverlay(private val base: SharedPreferences) : SharedPreferences {
    @Volatile
    private var pushed: Map<String, Any?>? = null

    fun replace(values: Map<String, Any?>) {
        pushed = values
    }

    private inline fun <reified T> read(key: String?, default: T, fromBase: () -> T): T {
        val source = pushed ?: return fromBase()
        return source[key] as? T ?: default
    }

    override fun getAll(): MutableMap<String, *> = (pushed ?: base.all).toMutableMap()
    override fun getString(key: String?, defValue: String?): String? =
        read(key, defValue) { base.getString(key, defValue) }
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        base.getStringSet(key, defValues)
    override fun getInt(key: String?, defValue: Int): Int = read(key, defValue) { base.getInt(key, defValue) }
    override fun getLong(key: String?, defValue: Long): Long = read(key, defValue) { base.getLong(key, defValue) }
    override fun getFloat(key: String?, defValue: Float): Float = read(key, defValue) { base.getFloat(key, defValue) }
    override fun getBoolean(key: String?, defValue: Boolean): Boolean =
        read(key, defValue) { base.getBoolean(key, defValue) }
    override fun contains(key: String?): Boolean = pushed?.containsKey(key) ?: base.contains(key)
    override fun edit(): SharedPreferences.Editor = throw UnsupportedOperationException()
    override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
}
