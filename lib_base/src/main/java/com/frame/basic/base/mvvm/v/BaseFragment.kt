package com.frame.basic.base.mvvm.v

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewbinding.ViewBinding
import com.google.common.collect.HashBiMap
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import com.frame.basic.base.ktx.bindCheck
import com.frame.basic.base.ktx.onChecked
import com.frame.basic.base.mvvm.c.*
import com.frame.basic.base.mvvm.v.base.ContainerStyle
import com.frame.basic.base.mvvm.vm.BaseVM
import com.frame.basic.base.utils.*

/**
 * Fragment基类
 *
 * @author Qu Yunshuo
 * @since 8/27/20
 */
abstract class BaseFragment<VB : ViewBinding, VM : BaseVM> : Fragment(), UIControl<VB>,
    ContainerStyle, RecreateControl, ArgumentsControl<BaseFragment<*, *>>, VMLifecycle {

    protected val mBinding: VB by lazy(mode = LazyThreadSafetyMode.NONE) {
        BindingReflex.reflexViewBinding<VB>(javaClass, layoutInflater).apply {
            if (this is ViewDataBinding) {
                lifecycleOwner = viewLifecycleOwner
            }
        }
    }
    protected abstract val mBindingVM: VM

    //Fragment的View加载完毕的标记
    internal var isViewCreated = false

    //Fragment对用户可见的标记
    internal var isUIVisible = false
    private var isRecreate = false
    private var isLoad = false
    private var recreateding = false
    private lateinit var rootView: ViewGroup

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        initVmLifecycleOwner(viewLifecycleOwner)
        //获取重建状态
        isRecreate = ViewRecreateHelper.getRecreateStatus(this, savedInstanceState)
        isLoad = ViewRecreateHelper.getLoadStatus(this, savedInstanceState)
        rootView = customRootView(ContainerStyle.packageContainers(this))
        return rootView
    }

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
            if (this is WebViewPlugin) {
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
        }
        refreshLayout!!.setOnLoadMoreListener {
            mBindingVM.refreshLayoutState.value = RefreshLayoutStatus.LOAD_MORE
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
        recyclerView.adapter = getAdapter(contentView.context)
        return contentView
    }

    //fragment默认禁止显示标题栏
    override fun isShowTitleBar() = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isViewCreated = true
        if (isAttachToViewPager()) {
            lazyLoad()
        } else {
            load()
        }
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        //isVisibleToUser这个boolean值表示:该Fragment的UI 用户是否可见
        if (isVisibleToUser) {
            isUIVisible = true
            if (isAttachToViewPager()) {
                lazyLoad()
                if (isViewCreated && hasLazyLoad) {
                    onResumeVisible()
                    userVisibleHintForAttachToViewPager(true)
                    onHiddenChangedForAttachToViewPager(true)
                }
            }
        } else {
            isUIVisible = false
            if (isAttachToViewPager()) {
                if (isViewCreated && hasLazyLoad) {
                    onStopInvisible()
                    userVisibleHintForAttachToViewPager(false)
                    onHiddenChangedForAttachToViewPager(false)
                }
            }
        }
    }

    internal var hiddening = false
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        hiddening = hidden
        if (!isAttachToViewPager()) {
            if (!hidden) {
                onResumeVisible()
                userVisibleHintForAttachToViewPager(true)
                onHiddenChangedForAttachToViewPager(true)
            } else {
                onStopInvisible()
                userVisibleHintForAttachToViewPager(false)
                onHiddenChangedForAttachToViewPager(false)
            }
        }
    }

    private fun userVisibleHintForAttachToViewPager(visible: Boolean) {
        if (visible) {
            getSafelyChildFragmentManager()?.fragments?.find {
                it is BaseFragment<*, *> && it.isAttachToViewPager() && it.isViewCreated && it.hasLazyLoad && it.callIsMenuVisible()
            }?.let {
                it as BaseFragment<*, *>
                it.userVisibleHint = true
            }
        } else {
            getSafelyChildFragmentManager()?.fragments?.find {
                it is BaseFragment<*, *> && it.isAttachToViewPager() && it.isViewCreated && it.hasLazyLoad && it.isUIVisible
            }?.let {
                it as BaseFragment<*, *>
                it.userVisibleHint = false
            }
        }
    }

    private fun onHiddenChangedForAttachToViewPager(visible: Boolean) {
        if (visible) {
            getSafelyChildFragmentManager()?.fragments?.find {
                it is BaseFragment<*, *> && !it.isAttachToViewPager() && !it.isHidden
            }?.let {
                (it as BaseFragment<*, *>).onHiddenChanged(false)
            }
        } else {
            getSafelyChildFragmentManager()?.fragments?.find {
                it is BaseFragment<*, *> && !it.isAttachToViewPager() && !it.isHidden
            }?.let {
                (it as BaseFragment<*, *>).onHiddenChanged(true)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        mBindingVM.displayStatus.value = DisplayStatus.STOP
        if (!isHidden && !isAttachToViewPager() && !hiddening) {
            onStopInvisible()
        }
        if (isUIVisible && isAttachToViewPager() && isViewCreated && hasLazyLoad) {
            onStopInvisible()
        }
    }

    override fun onResume() {
        super.onResume()
        mBindingVM.displayStatus.value = DisplayStatus.RESUME
        recreateding = false
        if (isAttachToViewPager()) {
            if (isUIVisible && callIsMenuVisible() && isViewCreated && hasLazyLoad) {
                onResumeVisible()
            }
        } else {
            if (!isHidden && !hiddening) {
                onResumeVisible()
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun callIsMenuVisible(): Boolean {
        return isMenuVisible
    }

    /**
     * 是否添加到ViewPager或ViewPager2，默认false
     * 添加到ViewPager将支持懒加载
     * @return
     */
    open fun isAttachToViewPager() = false
    private val unicode by lazy { getUnicode(this) }
    open fun onResumeVisible() {
        activity?.let {
            UIBarUtils.initStatusAndNavigationBar(it, this)
        }
        mBindingVM.displayStatus.value = DisplayStatus.SHOWING
    }

    open fun onStopInvisible() {
        mBindingVM.displayStatus.value = DisplayStatus.HIDDEN
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun onPause() {
        super.onPause()
        mBindingVM.displayStatus.value = DisplayStatus.PAUSING
    }

    internal var hasLazyLoad = false
    private fun lazyLoad() {
        //这里进行双重标记判断,是因为setUserVisibleHint会多次回调,并且会在onCreateView执行前回调,必须确保onCreateView加载完毕且页面可见,才加载数据
        if (isViewCreated && isUIVisible && !hasLazyLoad) {
            load()
            //数据加载完毕,恢复标记,防止重复加载
            hasLazyLoad = true
        }
    }

    /**
     * 真正的onCreate
     */
    private fun load() {
        mBinding.initView()
        mBinding.initListener()
        initVMControlObserve()

        if ((!isRecreate || !isLoad) && mBindingVM.autoOnRefresh()) {
            mBindingVM.loading()
            mBindingVM.onRefresh(viewLifecycleOwner)
            isLoad = true
        }
        mBindingVM.executeForever(viewLifecycleOwner)
    }

    /**
     * 存储Tab分页
     */
    private val mFragmentMap by lazy {
        HashBiMap.create<Int, Class<out Fragment>>().apply {
            if (this@BaseFragment is TabPlugin) {
                bindFragment(this)
            }
        }
    }

    /**
     * Tab控制器
     */
    private val mMainTabControl by lazy {
        val fm = getSafelyChildFragmentManager()
        if (fm != null){
            FragmentTabControl(
                viewLifecycleOwner,
                fm,
                (this as TabPlugin).getFrameLayout().id
            )
                .apply {
                    mFragmentMap.forEach { (t, u) ->
                        val exitFragment = fm.fragments.findLast { it.javaClass == u }
                            ?: u.newInstance().apply {
                                bindArguments(t, this)
                            }
                        bind(t, exitFragment)
                    }
                }
        }else{
            null
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
                mMainTabControl?.show(checkId)
            }
        }
        getTargetTabFragment().observe(viewLifecycleOwner) {
            mFragmentMap.inverse()[getTargetTabFragment().value]?.let { id ->
                getRadioGroup().check(id)
            }
        }
        getRadioGroup().onChecked { checkId ->
            mMainTabControl?.show(checkId)
            getTargetTabFragment().value = mFragmentMap[checkId]
        }
    }

    /**
     * 加载分页插件
     */
    private fun initIndicatorPlugin() {
        this as IndicatorPlugin<*>
        context?.let {
            getSafelyChildFragmentManager()?.let { fm ->
                buildIndicator(it, viewLifecycleOwner, fm)
            }
        }
    }

    /**
     * 初始化系统控制Observe
     */
    private fun initVMControlObserve() {
        mBindingVM.uiStatus.observe(viewLifecycleOwner) {
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
        mBindingVM.popLoadingStatus.observe(viewLifecycleOwner) {
            if (it.isShow) {
                showPopLoading(it.msg)
            } else {
                dismissPopLoading()
            }
        }
        mBindingVM.refreshLayoutState.observe(viewLifecycleOwner) {
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
                RefreshLayoutStatus.REFRESH -> {
                    if (refreshLayout?.isRefreshing != true){
                        refreshLayout?.resetNoMoreData()
                    }
                    mBindingVM.onRefresh(viewLifecycleOwner)
                }
                RefreshLayoutStatus.LOAD_MORE -> {
                    if (mBindingVM is PagingControl) {
                        (mBindingVM as PagingControl).apply {
                            if (mBindingVM.currentPageNo.value == null) {
                                mBindingVM.currentPageNo.value = getFirstPageNo()
                            }
                            onLoadMore(viewLifecycleOwner, mBindingVM.currentPageNo.value!! + 1)
                        }
                    }
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
     * 页面可能重建的时候回执行此方法，进行当前页面状态保存
     */
    override fun onSaveInstanceState(outState: Bundle) {
        ViewRecreateHelper.saveRecreateStatus(this, outState)
        if (isLoad) {
            ViewRecreateHelper.saveLoadStatus(this, outState)
        }
        //记录正在重建中，如果onDestroy就不清除备份数据
        recreateding = true
        super.onSaveInstanceState(outState)
    }

    final override fun getContentView() = realContentView
    private val realContentView by lazy { createRootView() }
    override fun statusBarDarkFont(): Boolean? {
        val host = host
        if (host != null && host is UIControl<*>) {
            return host.statusBarDarkFont()
        }
        return null
    }

    override fun navigationBarColor(): Int? {
        val host = host
        if (host != null && host is UIControl<*>) {
            return host.navigationBarColor()
        }
        return null
    }

    override fun navigationBarDarkIcon(): Boolean? {
        val host = host
        if (host != null && host is UIControl<*>) {
            return host.navigationBarDarkIcon()
        }
        return null
    }

    final override fun isRecreating(): Boolean {
        return if (activity != null && activity is RecreateControl) {
            (requireActivity() as RecreateControl).isRecreating()
        } else {
            recreateding
        }
    }

    final override fun isRecreated() = isRecreate
    final override fun getLastRootView() = rootView

    /**
     * 获取安全的ParentFragmentManager，避免IllegalStateException("Fragment " + this + " not associated with a fragment manager.")异常
     */
    fun getSafelyParentFragmentManager(): FragmentManager? {
        if (isAdded){
            return parentFragmentManager
        }
        return null
    }

    /**
     * 获取安全的ChildFragmentManager，避免IllegalStateException("Fragment " + this + " has not been attached yet.")异常
     */
    fun getSafelyChildFragmentManager(): FragmentManager? {
        if (isAdded){
            return childFragmentManager
        }
        return null
    }
}
