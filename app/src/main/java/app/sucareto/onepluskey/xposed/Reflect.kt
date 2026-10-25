package app.sucareto.onepluskey.xposed

import java.lang.reflect.Field
import java.lang.reflect.Method

internal object Reflect {
    fun findClass(name: String, loader: ClassLoader?): Class<*>? = try {
        Class.forName(name, false, loader)
    } catch (_: ClassNotFoundException) {
        null
    } catch (_: LinkageError) {
        null
    }

    fun method(clazz: Class<*>, name: String, vararg params: Class<*>): Method? {
        var current: Class<*>? = clazz
        while (current != null) {
            try {
                return current.getDeclaredMethod(name, *params).apply { isAccessible = true }
            } catch (_: NoSuchMethodException) {
            } catch (_: LinkageError) {
            }
            current = current.superclass
        }
        return null
    }

    fun methodMatching(clazz: Class<*>, name: String, predicate: (Method) -> Boolean): Method? = try {
        clazz.declaredMethods.firstOrNull { it.name == name && predicate(it) }?.apply { isAccessible = true }
    } catch (_: LinkageError) {
        null
    }

    fun field(clazz: Class<*>, name: String): Field? {
        var current: Class<*>? = clazz
        while (current != null) {
            try {
                return current.getDeclaredField(name).apply { isAccessible = true }
            } catch (_: NoSuchFieldException) {
            } catch (_: LinkageError) {
            }
            current = current.superclass
        }
        return null
    }

    fun get(target: Any, name: String): Any? = field(target.javaClass, name)?.get(target)

    fun getStatic(clazz: Class<*>, name: String): Any? = field(clazz, name)?.get(null)
}
