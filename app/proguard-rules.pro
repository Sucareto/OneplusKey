-dontwarn io.github.libxposed.annotation.**
-dontwarn io.github.libxposed.service.**
-adaptresourcefilecontents META-INF/xposed/java_init.list
-keep,allowoptimization,allowobfuscation class app.sucareto.onepluskey.xposed.ModuleMain {
    public <init>();
}
