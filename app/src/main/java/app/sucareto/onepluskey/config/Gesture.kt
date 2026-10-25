package app.sucareto.onepluskey.config

enum class Gesture(val id: String, val tapCount: Int) {
    SINGLE("single", 1),
    DOUBLE("double", 2),
    LONG("long", 0);

    val supportsNative: Boolean get() = this == SINGLE || this == LONG

    companion object {
        val taps = listOf(SINGLE, DOUBLE)

        fun forTaps(count: Int): Gesture? = taps.firstOrNull { it.tapCount == count }
    }
}
