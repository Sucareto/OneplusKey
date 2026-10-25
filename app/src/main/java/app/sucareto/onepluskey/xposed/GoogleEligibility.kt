package app.sucareto.onepluskey.xposed

import android.os.Build
import java.lang.reflect.Field
import java.lang.reflect.Modifier

internal const val GOOGLE_PACKAGE = "com.google.android.googlequicksearchbox"

internal object GoogleEligibility {

    private const val SYSTEM_PROPERTIES_CLASS = "android.os.SystemProperties"
    private const val PACKAGE_MANAGER_CLASS = "android.app.ApplicationPackageManager"
    private const val ELIGIBLE_PROP = "ro.opa.eligible_device"
    private val FEATURES = setOf(
        "com.google.android.feature.GOOGLE_BUILD",
        "com.google.android.feature.GOOGLE_EXPERIENCE",
    )

    private val DEVICE = mapOf(
        "MANUFACTURER" to "samsung",
        "BRAND" to "samsung",
        "MODEL" to "SM-S928B",
        "DEVICE" to "e3q",
        "PRODUCT" to "e3q",
    )

    fun install(hooks: Hooks, classLoader: ClassLoader) {
        hookSystemProperties(hooks, classLoader)
        hookFeatures(hooks, classLoader)
        spoofDevice()
    }

    private fun hookSystemProperties(hooks: Hooks, classLoader: ClassLoader) {
        val clazz = Reflect.findClass(SYSTEM_PROPERTIES_CLASS, classLoader) ?: run {
            Log.w("SystemProperties not found")
            return
        }
        val string = String::class.java
        hooks.hook(Reflect.method(clazz, "get", string), "SystemProperties.get(String)") { chain ->
            if (chain.getArg(0) == ELIGIBLE_PROP) "true" else chain.proceed()
        }
        hooks.hook(Reflect.method(clazz, "get", string, string), "SystemProperties.get(String, String)") { chain ->
            if (chain.getArg(0) == ELIGIBLE_PROP) "true" else chain.proceed()
        }
        hooks.hook(
            Reflect.method(clazz, "getBoolean", string, Boolean::class.javaPrimitiveType!!),
            "SystemProperties.getBoolean",
        ) { chain ->
            if (chain.getArg(0) == ELIGIBLE_PROP) true else chain.proceed()
        }
    }

    private fun hookFeatures(hooks: Hooks, classLoader: ClassLoader) {
        val clazz = Reflect.findClass(PACKAGE_MANAGER_CLASS, classLoader) ?: run {
            Log.w("ApplicationPackageManager not found")
            return
        }
        runCatching { clazz.declaredMethods.toList() }.getOrDefault(emptyList())
            .filter { it.name == "hasSystemFeature" && it.parameterTypes.firstOrNull() == String::class.java }
            .forEach { method ->
                method.isAccessible = true
                hooks.hook(method, "hasSystemFeature/${method.parameterCount}") { chain ->
                    if (chain.getArg(0) in FEATURES) true else chain.proceed()
                }
            }
    }

    private fun spoofDevice() {
        DEVICE.forEach { (name, value) ->
            val field = Reflect.field(Build::class.java, name) ?: run {
                Log.w("Build.$name not found")
                return@forEach
            }
            if (!StaticFieldWriter.write(field, value)) Log.w("Failed to set Build.$name")
        }
        Log.i("Build spoofed: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
    }
}

private object StaticFieldWriter {

    private const val UNSAFE_CLASS = "jdk.internal.misc.Unsafe"

    fun write(field: Field, value: Any): Boolean {
        if (!Modifier.isStatic(field.modifiers)) return false
        if (readMatches(field, value)) return true
        runCatching { field.set(null, value) }
        if (readMatches(field, value)) return true
        return runCatching { writeUnsafe(field, value) }
            .onFailure { Log.w("Unsafe write of ${field.name} failed", it) }
            .getOrDefault(false) && readMatches(field, value)
    }

    private fun readMatches(field: Field, value: Any): Boolean =
        runCatching { field.get(null) == value }.getOrDefault(false)

    private fun writeUnsafe(field: Field, value: Any): Boolean {
        val clazz = Class.forName(UNSAFE_CLASS)
        val unsafe = (Reflect.getStatic(clazz, "theUnsafe") ?: Reflect.getStatic(clazz, "THE_ONE")) ?: return false
        val base = clazz.getMethod("staticFieldBase", Field::class.java).invoke(unsafe, field)
        val offset = clazz.getMethod("staticFieldOffset", Field::class.java).invoke(unsafe, field) as Long
        val put = runCatching {
            clazz.getMethod("putReference", Any::class.java, Long::class.javaPrimitiveType, Any::class.java)
        }.getOrElse {
            clazz.getMethod("putObject", Any::class.java, Long::class.javaPrimitiveType, Any::class.java)
        }
        put.invoke(unsafe, base, offset, value)
        return true
    }
}
