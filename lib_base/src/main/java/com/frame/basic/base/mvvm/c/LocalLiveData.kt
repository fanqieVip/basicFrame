package com.frame.basic.base.mvvm.c

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.blankj.utilcode.util.GsonUtils
import com.frame.basic.base.mvvm.vm.CoreVM
import com.frame.basic.base.utils.SpUtils
import kotlin.reflect.KClass

/**
 * 持久化的LiveData，会将数据自动同步到本地
 * @param key 本地存储的key，必须全局唯一，否则会出现值覆盖的问题
 * @param initializer 默认值；恢复数据时优先使用本地缓存、如果本地缓存没有则使用传入的默认值
 */
inline fun <reified T : Any> CoreVM.localLiveData(
    key: String,
    initializer: T? = null
): Lazy<MutableLiveData<T>> = LocalMutableLiveDataLazy(this, key, T::class, initializer)

private val mainHandler by lazy { Handler(mainLooper) }
private val mainLooper by lazy { Looper.getMainLooper() }

class LocalMutableLiveDataLazy<T : Any>(
    private var vm: CoreVM?,
    private val key: String,
    private val dataClass: KClass<T>,
    private val initializer: T? = null
) : Lazy<MutableLiveData<T>> {
    private var cached: MutableLiveData<T>? = null
    override val value: MutableLiveData<T>
        get() {
            val data = cached
            return if (data == null) {
                val cached = object : MutableLiveData<T>() {
                    override fun getValue(): T? {
                        val originalValue = super.getValue()
                        if (originalValue != null) {
                            return originalValue
                        }
                        //读本地数据，如果本地有值则使用本地的，如果没有则使用默认值
                        val localData = SpUtils.getString(key, null)
                        if (localData != null) {
                            //有可能数据格式已经变更，这时候发生异常，则不使用原来的数据了
                            return try {
                                GsonUtils.fromJson(localData, dataClass.java)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        return initializer
                    }
                }.apply {
                    //读本地数据，如果本地有值则使用本地的，如果没有则使用默认值
                    val localData = SpUtils.getString(key, null)
                    if (localData != null) {
                        //有可能数据格式已经变更，这时候发生异常，则不使用原来的数据了
                        val result = try {
                            GsonUtils.fromJson(localData, dataClass.java)
                        } catch (e: Exception) {
                            null
                        }
                        if (result != null) {
                            if (mainLooper.thread.id == Thread.currentThread().id) {
                                this.value = result
                            } else {
                                this.postValue(result)
                            }

                        } else {
                            if (initializer != null) {
                                if (mainLooper.thread.id == Thread.currentThread().id) {
                                    this.value = initializer
                                } else {
                                    this.postValue(initializer)
                                }

                            }
                        }
                    } else {
                        if (initializer != null) {
                            if (mainLooper.thread.id == Thread.currentThread().id) {
                                this.value = initializer
                            } else {
                                this.postValue(initializer)
                            }
                        }
                    }
                    mainHandler.postAtFrontOfQueue {
                        vm?.safeObserveForever(this, object :Observer<Any?>{ //绝不能用lamble表达式，编译时会生成单例，有可能造成IllegalArgumentException("Cannot add the same observer with different lifecycles")异常
                            override fun onChanged(t: Any?) {
                                //同步到本地
                                if (t == null) {
                                    SpUtils.putString(key, null)
                                } else {
                                    SpUtils.putString(key, GsonUtils.toJson(t))
                                }
                            }
                        })
                        vm = null
                    }
                }
                this.cached = cached
                cached
            } else {
                data
            }
        }

    override fun isInitialized(): Boolean = cached != null
}
