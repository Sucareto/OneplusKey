package app.sucareto.onepluskey

import android.app.Application
import app.sucareto.onepluskey.config.ModuleState
import com.topjohnwu.superuser.Shell

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_MOUNT_MASTER)
                .setTimeout(20),
        )
        ModuleState.init()
    }
}
