package com.frame.basic.base.mvvm.v

import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.blankj.utilcode.util.KeyboardUtils
import com.google.common.collect.HashBiMap
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import com.frame.basic.base.ktx.bindCheck
import com.frame.basic.base.ktx.onChecked
import com.frame.basic.base.mvvm.c.*
import com.frame.basic.base.mvvm.v.base.ContainerStyle
import com.frame.basic.base.mvvm.vm.BaseVM
import com.frame.basic.base.utils.*

/**
 * Activity基类
 *
 * @author Qu Yunshuo
 * @since 8/27/20
 */
abstract class BaseActivity<VB : ViewBinding, VM : BaseVM> : AppCompatActivity(),
    UIControl<VB>, ContainerStyle, RecreateControl, VMLifecycle {
    protected val mBinding: VB by lazy(mode = LazyThreadSafetyMode.NONE) {
        BindingReflex.reflexViewBinding<VB>(javaClass, layoutInflater).apply {
            if (this is ViewDataBinding) {
                lifecycleOwner = this@BaseActivity
            }
        }
    }
    protected abstract val mBindingVM: VM
    private lateinit var rootView: ViewGroup
    private var isRecreate = false
    private var recreateding = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initVmLifecycleOwner(this)
        fullScreen()
        //获取重建状态
        isRecreate = ViewRecreateHelper.getRecreateStatus(this, savedInstanceState)
        rootView = customRootView(ContainerStyle.packageContainers(this))
        if (!launchModel()) {
            setContentView(rootView)
        }
        if (inputFitKeyBoard()){
            SoftHideKeyBoardUtil.assistActivity(this, true)
        }
        initVMControlObserve()
        mBinding.initView()
        mBinding.initListener()
        if (!isRecreate && mBindingVM.autoOnRefresh()) {
            mBindingVM.loading()
            mBindingVM.onRefresh(this)
        }
        mBindingVM.executeForever(this)
    }

    /**
     * 启动页模式
     * 该模式会禁止返回ContentView
     */
    protected open fun launchModel() = false

    /**
     * 输入文字时，键盘是否自动将布局全部顶上去
     */
    protected open fun inputFitKeyBoard() = false

    private fun createRootView(): View {
        var temp = mBinding.root
        //必须这样因为下面的分支可能会包含上一个分支
        if (this is RecyclerViewPlugin && this is CoordinatorPlugin) {
            //如果既是RecyclerViewPlugin又是CoordinatorPlugin，则默认绑定二者关系
            temp = initCoordinatorPlugin(temp, initRecyclerViewPlugin(temp))
        } else {
            if (this is CoordinatorPlugin) {
                temp = initCoordinatorPlugin(temp)
            }
            if (this is RecyclerViewPlugin) {
                temp = initRecyclerViewPlugin(temp)
            }
            if (this is WebViewPlugin){
                initWebView()
            }
        }
        if (this is RefreshLayoutPlugin) {
            temp = initRefreshLayoutPlugin(temp)
        }
        return temp
    }

    /**
     * 加载协调者布局插件
     */
    private fun initCoordinatorPlugin(contentView: View, bodyView: View? = null): ViewGroup {
        this as CoordinatorPlugin
        return initCoordinator(contentView.context, bodyView)
    }

    private var refreshLayout: SmartRefreshLayout? = null

    /**
     * 加载刷新插件
     */
    private fun initRefreshLayoutPlugin(contentView: View): ViewGroup {
        this as RefreshLayoutPlugin
        refreshLayout = SmartRefreshLayout(contentView.context).also {
            initSmartRefreshLayout(it)
        }
        refreshLayout!!.addView(
            contentView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        refreshLayout!!.setOnRefreshListener {
            mBindingVM.refreshLayoutState.value = RefreshLayoutStatus.REFRESH
            mBindingVM.onRefresh(this)
        }
        refreshLayout!!.setOnLoadMoreListener {
            mBindingVM.refreshLayoutState.value = RefreshLayoutStatus.LOAD_MORE
            if (mBindingVM is PagingControl) {
                (mBindingVM as PagingControl).apply {
                    if (mBindingVM.currentPageNo.value == null) {
                        mBindingVM.currentPageNo.value = getFirstPageNo()
                    }
                    onLoadMore(this@BaseActivity, mBindingVM.currentPageNo.value!! + 1)
                }
            }
        }
        return refreshLayout!!
    }

    /**
     * 加载列表插件
     */
    private fun initRecyclerViewPlugin(contentView: View): View {
        this as RecyclerViewPlugin
        val recyclerView = getRecyclerView().also {
            initRecyclerView(it)
        }
        recyclerView.adapter = getAdapter(this)
        return contentView
    }


    /**
     * 存储Tab分页
     */
    private val mFragmentMap by lazy {
        HashBiMap.create<Int, Class<out Fragment>>().apply {
            if (this@BaseActivity is TabPlugin) {
                bindFragment(this)
            }
        }
    }

    /**
     * Tab控制器
     */
    private val mMainTabControl by lazy {
        FragmentTabControl(this, supportFragmentManager, (this as TabPlugin).getFrameLayout().id)
            .apply {
                mFragmentMap.forEach { (t, u) ->
                    val exitFragment =
                        supportFragmentManager.fragments.findLast { it.javaClass == u }
                            ?: u.newInstance().apply {
                                bindArguments(t, this)
                            }
                    bind(t, exitFragment)
                }
            }
    }

    /**
     * 加载分栏插件
     */
    private fun initTabPlugin() {
        this as TabPlugin
        getTargetTabFragment().value?.let {
            mFragmentMap.inverse()[it]?.let { checkId ->
                getRadioGroup().bindCheck(checkId)
                mMainTabControl.show(checkId)
            }
        }
        getTargetTabFragment().observe(this) {
            mFragmentMap.inverse()[getTargetTabFragment().value]?.let { id ->
                getRadioGroup().check(id)
            }
        }
        getRadioGroup().onChecked { checkId ->
            mMainTabControl.show(checkId)
            getTargetTabFragment().value = mFragmentMap[checkId]
        }
    }

    /**
     * 加载分页插件
     */
    private fun initIndicatorPlugin() {
        this as IndicatorPlugin<*>
        buildIndicator(this, this, supportFragmentManager)
    }

    /**
     * 初始化系统控制Observe
     */
    private fun initVMControlObserve() {
        mBindingVM.uiStatus.observe(this) {
            when (it.uiStatus) {
                UIStatus.LOADING -> ContainerStyle.replaceToLoadingView(rootView, this)
                UIStatus.SUCCESS -> ContainerStyle.replaceToContentView(rootView, this)
                UIStatus.EMPTY -> ContainerStyle.replaceToEmptyView(rootView, this)
                UIStatus.ERROR -> ContainerStyle.replaceToErrorView(
                    rootView,
                    this,
                    it.error,
                    it.msg
                )
            }
            mBindingVM.uiStatusMode.value = it.uiStatus
        }
        mBindingVM.popLoadingStatus.observe(this) {
            if (it.isShow) {
                showPopLoading(it.msg)
            } else {
                dismissPopLoading()
            }
        }
        mBindingVM.refreshLayoutState.observe(this) {
            when (it) {
                RefreshLayoutStatus.STOP -> {
                    if (refreshLayout?.isRefreshing != false) {
                        refreshLayout?.finishRefresh()
                    }
                    if (refreshLayout?.isLoading != false) {
                        refreshLayout?.finishLoadMore()
                    }
                }
                RefreshLayoutStatus.NO_MORE -> {
                    refreshLayout?.finishLoadMoreWithNoMoreData()
                }
            }
        }
        if (this is TabPlugin) {
            initTabPlugin()
        }
        if (this is IndicatorPlugin<*>) {
            initIndicatorPlugin()
        }
    }


    /**
     * 设置沉浸式状态栏
     */
    private fun fullScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.TRANSPARENT
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        //记录重建状态
        ViewRecreateHelper.saveRecreateStatus(this, outState)
        recreateding = true
        super.onSaveInstanceState(outState)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 解决某些特定机型会触发的Android本身的Bug
        AndroidBugFixUtils().fixSoftInputLeaks(this)
    }

    override fun finish() {
        super.finish()
        recreateding = false
    }

    override fun onStop() {
        super.onStop()
        mBindingVM.displayStatus.value = DisplayStatus.HIDDEN
    }

    override fun onResume() {
        super.onResume()
        mBindingVM.displayStatus.value = DisplayStatus.SHOWING
        recreateding = false
        UIBarUtils.initStatusAndNavigationBar(this, this)
    }
    override fun onPause() {
        super.onPause()
        mBindingVM.displayStatus.value = DisplayStatus.PAUSING
    }
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (autoHideSoftInput()){
            if (ev?.action == MotionEvent.ACTION_DOWN) {
                currentFocus?.let {
                    if (KeyboardUtils.isSoftInputVisible(this) && SoftHideKeyBoardUtil.isShouldHideKeyboard(it, ev)) {
                        KeyboardUtils.hideSoftInput(it)
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
    protected open fun autoHideSoftInput() = true

    final override fun getContentView() = realContentView
    private val realContentView by lazy { createRootView() }
    final override fun isRecreating() = recreateding
    final override fun isRecreated() = isRecreate
    final override fun getLastRootView() = rootView
}
