package com.frame.basic.base.mvvm.v

import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewbinding.ViewBinding
import com.blankj.utilcode.util.ScreenUtils
import com.google.common.collect.HashBiMap
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import com.frame.basic.base.R
import com.frame.basic.base.ktx.bindCheck
import com.frame.basic.base.ktx.onChecked
import com.frame.basic.base.mvvm.c.*
import com.frame.basic.base.mvvm.v.base.ContainerStyle
import com.frame.basic.base.mvvm.vm.BaseVM
import com.frame.basic.base.utils.*

/**
 * @Description:
 * @Author:         fanj
 * @CreateDate:     2021/11/10 9:58
 * @Version:        1.0.2
 */
abstract class BaseDialog<VB : ViewBinding, VM : BaseVM> : DialogFragment(), UIControl<VB>,
    ContainerStyle, RecreateControl, ArgumentsControl<BaseDialog<*, *>> {
    protected val mBinding: VB by lazy(mode = LazyThreadSafetyMode.NONE) {
        BindingReflex.reflexViewBinding<VB>(javaClass, layoutInflater).apply {
            if (this is ViewDataBinding) {
                lifecycleOwner = viewLifecycleOwner
            }
        }
    }
    protected abstract val mBindingVM: VM
    private lateinit var rootView: ViewGroup
    private var isRecreate = false
    private var recreateding = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //当全屏或者全透明度时，禁用backgroundDim，已确保状态栏文字颜色正常显示
        if (getDimAmount() <= 0 || (getHeight() == WindowManager.LayoutParams.MATCH_PARENT && getWidth() == WindowManager.LayoutParams.MATCH_PARENT)){
            setStyle(STYLE_NO_FRAME, R.style.base_dialog_dim_dis_enabled_dialog)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        //获取重建状态
        isRecreate = ViewRecreateHelper.getRecreateStatus(this, savedInstanceState)
        initWindowStyleBefore()
        initWindowStyle()
        rootView = customRootView(ContainerStyle.packageContainers(this))
        return rootView
    }

    /**
     * 初始化Window参数之前运行，用于动态修改一些window参数
     */
    open fun initWindowStyleBefore() {}

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
            mBindingVM.onRefresh(viewLifecycleOwner)
        }
        refreshLayout!!.setOnLoadMoreListener {
            mBindingVM.refreshLayoutState.value = RefreshLayoutStatus.LOAD_MORE
            if (mBindingVM is PagingControl) {
                (mBindingVM as PagingControl).apply {
                    if (mBindingVM.currentPageNo.value == null) {
                        mBindingVM.currentPageNo.value = getFirstPageNo()
                    }
                    onLoadMore(viewLifecycleOwner, mBindingVM.currentPageNo.value!! + 1)
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
        recyclerView.adapter = getAdapter(contentView.context)
        return contentView
    }

    //fragment默认禁止显示标题栏
    override fun isShowTitleBar() = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val params = dialog?.window?.attributes
        params?.windowAnimations = getAnimationStyle()
        dialog?.window?.attributes = params
        mBinding.initView()
        mBinding.initListener()
        initVMControlObserve()
        if (!isRecreate && mBindingVM.autoOnRefresh()) {
            mBindingVM.loading()
            mBindingVM.onRefresh(viewLifecycleOwner)
        }
        mBindingVM.executeForever(viewLifecycleOwner)
    }

    /**
     * 存储Tab分页
     */
    private val mFragmentMap by lazy {
        HashBiMap.create<Int, Class<out Fragment>>().apply {
            if (this@BaseDialog is TabPlugin) {
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
            FragmentTabControl(viewLifecycleOwner, fm, (this as TabPlugin).getFrameLayout().id)
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
            }
        }
        if (this is TabPlugin) {
            initTabPlugin()
        }
        if (this is IndicatorPlugin<*>) {
            initIndicatorPlugin()
        }
    }
    override fun onStop() {
        super.onStop()
        mBindingVM.displayStatus.value = DisplayStatus.HIDDEN
    }

    override fun onPause() {
        super.onPause()
        mBindingVM.displayStatus.value = DisplayStatus.PAUSING
    }
    override fun onResume() {
        super.onResume()
        mBindingVM.displayStatus.value = DisplayStatus.SHOWING
        recreateding = false
        //todo Dialog设置StatusBar, NavigationBar亮色模式无效，等待解决
        activity?.let {
            UIBarUtils.initStatusAndNavigationBar(it, this)
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        //关闭时恢复界面Host的状态栏
        val host = host
        if (host != null && host is UIControl<*>) {
            activity?.let {
                UIBarUtils.initStatusAndNavigationBar(it, host)
            }
        }

    }

    /**
     * 页面可能重建的时候回执行此方法，进行当前页面状态保存
     */
    override fun onSaveInstanceState(outState: Bundle) {
        ViewRecreateHelper.saveRecreateStatus(this, outState)
        recreateding = true
        super.onSaveInstanceState(outState)
    }

    final override fun getContentView() = realContentView
    private val realContentView by lazy { createRootView() }

    /**
     * 初始化Dialog的Window样式
     */
    private fun initWindowStyle() {
        if (getDimAmount() == 0f) {
            dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.attributes?.let {
            if (getAnimationStyle() != Int.MAX_VALUE) {
                it.windowAnimations = getAnimationStyle()
            }
            dialog?.window?.attributes = it
        }
        dialog?.setCanceledOnTouchOutside(getCancelOutside())
        isCancelable = getCancelable()
        // 去掉dialog默认的padding
        dialog?.window?.decorView?.setPadding(0, 0, 0, 0)
        //是否允许穿透事件
        if (getOutsideTouchable()) {
            dialog?.window?.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            )
        }
    }

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            dialog?.window?.let {
                it.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                it.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            }
        }

        dialog?.window?.attributes?.let {
            it.dimAmount = getDimAmount()
            val width = getWidth()
            //当设置横屏全屏且竖屏全屏时，安卓会默认吧状态栏设置成黑色并遮挡内容，-1像素看不出来但有效
            it.width = if (width == WindowManager.LayoutParams.MATCH_PARENT){
                ScreenUtils.getScreenWidth()-1
            }else{
                width
            }
            it.height = getHeight()
            it.gravity = getGravity()
            if (Build.VERSION.SDK_INT >= 28) {
                //适配挖孔屏, 避免界面被顶到孔孔下边
                it.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            dialog?.window?.attributes = it
        }
    }

    /**
     * 是否允许点击窗口外关闭
     */
    protected open fun getCancelOutside(): Boolean = true

    /**
     * 是否允许关闭
     */
    protected open fun getCancelable(): Boolean = true

    /**
     * 是否允许外部点击事件穿透
     */
    protected open fun getOutsideTouchable(): Boolean = false

    /**
     * 透明度
     * 0~1
     */
    protected open fun getDimAmount(): Float = 0.5f

    /**
     * 宽度
     */
    open fun getWidth(): Int = WindowManager.LayoutParams.MATCH_PARENT

    /**
     * 高度
     */
    open fun getHeight(): Int = WindowManager.LayoutParams.WRAP_CONTENT

    /**
     * 对齐方式
     */
    protected open fun getGravity(): Int = Gravity.CENTER

    /**
     * 标识
     */
    protected open fun getFragmentTag(): String = javaClass.name

    /**
     * 进出动画
     * 底部弹出动画：base_dialog_bottom_animation
     * 顶部弹出动画：base_dialog_top_animation
     * 中间弹出动画：base_dialog_center_animation
     * 右侧弹出动画：base_dialog_right_animation
     * 左侧弹出动画：base_dialog_left_animation
     */
    protected open fun getAnimationStyle(): Int = when (getGravity()) {
        Gravity.CENTER -> R.style.base_dialog_center_animation
        Gravity.LEFT, Gravity.START -> R.style.base_dialog_left_animation
        Gravity.RIGHT, Gravity.END -> R.style.base_dialog_right_animation
        Gravity.TOP -> R.style.base_dialog_top_animation
        Gravity.BOTTOM -> R.style.base_dialog_bottom_animation
        else -> Int.MAX_VALUE
    }

    open fun showDialog(fragmentManager: FragmentManager) {
        showDialog(fragmentManager, getFragmentTag())
    }

    open fun showDialog(fragmentManager: FragmentManager, hasPriority: Boolean = false) {
        showDialog(fragmentManager, getFragmentTag(), hasPriority)
    }

    open fun showDialog(
        fragmentManager: FragmentManager,
        tag: String,
        hasPriority: Boolean = false
    ) {
        realShowDialog(fragmentManager, tag)
    }

    open fun realShowDialog(fragmentManager: FragmentManager, tag: String) {
        try {
            if (!isAdded && fragmentManager.findFragmentByTag(tag) == null) {
                fragmentManager.beginTransaction().add(this, tag).commitNowAllowingStateLoss()
            } else {
                fragmentManager.beginTransaction().show(this).commitNowAllowingStateLoss()
            }
        } catch (e: Exception) {
            e.printStackTrace(System.out)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        onDisMissing = true
        super.onDismiss(dialog)
    }

    //是否正在关闭
    private var onDisMissing = false
    open fun dismissDialog() {
        try {
            onDisMissing = true
            dismissAllowingStateLoss()
        } catch (e: Exception) {
        }
    }

    /**
     * 如果刚调用了dismiss，马上再show是无法显示的，通常需要先知道是否已经dismiss了
     */
    fun isOnDisMissing() = onDisMissing

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
