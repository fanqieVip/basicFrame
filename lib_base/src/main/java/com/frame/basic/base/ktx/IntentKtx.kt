package com.frame.basic.base.ktx

import android.app.Activity
import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.frame.basic.base.utils.getUnicode

internal val functionExtras by lazy { HashMap<String, Function<*>>() }
internal val functionExtraTag by lazy { "functionExtraTag" }

internal fun getFunctionExtraKey(owner: LifecycleOwner, key: String, value: Function<*>) = "${functionExtraTag}_${getUnicode(owner)}_${value.toString()}_${value.javaClass.name}_${key}"

/**
 * 仿Eventbus回调方式，无序列化问题，可直接访问当前对象
 * 注意：1.界面发生旋转重建后，界面对象已变化，会造成回调无效的问题，所以尽量只访问viewModel
 *       2.app异常重启恢复时，由于回调存在内存，会造成丢失
 */
fun Intent.putExtra(owner: LifecycleOwner, key: String, value: Function<*>) {
    val realKey = getFunctionExtraKey(owner, key, value)
    putExtra(key, realKey)
    functionExtras[realKey] = value
    owner.lifecycle.addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if ((source is Activity && source.isFinishing) || (source is Fragment && source.activity != null && source.requireActivity().isFinishing)) {
                        functionExtras.remove(realKey)
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    functionExtras.remove(realKey)
                }
                else -> {}
            }
        }
    })
}


