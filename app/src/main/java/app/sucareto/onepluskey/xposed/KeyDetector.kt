package app.sucareto.onepluskey.xposed

import android.os.Handler
import android.os.HandlerThread
import app.sucareto.onepluskey.config.Gesture

internal class KeyDetector(private val callbacks: Callbacks) {

    interface Callbacks {
        fun longPressWanted(): Boolean
        fun maxTaps(): Int
        fun longPressMs(): Long
        fun multiTapMs(): Long
        fun onGesture(gesture: Gesture)
        fun onLongPressReleased()
    }

    private val thread = HandlerThread("OnePlusKeyDetector").apply { start() }
    private val handler = Handler(thread.looper)

    private var longPressFired = false
    private var waitingUp = false
    private var tapCount = 0

    private val longPressRunnable = Runnable {
        if (waitingUp && !longPressFired) {
            longPressFired = true
            flushTaps()
            callbacks.onGesture(Gesture.LONG)
        }
    }

    private val tapTimeoutRunnable = Runnable { flushTaps() }

    fun onDown() {
        handler.post {
            handler.removeCallbacks(tapTimeoutRunnable)
            waitingUp = true
            longPressFired = false
            if (callbacks.longPressWanted()) {
                handler.removeCallbacks(longPressRunnable)
                handler.postDelayed(longPressRunnable, callbacks.longPressMs())
            }
        }
    }

    fun onUp() {
        handler.post {
            waitingUp = false
            handler.removeCallbacks(longPressRunnable)
            if (longPressFired) {
                longPressFired = false
                callbacks.onLongPressReleased()
                return@post
            }
            tapCount++
            val max = callbacks.maxTaps()
            if (tapCount >= max) {
                flushTaps()
            } else {
                handler.removeCallbacks(tapTimeoutRunnable)
                handler.postDelayed(tapTimeoutRunnable, callbacks.multiTapMs())
            }
        }
    }

    private fun flushTaps() {
        handler.removeCallbacks(tapTimeoutRunnable)
        val count = tapCount
        tapCount = 0
        val gesture = Gesture.forTaps(count) ?: return
        callbacks.onGesture(gesture)
    }
}
