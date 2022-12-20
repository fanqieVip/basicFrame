package com.frame.basic.base.mvvm.c

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.savedstate.SavedStateRegistryOwner
import com.frame.basic.base.BaseApplication
import com.frame.basic.base.mvvm.vm.CoreVM
import com.frame.basic.base.mvvm.vm.SavedStateHandleMutableLiveDataLazy
import kotlin.reflect.KClass

/**
 * 共享ViewModel原理
 * 1.不使用系统自带的ViewModelStore,用自定义的
 * 2.全局存放VMStores,使用ViewModel类名作为唯一key
 * 3.打开页面加载ViewModel如果检测到这个类已经有实例则使用该实例，并绑定到LifecycleOwner生命周期
 * 4.当LifecycleOwner销毁时先取消二者绑定关系，再检测该ViewModel还有没有被其他LifecycleOwner绑定，
 * 如果没有就销毁ViewModel，如果还有就不销毁
 *
 * 共享ViewModel的好处
 * 1.安全。ViewModel是共享的而不是跟随应用生命周期的，当没有LifecycleOwner持有后会自动销毁
 * 2.跨页面共享。activity、fragment、dialogFragment都可以使用
 *
 * 其他：
 * 1.viewModels和keepViewModels的ViewModel都是一起共享的，但一旦使用了keepViewModels，那么该viewModel将跟随应用生命周期不再销毁，所以建议仅在Application里面使用keepViewModels初始化必要数据
 */

@MainThread
inline fun <reified VM : CoreVM> ComponentActivity.vms(): Lazy<VM> {
    return ShareViewModelLazy({ defaultViewModelProviderFactory }, {this.defaultViewModelCreationExtras}, VM::class, this)
}

@MainThread
inline fun <reified VM : CoreVM> Fragment.vms(): Lazy<VM> {
    return ShareViewModelLazy({ defaultViewModelProviderFactory }, {this.defaultViewModelCreationExtras}, VM::class, this)
}

/**
 * 跟随应用生命周期的ViewModel(切忌滥用)
 * ！！！禁止在activity、fragment、dialog、popuWindow、View中调用，会造成内存泄漏，这几个地方一律使用viewModels来加载！！！
 * @param autoInit 是否自动初始化，默认true  //by keepViewModels如不自动初始化且不调用时，将不能跟随生命周期
 */
@MainThread
inline fun <reified VM : CoreVM> Any.keepVms(autoInit: Boolean = true): Lazy<VM> {
    if (this is SavedStateRegistryOwner) {
        throw RuntimeException("Forbid Use KeepViewModels Loading Any ViewModel")
    }
    keepApplications.add(VM::class.java.name)
    val factoryPromise =
        ViewModelProvider.AndroidViewModelFactory.getInstance(BaseApplication.application)
    return ViewModelLazy(VM::class, { globalVmStore }, { factoryPromise }).also {
        //by keepViewModels将自动初始化以防止未调用时，没有实现跟随生命周期
        if (autoInit) {
            it.value
        }
    }
}

//全局共享的ViewModel提供者
val globalVmStore: ViewModelStore by lazy {
    ViewModelStore()
}

//需要跟随应用生命周期的ViewModel
val keepApplications by lazy { HashSet<String>() }

/**
 * 获取ViewModel实例
 */
inline fun <reified VM : ViewModel> getViewModel(): VM? {
    val element = VM::class.java.name
    val field = ViewModelStore::class.java.declaredFields.find { it.name == "mMap" }?.apply {
        isAccessible = true
    }
    field?.get(globalVmStore)?.let {
        it as HashMap<String, ViewModel>
        return it["${ViewModelProvider::class.java.name}.DefaultKey:${element}"] as? VM
    }
    return null
}

class ShareViewModelLazy<VM : ViewModel>(
    private val factoryProducer: () -> ViewModelProvider.Factory,
    private val defaultCreationExtras: () -> CreationExtras ,
    private val viewModelClass: KClass<VM>,
    private val lifecycleOwner: LifecycleOwner
) : Lazy<VM> {
    private var cached: VM? = null
    override val value: VM
        get() {
            val viewModel = cached
            return if (viewModel == null) {
                val store = shareVmStore
                store.bindHost(viewModelClass.java.name, lifecycleOwner)
                ViewModelProvider(store.viewModelStore, factoryProducer(), defaultCreationExtras())[viewModelClass.java].also {
                    cached = it
                }
            } else {
                viewModel
            }
        }


    override fun isInitialized(): Boolean = cached != null
}

private val shareVmStore by lazy { VMStore() }

/**
 * 自定义ViewModel商店
 */
class VMStore : ViewModelStoreOwner {
    private val bindTargets by lazy { HashMap<String, ArrayList<LifecycleOwner>>() }

    //绑定
    fun bindHost(vm: String, host: LifecycleOwner) {
        var vmLifecycleOwners = bindTargets[vm]
        if (vmLifecycleOwners == null) {
            vmLifecycleOwners = ArrayList()
            bindTargets[vm] = vmLifecycleOwners
        }
        if (!vmLifecycleOwners.contains(host)) {
            vmLifecycleOwners.add(host)
            //绑定生命周期
            host.lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_DESTROY || (event == Lifecycle.Event.ON_PAUSE && ((source is Activity && source.isFinishing) || (source is Fragment && source.activity != null && source.requireActivity().isFinishing)))) {
                        //移除LifecycleOwner
                        val emptyBindTargets = ArrayList<String>()
                        bindTargets.forEach { entry ->
                            if (entry.value.contains(source)) {
                                entry.value.remove(source)
                                if (entry.value.isEmpty()) {
                                    emptyBindTargets.add(entry.key)
                                }
                                //检查是否应该清除掉ViewModel全局缓存
                                if (entry.value.isEmpty()) {
                                    //如果不是在重建中，才移除缓存
                                    if (source is RecreateControl && !source.isRecreating()) {
                                        //如果当前商店没有关联对象，则释放内存
                                        val field =
                                            ViewModelStore::class.java.declaredFields.find { it.name == "mMap" }
                                                ?.apply {
                                                    isAccessible = true
                                                }
                                        val shareViewModelMap =
                                            field?.get(globalVmStore) as? HashMap<String, ViewModel>
                                        val key = "${ViewModelProvider::class.java.name}.DefaultKey:${entry.key}"
                                        shareViewModelMap?.get(key)?.let {
                                            clearLiveDataObserve(source, it)
                                            if (!keepApplications.contains(it.javaClass.name)) {
                                                shareViewModelMap.remove(key)
                                                if (it is CoreVM) {
                                                    it.clearALL()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        //清除没有绑定LifecycleOwner的数据
                        if (emptyBindTargets.isNotEmpty()) {
                            emptyBindTargets.forEach {
                                bindTargets.remove(it)
                            }
                        }
                    }
                }
            })
        }
    }

    override fun getViewModelStore() = globalVmStore

    private fun clearLiveDataObserve(owner: LifecycleOwner, vm: ViewModel){
        vm::class.java.declaredFields.forEach { field ->
            field.isAccessible = true
            field.get(vm).let {
                when(it){
                    is SavedStateHandleMutableLiveDataLazy<*> -> {
                        it.value?.removeObservers(owner)
                    }
                    is LiveData<*> -> {
                        it.removeObservers(owner)
                    }
                    is LocalMutableLiveDataLazy<*> -> {
                        it.value?.removeObservers(owner)
                    }
                }
            }
        }
    }
}
