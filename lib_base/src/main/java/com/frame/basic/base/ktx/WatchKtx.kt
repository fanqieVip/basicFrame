package com.frame.basic.base.ktx

import android.app.Activity
import androidx.fragment.app.Fragment
import androidx.lifecycle.*

/**
 * LiveData观察者，可自定义观察对象，及时更新自己
 */
class WatchLiveData<T>(initData: T? = null) : MutableLiveData<T>() {
    init {
        if (initData != null){
            postValue(initData)
        }
    }
    private val watchData by lazy { HashMap<MutableLiveData<*>, Observer<*>>() }

    /**
     * 该方法会绑定生命周期自动绑定和解绑
     */
    fun <D> watch(
        data: MutableLiveData<D>?,
        onChange: (data: D?, self: WatchLiveData<T>) -> Unit
    ): WatchLiveData<T> {
        data?.let {
            watchData.put(it, Observer<D> { t ->
                onChange.invoke(t, this@WatchLiveData) }
            )
        }
        return this
    }

    /**
     * 该方法不绑定生命周期，容易造成内存泄漏，谨慎使用
     */
    fun <D> watchForever(
        data: MutableLiveData<D>?,
        onChange: (data: D?, self: WatchLiveData<T>) -> Unit
    ): WatchLiveData<T> {
        data?.observeForever {
            onChange(it, this)
        }
        return this
    }

    internal fun attachLifecycleOwner(owner: LifecycleOwner) {
        watchData.forEach { (t, u) ->
            t.observe(owner, u as Observer<in Any>)
        }
        owner.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> {
                        if ((source is Activity && source.isFinishing) || (source is Fragment && source.activity != null && source.requireActivity().isFinishing)) {
                            clearWatchData()
                        }
                    }
                    Lifecycle.Event.ON_DESTROY -> {
                        clearWatchData()
                    }
                    else -> {}
                }
            }

        })
    }

    private fun clearWatchData() {
        if (watchData.isEmpty()) {
            return
        }
        watchData.forEach { (t, u) ->
            t.removeObserver(u as Observer<in Any>)
        }
        watchData.clear()
    }
}
