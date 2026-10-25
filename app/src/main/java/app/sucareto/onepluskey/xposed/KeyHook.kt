package app.sucareto.onepluskey.xposed

import android.view.KeyEvent
import io.github.libxposed.api.XposedInterface
import app.sucareto.onepluskey.config.Gesture
import app.sucareto.onepluskey.config.KeyAction
import app.sucareto.onepluskey.config.KeyConfig
import app.sucareto.onepluskey.config.ScreenOffMode

internal class KeyHook(
    private val config: KeyConfig,
    private val overlay: ConfigOverlay,
    private val classLoader: ClassLoader,
) : KeyDetector.Callbacks {

    private companion object {
        const val STRATEGY_CLASS = "com.android.server.policy.StrategyActionButtonKeyLaunchApp"
        const val METHOD = "actionInterceptKeyBeforeQueueing"
        const val ACTION_KEY_CODE = 780
        const val RESULT_CONSUMED = 0
        const val INTERCEPTOR_BASE_CLASS = "com.android.server.policy.InputKeyCallback\$KeyInterceptor"
        const val INTERCEPTOR_METHOD = "interceptKeyBeforeQueueing"
        const val MAX_INNER_CLASS_INDEX = 9
        const val BOOT_COMPLETED_METHOD = "initInBootCompleted"
        val SIGNATURE = arrayOf(
            KeyEvent::class.java,
            Int::class.javaPrimitiveType!!,
            Int::class.javaPrimitiveType!!,
            Boolean::class.javaPrimitiveType!!,
            Boolean::class.javaPrimitiveType!!,
        )
    }

    private val detector = KeyDetector(this)

    @Volatile
    private var strategy: Any? = null

    @Volatile
    private var interactive = true

    @Volatile
    private var passThroughDownTime = Long.MIN_VALUE

    fun install(hooks: Hooks): Boolean {
        val clazz = Reflect.findClass(STRATEGY_CLASS, classLoader) ?: run {
            Log.w("StrategyActionButtonKeyLaunchApp not found")
            return false
        }
        val targets = interceptorMethods(clazz) + listOfNotNull(Reflect.method(clazz, METHOD, *SIGNATURE))
        var installed = false
        targets.forEach { method ->
            hooks.deoptimize(method)
            if (hooks.hook(method, "${method.declaringClass.name}.${method.name}") { chain -> intercept(chain) }) {
                installed = true
            }
        }
        Reflect.method(clazz, BOOT_COMPLETED_METHOD)?.let { method ->
            hooks.hook(method, "$STRATEGY_CLASS.$BOOT_COMPLETED_METHOD") { chain ->
                val result = chain.proceed()
                ConfigReceiver.register(overlay)
                result
            }
        }
        return installed
    }

    private fun interceptorMethods(strategy: Class<*>): List<java.lang.reflect.Method> {
        val interceptorBase = Reflect.findClass(INTERCEPTOR_BASE_CLASS, classLoader) ?: return emptyList()
        return (1..MAX_INNER_CLASS_INDEX).mapNotNull { index ->
            val inner = Reflect.findClass("${strategy.name}\$$index", classLoader) ?: return@mapNotNull null
            if (!interceptorBase.isAssignableFrom(inner)) return@mapNotNull null
            runCatching { inner.getDeclaredMethod(INTERCEPTOR_METHOD, *SIGNATURE) }.getOrNull()
                ?.apply { isAccessible = true }
        }
    }

    private fun intercept(chain: XposedInterface.Chain): Any? {
        val event = chain.getArg(0) as? KeyEvent
        if (!config.enabled || event == null || event.keyCode != ACTION_KEY_CODE) {
            return chain.proceed()
        }
        if (event.downTime == passThroughDownTime) return chain.proceed()
        val isInteractive = chain.getArg(4) as? Boolean ?: true
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0 && !isInteractive && !screenOffWanted()) {
            passThroughDownTime = event.downTime
            return chain.proceed()
        }
        if (event.repeatCount != 0) return RESULT_CONSUMED
        ConfigReceiver.register(overlay)
        val owner = chain.thisObject
        strategy = Reflect.get(owner, "this\$0") ?: owner
        interactive = isInteractive
        when (event.action) {
            KeyEvent.ACTION_DOWN -> detector.onDown()
            KeyEvent.ACTION_UP -> detector.onUp()
        }
        return RESULT_CONSUMED
    }

    private fun screenOffWanted(): Boolean = Gesture.entries.any {
        val binding = config.binding(it)
        binding.isActive && binding.screenOff != ScreenOffMode.IGNORE
    }

    override fun longPressWanted(): Boolean = config.binding(Gesture.LONG).isActive

    override fun maxTaps(): Int =
        Gesture.taps.lastOrNull { config.binding(it).isActive }?.tapCount ?: 1

    override fun longPressMs(): Long = config.longPressMs

    override fun multiTapMs(): Long = config.multiTapMs

    override fun onGesture(gesture: Gesture) {
        runCatching { handleGesture(gesture) }.onFailure { Log.w("Gesture ${gesture.id} failed", it) }
    }

    private fun handleGesture(gesture: Gesture) {
        val binding = config.binding(gesture)
        Log.i("Gesture ${gesture.id} -> ${binding.action.type}, interactive=$interactive")
        if (!binding.isActive) return
        if (!interactive) {
            when (binding.screenOff) {
                ScreenOffMode.IGNORE -> return
                ScreenOffMode.SILENT -> Unit
                ScreenOffMode.WAKE -> wakeUp()
            }
        }
        val hold = gesture == Gesture.LONG
        when (val action = binding.action) {
            KeyAction.Native -> if (hold) {
                ColorOsBridge.nativeLongPressDown()
            } else {
                if (binding.vibrate) vibrate()
                ColorOsBridge.nativeTap()
            }
            is KeyAction.ColorOsFunction -> ColorOsBridge.runFunction(action.function, hold)
            else -> {
                if (binding.vibrate) vibrate()
                ActionExecutor.run(action)
            }
        }
    }

    override fun onLongPressReleased() {
        ColorOsBridge.longPressUp()
    }

    private fun wakeUp() {
        val target = strategy ?: return
        runCatching { Reflect.method(target.javaClass, "wakeup")?.invoke(target) }
    }

    private fun vibrate() {
        val target = strategy ?: return
        runCatching { Reflect.method(target.javaClass, "longPressStartVibrate")?.invoke(target) }
    }
}
