package com.frame.basic.base.mvvm.vm

import android.accounts.NetworkErrorException
import android.app.Activity
import android.net.ParseException
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import com.frame.basic.base.ktx.functionExtraTag
import com.frame.basic.base.ktx.functionExtras
import com.frame.basic.base.mvvm.c.*
import com.frame.basic.base.mvvm.v.LocationInfo
import com.google.gson.JsonSyntaxException
import com.google.gson.stream.MalformedJsonException
import kotlinx.coroutines.*
import org.json.JSONException
import retrofit2.HttpException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * ViewModel 基类
 * @author: fanj BaseVM
 * @CreateDate:     2021/11/5 10:14
 */
abstract class BaseVM(private val handle: SavedStateHandle) : CoreVM(), VMControl {
    internal val uiStatus by savedStateLiveData<UIStatusInfo>("VMControl_uiStatus")
    internal val refreshLayoutState by savedStateLiveData<RefreshLayoutStatus>("VMControl_refreshLayoutState")
    internal val popLoadingStatus by savedStateLiveData<PopLoadingStatus>("VMControl_popLoadingStatus")
    internal val currentPageNo by savedStateLiveData<Int>("VMControl_currentPageNo")
    internal val popWindowLocationInfo by savedStateLiveData<LocationInfo>("VMControl_popWindowLocationInfo")
    val displayStatus = MutableLiveData<DisplayStatus>()
    val uiStatusMode = MutableLiveData<UIStatus>()
    final override fun loadSuccess(empty: Boolean) {
        if (!empty) {
            if (uiStatus.value?.uiStatus != UIStatus.SUCCESS){
                uiStatus.postValue(UIStatusInfo(UIStatus.SUCCESS))
            }
            if (this is PagingControl) {
                when (refreshLayoutState.value) {
                    RefreshLayoutStatus.LOAD_MORE -> {
                        // "?:"的优先级比"+"的优先级低
                        currentPageNo.postValue((currentPageNo.value ?: getFirstPageNo()) + 1)
                    }
                    RefreshLayoutStatus.REFRESH, null -> {
                        currentPageNo.postValue(getFirstPageNo())
                    }
                    else -> {
                    }
                }
            }
            refreshLayoutState.postValue(RefreshLayoutStatus.STOP)
        } else {
            if (this is PagingControl && refreshLayoutState.value == RefreshLayoutStatus.LOAD_MORE) {
                refreshLayoutState.postValue(RefreshLayoutStatus.NO_MORE)
            } else {
                uiStatus.postValue(UIStatusInfo(UIStatus.EMPTY))
                refreshLayoutState.postValue(RefreshLayoutStatus.STOP)
            }
        }
    }

    override fun loadError(error: Int, msg: String?) {
        if (!(this is PagingControl && refreshLayoutState.value == RefreshLayoutStatus.LOAD_MORE)) {
            uiStatus.postValue(UIStatusInfo(UIStatus.ERROR, error, msg))
        }
        refreshLayoutState.postValue(RefreshLayoutStatus.STOP)
    }

    final override fun loading() {
        if (uiStatus.value?.uiStatus != UIStatus.LOADING){
            uiStatus.postValue(UIStatusInfo(UIStatus.LOADING))
        }
    }

    final override fun showPopLoading(text: String) {
        popLoadingStatus.postValue(PopLoadingStatus(true, text))
    }

    final override fun dismissPopLoading() {
        popLoadingStatus.postValue(PopLoadingStatus(false))
    }

    /**
     * 跟随页面自动恢复数据的LiveData
     * 1.app异常重启后会自动恢复重启前的数据，如果页面正常退出或app正常关闭则不会
     * 2.数据取自Bundle的自动序列化数据，所以extra的数据也可以直接用这个传输
     * @param key
     */
    protected fun <T : Any> savedStateLiveData(
        key: String,
        initializer: T? = null
    ): Lazy<MutableLiveData<T>> = SavedStateHandleMutableLiveDataLazy(key, handle, initializer).also {
        //如果是通过putExtra(owner, key, initializer)传过来的值，则默认初始化一次，初始化时会清空全局变量中存放的callback，以避免未进行初始化调用时内存泄漏的问题
        val functionExtraKey = handle.get<Any>(key)
        if (functionExtraKey != null && functionExtraKey is String && functionExtraKey.startsWith(functionExtraTag)) {
            it.value
        }
    }
}

internal class SavedStateHandleMutableLiveDataLazy<T : Any>(
    private val key: String,
    private val handle: SavedStateHandle,
    private val initializer: T? = null
) : Lazy<MutableLiveData<T>> {
    private var cached: MutableLiveData<T>? = null
    override val value: MutableLiveData<T>
        get() {
            val data = cached
            return if (data == null) {
                cached = createCache()
                cached!!
            } else {
                data
            }
        }

    override fun isInitialized(): Boolean = cached != null

    private fun createCache(): MutableLiveData<T>{
        val functionExtraKey = handle.get<Any>(key)
        if (functionExtraKey != null && functionExtraKey is String && functionExtraKey.startsWith(functionExtraTag)){
            //全局存储的Function的key
            val function = functionExtras.remove(functionExtraKey)
            return if(function != null){
                MutableLiveData(function as T)
            }else{
                if (initializer != null){
                    MutableLiveData(initializer)
                }else{
                    MutableLiveData<T>()
                }
            }
        }else{
            //注意这里必须这样写，如果只用getLiveData(key, initializer)，会造成observe有null的回调
            return if (initializer != null) {
                handle.getLiveData(key, initializer)
            } else {
                handle.getLiveData(key)
            }
        }
    }
}

/**
 * 核心ViewModel
 */
open class CoreVM : ViewModel(), VMLifecycle {
    internal val watchLiveDataMap by lazy { HashMap<MutableLiveData<*>,ArrayList<Observer<*>>>() }
    private val jobs by lazy { ArrayList<JobControl>() }
    private val observeForeverLiveDatas by lazy { HashMap<LiveData<*>, ArrayList<Observer<in Any?>>?>() }
    override fun onCleared() {
        super.onCleared()
        watchLiveDataMap.clear()
        //移除掉observeForever的LiveData避免内存泄漏
        removeSafeObserveForever()
        //结束协程
        cancelJobs()
    }

    private fun cancelJobs() {
        if (jobs.isNotEmpty()) {
            jobs.forEach {
                if (it.bindLifecycle && (it.job.isActive || !it.job.isCompleted)) {
                    it.job.cancel(CancellationException("You Has Canceled Job"))
                }
            }
            jobs.clear()
        }
    }

    @MainThread
    internal fun clearALL() {
        onCleared()
    }

    //安全的LiveData观察者，在LiveData会在ViewModel销毁时自动取消observeForever的观察者
    fun safeObserveForever(liveData: LiveData<*>, observer: Observer<in Any?>) {
        var listObserves = observeForeverLiveDatas[liveData]
        if (listObserves == null) {
            listObserves = ArrayList()
            observeForeverLiveDatas[liveData] = listObserves
        }
        listObserves.add(observer)
        liveData.observeForever(observer)
    }

    private fun removeSafeObserveForever() {
        observeForeverLiveDatas.forEach { (t, u) ->
            u?.let {
                it.forEach { ob ->
                    t.removeObserver(ob)
                }
            }
        }
    }

    /**
     * 协程处理器
     * @param onExecute 执行单元
     * @param onError 发生错误
     * @param onComplete 完成
     * @param bindLifecycle 是否绑定生命周期，如绑定则页面关闭后自动取消协程
     */
    fun launch(
        onExecute: suspend CoroutineScope.() -> Unit,
        onError: ((code: Int, error: String?, e: Throwable) -> Unit)? = null,
        onComplete: (() -> Unit)? = null,
        bindLifecycle: Boolean = true
    ): Job {
        return launch(
            call = object : VMCall {
                override val execute: suspend CoroutineScope.() -> Unit = onExecute
                override fun onError(code: Int, error: String?, e: Throwable) {
                    onError?.invoke(code, error, e)
                }

                override fun onComplete() {
                    super.onComplete()
                    onComplete?.invoke()
                }
            },
            bindLifecycle = bindLifecycle
        )
    }

    /**
     * 协程处理器
     * @param call 请求单元
     * @param bindLifecycle 是否绑定生命周期，如绑定则页面关闭后自动取消协程
     */
    fun launch(call: VMCall, bindLifecycle: Boolean = true): Job {
        val job = viewModelScope.launch(
            CoroutineExceptionHandler { _, throwable ->
                //如果是取消任务造成的，则不回调
                if (throwable is CancellationException) {
                    return@CoroutineExceptionHandler
                }
                run {
                    // 这里统一处理错误
                    exceptionHandler(throwable).apply {
                        call.onError(this.first, this.second, throwable)
                    }
                }
            },
        ) {
            try {
                withContext(Dispatchers.IO) {
                    call.execute.invoke(this)
                }
            } finally {
                call.onComplete()
            }
        }
        jobs.add(JobControl(job, bindLifecycle))
        return job
    }
    final override fun initVmLifecycleOwner(owner: LifecycleOwner) {
        if (watchLiveDataMap.isNotEmpty()){
            watchLiveDataMap.forEach { (observeLV, targetWatchData) ->
                targetWatchData.forEach {
                    val observer = it as Observer<in Any>
                    try {
                        observeLV.observe(owner, observer)
                    }catch (e: IllegalArgumentException){
                        //避免跨页面共享VM时，出现一个Observer实例绑定多个owner的异常，只需要保证这种场景有一个绑定有效即可
                        // 之所以不采用绑定一次之后清空watchLiveDataMap避免该异常，主要是为了解决单一页面重建后无法再绑定的问题
                        if (e.message?.equals("Cannot add the same observer with different lifecycles") == false){
                            throw e
                        }
                    }
                }
                owner.lifecycle.addObserver(object : LifecycleEventObserver{
                    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                        when (event) {
                            Lifecycle.Event.ON_PAUSE -> {
                                if ((source is Activity && source.isFinishing) || (source is Fragment && source.activity != null && source.requireActivity().isFinishing)) {
                                    observeLV.removeObservers(owner)
                                }
                            }
                            Lifecycle.Event.ON_DESTROY -> {
                                observeLV.removeObservers(owner)
                            }
                            else -> {}
                        }
                    }

                })
            }
        }
    }
}

/**
 * 记录携程任务单元是否绑定生命周期
 * @param job 任务单元
 * @param bindLifecycle 是否绑定生命周期
 */
private class JobControl(val job: Job, val bindLifecycle: Boolean)

/**
 * 业务异常
 */
class ApiException(val code: Int, error: String?) : Exception(error)

/**
 * VM协程处理工具
 */
interface VMCall {
    //协程执行器
    val execute: suspend CoroutineScope.() -> Unit

    //错误回调
    fun onError(code: Int, error: String?, e: Throwable)
    fun onComplete() {}
}

/**
 * 未知错误码
 */
internal const val OTHER_ERROR_CODE = 999999

internal fun exceptionHandler(e: Throwable): Pair<Int, String?> {
    e.printStackTrace()
    return when (e) {
        is UnknownHostException -> Pair(OTHER_ERROR_CODE, "${e.javaClass.simpleName}：找不到服务器地址")
        is SocketTimeoutException, is InterruptedIOException, is ConnectException, is NetworkErrorException, is SSLException -> Pair(
            OTHER_ERROR_CODE,
            "${e.javaClass.simpleName}：请求网络超时"
        )
        is HttpException -> convertStatusCode(e)
        is ParseException, is JSONException, is MalformedJsonException, is JsonSyntaxException -> Pair(
            OTHER_ERROR_CODE,
            "${e.javaClass.simpleName}：数据解析错误"
        )
        is IllegalStateException -> Pair(OTHER_ERROR_CODE, "${e.javaClass.simpleName}：非法状态错误")
        is ApiException -> Pair(e.code, "${e.message}")
        else -> Pair(OTHER_ERROR_CODE, "${e.javaClass.simpleName}：${e.message}")
    }
}

private fun convertStatusCode(httpException: HttpException): Pair<Int, String?> {
    return when (val exceptionCode = httpException.code()) {
        in 500..599 -> Pair(exceptionCode, "服务器处理请求出错")
        in 400..499 -> Pair(exceptionCode, "服务器无法处理请求")
        in 300..399 -> Pair(exceptionCode, "请求被重定向到其他页面")
        else -> Pair(OTHER_ERROR_CODE, httpException.message())
    }
}

