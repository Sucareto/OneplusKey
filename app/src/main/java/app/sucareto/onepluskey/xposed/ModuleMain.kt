package app.sucareto.onepluskey.xposed

import android.content.SharedPreferences
import app.sucareto.onepluskey.config.ConfigKeys
import app.sucareto.onepluskey.config.KeyConfig
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam

@Suppress("unused")
class ModuleMain : XposedModule() {

    private fun preferences(): SharedPreferences {
        val preferences: SharedPreferences? =
            runCatching { getRemotePreferences(ConfigKeys.GROUP) }.getOrNull()
        if (preferences == null) Log.w("Remote preferences unavailable; using defaults")
        return preferences ?: EmptyPreferences
    }

    override fun onSystemServerStarting(param: SystemServerStartingParam) {
        Log.attach(this)
        val overlay = ConfigOverlay(preferences())
        val config = KeyConfig(overlay)
        val hooks = Hooks(this)
        val classLoader = param.classLoader

        val keyInstalled = KeyHook(config, overlay, classLoader).install(hooks)
        Log.i("KeyHook installed=$keyInstalled")

        ContextualSearch.install(this, hooks, classLoader)
    }

    override fun onPackageLoaded(param: PackageLoadedParam) {
        if (param.packageName != GOOGLE_PACKAGE || !param.isFirstPackage) return
        Log.attach(this)
        GoogleEligibility.install(Hooks(this), param.defaultClassLoader)
        Log.i("GoogleEligibility installed for ${param.packageName}")
    }
}
