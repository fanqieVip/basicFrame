package com.frame.basic.base.ktx

import android.app.Activity
import androidx.annotation.NonNull
import androidx.fragment.app.Fragment
import androidx.lifecycle.*

/**
 * 不感知声明周期，实时响应，但会伴随页面销毁而销毁
 */
fun <T> LiveData<T>.observeForever(
    @NonNull owner: LifecycleOwner,
    @NonNull observer: Observer<in T>
) {
    observeForever(observer)
    owner.lifecycle.addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (event == Lifecycle.Event.ON_DESTROY || (event == Lifecycle.Event.ON_PAUSE  && ((source is Activity && source.isFinishing) || (source is Fragment && source.activity != null && source.requireActivity().isFinishing)))) {
                removeObserver(observer)
            }
        }
    })
}
