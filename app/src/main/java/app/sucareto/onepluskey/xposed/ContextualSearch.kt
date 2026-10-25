package app.sucareto.onepluskey.xposed

import android.os.Binder
import android.os.IBinder
import android.os.Process
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Method

internal object ContextualSearch {

    private const val SYSTEM_SERVER_CLASS = "com.android.server.SystemServer"
    private const val SERVICE_CLASS = "com.android.server.contextualsearch.ContextualSearchManagerService"
    private const val SERVICE_MANAGER_CLASS = "android.os.ServiceManager"
    private const val INTERFACE_CLASS = "android.app.contextualsearch.IContextualSearchManager"
    private const val INTERNAL_STRING_CLASS = "com.android.internal.R\$string"
    private const val PACKAGE_RES_NAME = "config_defaultContextualSearchPackageName"
    private const val SERVICE_NAME = "contextual_search"
    private const val START_METHOD = "startContextualSearch"

    private const val ENTRYPOINT_LONG_PRESS_HOME = 2

    private val selfTrigger = ThreadLocal<Boolean>()

    fun install(module: XposedModule, hooks: Hooks, classLoader: ClassLoader) {
        val resId = Reflect.findClass(INTERNAL_STRING_CLASS, classLoader)
            ?.let { runCatching { Reflect.getStatic(it, PACKAGE_RES_NAME) as? Int }.getOrNull() }
        if (resId == null) Log.w("$PACKAGE_RES_NAME not found")
        val service = Reflect.findClass(SERVICE_CLASS, classLoader) ?: run {
            Log.w("ContextualSearchManagerService not found")
            return
        }
        Reflect.findClass(SYSTEM_SERVER_CLASS, classLoader)?.let { systemServer ->
            hookServiceStart(module, hooks, systemServer, service, resId)
        } ?: Log.w("SystemServer not found")
        hookService(hooks, service)
    }

    private fun hookServiceStart(
        module: XposedModule,
        hooks: Hooks,
        systemServer: Class<*>,
        service: Class<*>,
        resId: Int?,
    ) {
        if (resId != null) {
            val check = Reflect.methodMatching(systemServer, "deviceHasConfigString") {
                it.parameterTypes.size == 2 && it.parameterTypes[1] == Int::class.javaPrimitiveType
            }
            hooks.hook(check, "SystemServer.deviceHasConfigString") { chain ->
                if (chain.getArg(1) == resId) true else chain.proceed()
            }
        }
        val startOther = Reflect.methodMatching(systemServer, "startOtherServices") { it.parameterTypes.size == 1 }
        hooks.deoptimize(startOther)
        hooks.hook(startOther, "SystemServer.startOtherServices") { chain ->
            val result = chain.proceed()
            runCatching { ensureServiceStarted(module, chain.thisObject, service) }
                .onFailure { Log.w("Fallback start of $SERVICE_NAME failed", it) }
            result
        }
    }

    private fun ensureServiceStarted(module: XposedModule, systemServer: Any?, service: Class<*>) {
        if (serviceBinder() != null) {
            Log.i("$SERVICE_NAME service present")
            return
        }
        val manager = systemServer?.let { Reflect.get(it, "mSystemServiceManager") } ?: run {
            Log.w("mSystemServiceManager unavailable")
            return
        }
        val startService = Reflect.method(manager.javaClass, "startService", Class::class.java) ?: run {
            Log.w("SystemServiceManager.startService(Class) not found")
            return
        }
        module.getInvoker(startService)
            .setType(XposedInterface.Invoker.Type.ORIGIN)
            .invoke(manager, service)
        Log.i("$SERVICE_NAME started by fallback, present=${serviceBinder() != null}")
    }

    private fun hookService(hooks: Hooks, service: Class<*>) {
        (listOf(service) + runCatching { service.declaredClasses.toList() }.getOrDefault(emptyList()))
            .flatMap { runCatching { it.declaredMethods.toList() }.getOrDefault(emptyList()) }
            .forEach { hooks.deoptimize(it) }

        val packageName = Reflect.methodMatching(service, "getContextualSearchPackageName") {
            it.parameterTypes.isEmpty()
        }
        hooks.hook(packageName, "$SERVICE_NAME getContextualSearchPackageName") { chain ->
            val original = chain.proceed() as? String
            if (original.isNullOrEmpty()) GOOGLE_PACKAGE else original
        }

        val enforce = Reflect.methodMatching(service, "enforcePermission") {
            it.parameterTypes.size == 1 && it.parameterTypes[0] == String::class.java
        }
        hooks.hook(enforce, "$SERVICE_NAME enforcePermission") { chain ->
            if (selfTrigger.get() == true && chain.getArg(0) == START_METHOD &&
                Binder.getCallingPid() == Process.myPid()
            ) {
                null
            } else {
                chain.proceed()
            }
        }
    }

    private fun serviceBinder(): IBinder? = runCatching {
        Class.forName(SERVICE_MANAGER_CLASS)
            .getMethod("getService", String::class.java)
            .invoke(null, SERVICE_NAME) as? IBinder
    }.getOrNull()

    fun trigger(): Boolean = runCatching {
        val binder = serviceBinder() ?: run {
            Log.w("$SERVICE_NAME service not running")
            return false
        }
        val iface = Class.forName(INTERFACE_CLASS)
        val stub = Class.forName("$INTERFACE_CLASS\$Stub")
        val manager = stub.getMethod("asInterface", IBinder::class.java).invoke(null, binder) ?: return false
        val method = startMethod(iface) ?: run {
            Log.w("$START_METHOD not found")
            return false
        }
        val args = arrayOfNulls<Any>(method.parameterCount).also { it[0] = ENTRYPOINT_LONG_PRESS_HOME }
        selfTrigger.set(true)
        try {
            method.invoke(manager, *args)
        } finally {
            selfTrigger.remove()
        }
        true
    }.getOrElse {
        Log.w("Circle to Search trigger failed", it)
        false
    }

    private fun startMethod(iface: Class<*>): Method? = iface.methods
        .filter { it.name == START_METHOD && it.parameterCount >= 1 && it.parameterTypes[0] == Int::class.javaPrimitiveType }
        .minByOrNull { it.parameterCount }
}
