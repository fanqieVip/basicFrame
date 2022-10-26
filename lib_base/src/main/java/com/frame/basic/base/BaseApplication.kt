package com.frame.basic.base

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDexApplication
import com.blankj.utilcode.util.GsonUtils
import com.google.gson.GsonBuilder
import com.frame.basic.base.app.ActivityLifecycleCallbacksImpl
import com.frame.basic.base.app.ApplicationRebootHelper
import com.frame.basic.base.app.InitDepend
import com.frame.basic.base.app.LoadModuleProxy
import com.frame.basic.base.ktx.TimerManager
import com.frame.basic.base.utils.CrashHelper
import com.frame.basic.base.utils.DownloadManager
import com.frame.basic.base.utils.ProcessUtils
import com.frame.basic.base.utils.SpUtils
import com.frame.basic.base.utils.json.NullObjectJsonAdapter
import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

/**
 * Application 基类
 *
 * @author Qu Yunshuo
 * @since 4/24/21 5:30 PM
 */
open class BaseApplication : MultiDexApplication(), LifecycleObserver {
    private val mCoroutineScope by lazy(mode = LazyThreadSafetyMode.NONE) { MainScope() }

    private val mLoadModuleProxy by lazy(mode = LazyThreadSafetyMode.NONE) { LoadModuleProxy() }

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var application: BaseApplication
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        application = this
        mLoadModuleProxy.onAttachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        // 在初始化第三方SDK前，先同步初始化必要的sdk，避免冲突
        SpUtils.initMMKV(this) //初始化mmkv
        // 初始化定时任务
        TimerManager.build(mCoroutineScope, true)
        // 初始化下载管理器
        DownloadManager.build(mCoroutineScope, 5)
        // 全局监听 Activity 生命周期
        registerActivityLifecycleCallbacks(ActivityLifecycleCallbacksImpl())
        // 应用异常销毁重启，自动进入启动页
        if (BuildConfig.CRASH_REBOOT){
            registerActivityLifecycleCallbacks(ApplicationRebootHelper())
        }
        //全局捕获一些非人为因素造成的异常
        CrashHelper.filterException()
        mLoadModuleProxy.onCreate(this)
        // 策略初始化第三方依赖
        initDepends()
    }

    /**
     * 初始化基础框架中需要在主进程中初始化的功能
     */
    private fun initBaseByFrontDesk(): InitDepend {
        val worker = mutableListOf<() -> String>()
        val main = mutableListOf<() -> String>()
        main.add { initApplicationLifecycle() }
        main.add { initJsonUtils() }
        return InitDepend(main, worker)
    }

    /**
     * 初始化第三方依赖
     *
     * 步骤：
     * * 1. 首先开启一个后台协程对不会立即使用的第三方进行初始化
     * * 2. 对需要被立即使用的第三方进行初始化
     * * 2.1 首先是并行对非必须要在主线程初始化的依赖进行初始化 此时不用管初始化是否完成 紧接着进行下一步
     * * 2.2 对必须要在主线程初始化的依赖进行初始化 由于是在主线程 所以后面的操作等待初始化完成 这部分时间不能浪费掉 这就是为什么先并行初始化非主线程的 因为这部分时间会被利用上
     * * 2.3 等待所有并行初始化的job完成就结束了整个初始化过程
     */
    private fun initDepends() {
        // 开启一个 Default Coroutine 进行初始化不会立即使用的第三方
        mCoroutineScope.launch(Dispatchers.Default) {
            mLoadModuleProxy.initByBackstage()
        }

        // 初始化需要被立即初始化的第三方 多线程并行，并阻塞至全部完成
        val measureTimeMillis = measureTimeMillis {
            mCoroutineScope.launch(Dispatchers.Main.immediate) {
                // 基础框架中 + 各模块中在主进程中初始化的功能
                val depends = initBaseByFrontDesk().apply {
                    val mLoadModuleDependsProxy = mLoadModuleProxy.initByFrontDesk()
                    mainThreadDepends.addAll(mLoadModuleDependsProxy.mainThreadDepends)
                    workerThreadDepends.addAll(mLoadModuleDependsProxy.workerThreadDepends)
                }
                // 1. 对非必须在主线程初始化的第三方依赖发起并行初始化
                // 并行job
                var jobs: MutableList<Deferred<String>>? = null
                if (depends.workerThreadDepends.isNotEmpty()) {
                    jobs = mutableListOf()
                    depends.workerThreadDepends.forEach {
                        jobs.add(async(Dispatchers.Default) { it() })
                    }
                }

                // 2. 对必须在主线程初始化的第三方依赖进行初始化
                if (depends.mainThreadDepends.isNotEmpty()) {
                    depends.mainThreadDepends.forEach { it() }
                }

                // 3. 等待每一个子线程初始化的依赖完成
                jobs?.forEach { it.await() }
            }
        }
        Log.d("ApplicationInit", "初始化完成 $measureTimeMillis ms")
    }

    override fun onTerminate() {
        super.onTerminate()
        mLoadModuleProxy.onTerminate(this)
        mCoroutineScope.cancel()
    }


    /**
     * 初始化JSON解析器
     */
    private fun initJsonUtils(): String {
        GsonUtils.setGsonDelegate(
            GsonBuilder().registerTypeHierarchyAdapter(
                Any::class.java,
                NullObjectJsonAdapter()//将返回值中null（非字符串"null"）的项目移除掉，保证映射的空安全
            ).create()
        )
        return "JsonUtils -->> init complete"
    }


    /**
     * 初始化应用生命周期监听
     */
    private fun initApplicationLifecycle(): String {
        if (ProcessUtils.isMainProcess(this)) {
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        }
        return "ApplicationLifecycle -->> init complete"
    }

    /**
     * 当App被切换到前台时被调用
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onForeground() {
        mLoadModuleProxy.onForeground(this)
    }

    /**
     * 当App被切换到后台时被调用
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onBackground() {
        mLoadModuleProxy.onBackground(this)
    }
}