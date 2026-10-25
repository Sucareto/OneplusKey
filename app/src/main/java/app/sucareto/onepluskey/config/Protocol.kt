package app.sucareto.onepluskey.config

object Protocol {
    const val PACKAGE = "app.sucareto.onepluskey"
    const val SYSTEM_PACKAGE = "android"

    const val ACTION_SHELL = "$PACKAGE.action.SHELL"
    const val ACTION_CONFIG = "$PACKAGE.action.CONFIG"
    const val EXTRA_CONFIG = "config"
    const val EXTRA_COMMAND = "command"
    const val EXTRA_ROOT = "root"
}
