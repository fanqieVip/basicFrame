package com.frame.basic.base.ktx

import androidx.lifecycle.LifecycleOwner

/**
 * Function包装类，用于绑定生命周期，使用完后自动清除应用避免内存泄漏
 */
class LifecycleFunction<T : Function<*>>(owner: LifecycleOwner, var delegate: T?) {
    init {
        owner.lifecycle.addObserver(object : androidx.lifecycle.LifecycleEventObserver {
            override fun onStateChanged(
                source: LifecycleOwner,
                event: androidx.lifecycle.Lifecycle.Event
            ) {
                if (event == androidx.lifecycle.Lifecycle.Event.ON_DESTROY) {
                    delegate = null
                }
            }
        })
    }
}
