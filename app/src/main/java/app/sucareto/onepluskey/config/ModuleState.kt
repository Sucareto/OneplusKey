package app.sucareto.onepluskey.config

import android.content.SharedPreferences
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object ModuleState : XposedServiceHelper.OnServiceListener {
    private val serviceFlow = MutableStateFlow<XposedService?>(null)
    val service: StateFlow<XposedService?> = serviceFlow

    val active: Boolean get() = serviceFlow.value != null

    val frameworkName: String?
        get() = runCatching { serviceFlow.value?.frameworkName }.getOrNull()

    fun init() {
        XposedServiceHelper.registerListener(this)
    }

    override fun onServiceBind(service: XposedService) {
        serviceFlow.value = service
    }

    override fun onServiceDied(service: XposedService) {
        if (serviceFlow.value === service) serviceFlow.value = null
    }

    fun preferences(): SharedPreferences? =
        runCatching { serviceFlow.value?.getRemotePreferences(ConfigKeys.GROUP) }.getOrNull()
}
