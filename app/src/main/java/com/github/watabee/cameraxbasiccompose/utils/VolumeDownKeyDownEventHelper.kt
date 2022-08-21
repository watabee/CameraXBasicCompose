package com.github.watabee.cameraxbasiccompose.utils

import java.util.concurrent.CopyOnWriteArraySet

fun interface KeyDownEventListener {
    fun onKeyDown()
}

object VolumeDownKeyDownEventHelper {
    private val listeners = CopyOnWriteArraySet<KeyDownEventListener>()

    fun addListener(listener: KeyDownEventListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: KeyDownEventListener) {
        listeners.remove(listener)
    }

    fun dispatchOnKeyDown() {
        listeners.forEach { it.onKeyDown() }
    }
}
