package app.sucareto.onepluskey.xposed

import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.Chain
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Executable

internal class Hooks(private val module: XposedModule) {

    fun hook(executable: Executable?, description: String, hooker: (Chain) -> Any?): Boolean {
        if (executable == null) {
            Log.w("Target not found: $description")
            return false
        }
        return runCatching {
            module.hook(executable)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain -> hooker(chain) }
            Log.i("Hooked: $description")
            true
        }.getOrElse {
            Log.w("Hook failed: $description", it)
            false
        }
    }

    fun deoptimize(executable: Executable?) {
        if (executable == null) return
        runCatching { module.deoptimize(executable) }
    }
}
