package app.sucareto.onepluskey.root

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Process
import app.sucareto.onepluskey.config.Protocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ShellReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Protocol.ACTION_SHELL) return
        if (sentFromUid != Process.SYSTEM_UID) return
        val command = intent.getStringExtra(Protocol.EXTRA_COMMAND)?.takeIf { it.isNotBlank() } ?: return
        val asRoot = intent.getBooleanExtra(Protocol.EXTRA_ROOT, true)
        val pending = goAsync()
        scope.launch {
            try {
                RootRunner.run(command, asRoot)
            } finally {
                pending.finish()
            }
        }
    }
}
