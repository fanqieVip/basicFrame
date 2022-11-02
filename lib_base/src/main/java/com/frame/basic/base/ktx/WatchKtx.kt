package com.frame.basic.base.ktx

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.frame.basic.base.mvvm.vm.CoreVM
import kotlin.collections.set

/**
 * 绑定生命周期的Watch方法
 */
fun <T, DATA> MutableLiveData<T>.watch(
    vm: CoreVM,
    data: MutableLiveData<DATA>?,
    onChange: (data: DATA?, self: MutableLiveData<T>) -> Unit
): MutableLiveData<T> {
    data?.let {
        var watchData = vm.watchLiveDataMap[it]
        if (watchData == null) {
            watchData = ArrayList<Observer<*>>()
        }
        watchData.add(Observer<DATA> { t -> onChange.invoke(t, this) })
        vm.watchLiveDataMap[it] = watchData
    }
    return this
}

/**
 * 不绑定生命周期的Watch方法，有内存泄漏风险，谨慎使用
 */
fun <T, DATA> MutableLiveData<T>.watchForever(
    data: MutableLiveData<DATA>?,
    onChange: (data: DATA?, self: MutableLiveData<T>) -> Unit
): MutableLiveData<T> {
    data?.observeForever {
        onChange(it, this)
    }
    return this
}
/**
 * 绑定生命周期的Watch方法
 */
fun <T, DATA> Lazy<MutableLiveData<T>>.watch(
    vm: CoreVM,
    data: MutableLiveData<DATA>?,
    onChange: (data: DATA?, self: MutableLiveData<T>) -> Unit
): Lazy<MutableLiveData<T>> {
    data?.let {
        var watchData = vm.watchLiveDataMap[it]
        if (watchData == null) {
            watchData = ArrayList<Observer<*>>()
        }
        watchData.add(Observer<DATA> { t -> onChange.invoke(t, this.value) })
        vm.watchLiveDataMap[it] = watchData
    }
    return this
}

/**
 * 不绑定生命周期的Watch方法，有内存泄漏风险，谨慎使用
 */
fun <T, DATA> Lazy<MutableLiveData<T>>.watchForever(
    data: MutableLiveData<DATA>?,
    onChange: (data: DATA?, self: MutableLiveData<T>) -> Unit
): Lazy<MutableLiveData<T>> {
    data?.observeForever {
        onChange(it, this.value)
    }
    return this
}
