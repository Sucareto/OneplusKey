package app.sucareto.onepluskey.root

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RootRunner {
    suspend fun run(command: String, asRoot: Boolean) {
        withContext(Dispatchers.IO) {
            if (!asRoot) {
                Shell.Builder.create().build("sh").use { it.newJob().add(command).exec() }
            } else if (runCatching { Shell.getShell().isRoot }.getOrDefault(false)) {
                Shell.cmd(command).exec()
            }
        }
    }
}
