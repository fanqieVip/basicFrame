package com.frame.basic.base.ktx

import androidx.annotation.NonNull
import androidx.lifecycle.*
import com.frame.basic.base.mvvm.c.DisplayStatus

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
            if (event == Lifecycle.Event.ON_DESTROY) {
                removeObserver(observer)
            }
        }
    })
}

/**
 * 绑定真正的页面生命周期，在页面真正显示时才触发
 * @param displayStatus 必须是页面主VM中的displayStatus
 */
fun <T> LiveData<T>.observe(@NonNull displayStatus: LiveData<DisplayStatus>, @NonNull owner: LifecycleOwner, @NonNull observer: Observer<in T>){
    val realLiveData = MutableLiveData<T>()
    realLiveData.observe(owner, observer)
    var stagingData: T? = null
    observe(owner, Observer {
        stagingData = null
        if (displayStatus.value == DisplayStatus.SHOWING){
            realLiveData.value = it
        }else{
            stagingData = it
        }
    })
    displayStatus.observe(owner, Observer {
        if (it == DisplayStatus.SHOWING){
            stagingData?.let { data ->
                realLiveData.value = data
            }
            stagingData = null
        }
    })
}
