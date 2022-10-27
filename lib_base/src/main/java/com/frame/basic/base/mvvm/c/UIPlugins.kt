package com.frame.basic.base.mvvm.c

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Message
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
import androidx.annotation.IntRange
import androidx.annotation.LayoutRes
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.chad.library.adapter.base.BaseProviderMultiAdapter
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.provider.BaseItemProvider
import com.chad.library.adapter.base.viewholder.BaseDataBindingHolder
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.common.collect.HashBiMap
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import com.frame.basic.base.ktx.bindCurrentItem
import com.frame.basic.base.ktx.onClick
import com.frame.basic.base.widget.NestRadioGroup
import net.lucode.hackware.magicindicator.MagicIndicator
import net.lucode.hackware.magicindicator.buildins.commonnavigator.CommonNavigator
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.CommonNavigatorAdapter
import net.lucode.hackware.magicindicator.buildins.commonnavigator.titles.CommonPagerTitleView
import java.net.MalformedURLException
import java.net.URL


/**
 * 实现该协议以支持上下拉交互功能
 * 如果要同时支持上下拉，则需要ViewModel中实现PagingControl控制器
 * 如果只是用下拉，则需要把enableLoadMore设置为false
 */
interface RefreshLayoutPlugin {
    /**
     * 定制SmartRefreshLayout样式
     */
    fun initSmartRefreshLayout(refreshLayout: SmartRefreshLayout) {
        refreshLayout.setRefreshHeader(ClassicsHeader(refreshLayout.context))
        refreshLayout.setRefreshFooter(ClassicsFooter(refreshLayout.context))
        refreshLayout.setEnableLoadMore(enableLoadMore())
        refreshLayout.setEnableRefresh(enableRefresh())
        refreshLayout.setEnableAutoLoadMore(enableAutoLoadMore())
    }

    fun enableLoadMore() = true
    fun enableRefresh() = true
    fun enableAutoLoadMore() = true
}

/**
 * 实现该协议以支持列表展示能力
 */
interface RecyclerViewPlugin {
    fun getRecyclerView(): RecyclerView
    fun initRecyclerView(recyclerView: RecyclerView)
    fun getAdapter(context: Context): RecyclerView.Adapter<out RecyclerView.ViewHolder>
}

/**
 * 实现该协议以支持"标准列表"展示能力
 */
interface RecyclerViewBasicPlugin<T, VB : ViewDataBinding> : RecyclerViewPlugin {
    override fun initRecyclerView(recyclerView: RecyclerView) {
        recyclerView.layoutManager = LinearLayoutManager(recyclerView.context)
    }

    override fun getAdapter(context: Context): RecyclerView.Adapter<out RecyclerView.ViewHolder> {
        val mutableLiveData = getMutableLiveData()
        val adapter = object :
            BaseQuickAdapter<T, BaseDataBindingHolder<VB>>(getItemId(), mutableLiveData.value) {
            override fun convert(holder: BaseDataBindingHolder<VB>, item: T) {
                this@RecyclerViewBasicPlugin.convert(holder, item)
            }
        }
        val owner: LifecycleOwner? = when (this@RecyclerViewBasicPlugin) {
            is AppCompatActivity -> {
                this
            }
            is Fragment -> {
                this.viewLifecycleOwner
            }
            else -> null
        }
        owner?.let {
            mutableLiveData.observe(owner) {
                adapter.setList(it)
            }
        }
        bindAdapterListener(adapter)
        return adapter
    }

    /**
     * 绑定事件
     */
    fun bindAdapterListener(adapter: BaseQuickAdapter<T, BaseDataBindingHolder<VB>>)

    /**
     * 返回数据源
     */
    fun getMutableLiveData(): MutableLiveData<MutableList<T>>

    /**
     * 获取item的xml
     */
    @LayoutRes
    fun getItemId(): Int

    /**
     * 绘制item
     */
    fun convert(holder: BaseDataBindingHolder<VB>, item: T)

}

/**
 * 实现该协议以支持"多布局列表"展示能力
 */
interface RecyclerViewMultiPlugin<T> : RecyclerViewPlugin {
    override fun initRecyclerView(recyclerView: RecyclerView) {
        recyclerView.layoutManager = LinearLayoutManager(recyclerView.context)
    }

    override fun getAdapter(context: Context): RecyclerView.Adapter<out RecyclerView.ViewHolder> {
        val mutableLiveData = getMutableLiveData()
        val adapter = object : BaseProviderMultiAdapter<T>(mutableLiveData.value) {
            override fun getItemType(data: List<T>, position: Int): Int {
                return this@RecyclerViewMultiPlugin.getItemType(data as MutableList<T>, position)
            }
        }
        getMultiItemProviders().forEach {
            adapter.addItemProvider(it)
        }
        val owner: LifecycleOwner? = when (this@RecyclerViewMultiPlugin) {
            is AppCompatActivity -> {
                this
            }
            is Fragment -> {
                this.viewLifecycleOwner
            }
            else -> null
        }
        owner?.let {
            mutableLiveData.observe(owner) {
                adapter.setList(it)
            }
        }
        return adapter
    }

    /**
     * 返回item的样式标识
     */
    fun getItemType(data: MutableList<T>, position: Int): Int

    /**
     * 返回数据源
     */
    fun getMutableLiveData(): MutableLiveData<MutableList<T>>

    /**
     * 返回各布局的实现
     */
    fun getMultiItemProviders(): MutableList<MultiItemProvider<T, out ViewDataBinding>>
}

/**
 * 多布局的item节点基类
 */
abstract class MultiItemProvider<T, VB : ViewDataBinding> : BaseItemProvider<T>() {
    final override fun convert(helper: BaseViewHolder, item: T) {
        convert(BaseDataBindingHolder(helper.itemView), item)
    }

    abstract fun convert(holder: BaseDataBindingHolder<VB>, item: T)
}

/**
 * 实现该协议以支持协调者布局
 */
interface CoordinatorPlugin {
    /**
     * 构建协调者布局
     */
    fun initCoordinator(context: Context, bodyView: View? = null): ViewGroup {
        //仅在默认body参数未传入时才使用自定义body
        val headLayoutView = getHeadLayoutView()
        val bodyLayoutView = bodyView ?: getBodyLayoutView()
        val collapsingToolbarLayout = CollapsingToolbarLayout(context).apply {
            initCollapsingToolbarLayout(this, headLayoutView, bodyLayoutView)
            addView(
                headLayoutView,
                CollapsingToolbarLayout.LayoutParams(
                    CollapsingToolbarLayout.LayoutParams.MATCH_PARENT,
                    CollapsingToolbarLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    initCollapsingToolbarLayoutLayoutParams(this)
                }
            )
        }
        val appBarLayout = AppBarLayout(context).apply {
            initAppBarLayout(this, headLayoutView, bodyLayoutView)
            addView(
                collapsingToolbarLayout,
                AppBarLayout.LayoutParams(
                    AppBarLayout.LayoutParams.MATCH_PARENT,
                    AppBarLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    initAppBarLayoutLayoutParams(this)
                }
            )
        }
        return CoordinatorLayout(context).apply {
            initCoordinatorLayout(this, headLayoutView, bodyLayoutView)
            addView(
                appBarLayout,
                CoordinatorLayout.LayoutParams(
                    CoordinatorLayout.LayoutParams.MATCH_PARENT,
                    CoordinatorLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    getHeadLayoutBehavior()?.let {
                        behavior = it
                    }
                }
            )
            bodyLayoutView?.let {
                addView(
                    bodyLayoutView,
                    CoordinatorLayout.LayoutParams(
                        CoordinatorLayout.LayoutParams.MATCH_PARENT,
                        CoordinatorLayout.LayoutParams.MATCH_PARENT
                    ).apply {
                        getBodyLayoutBehavior()?.let {
                            behavior = it
                        }
                    }
                )
            }
        }
    }

    /**
     * 自定义CoordinatorLayout
     */
    fun initCoordinatorLayout(
        coordinatorLayout: CoordinatorLayout,
        headLayoutView: View,
        bodyLayoutView: View?
    ) {
    }

    /**
     * 自定义AppBarLayout
     */
    fun initAppBarLayout(appBarLayout: AppBarLayout, headLayoutView: View, bodyLayoutView: View?) {
    }

    /**
     *
     * 自定义CollapsingToolbarLayout
     */
    fun initCollapsingToolbarLayout(
        collapsingToolbarLayout: CollapsingToolbarLayout,
        headLayoutView: View,
        bodyLayoutView: View?
    ) {
    }

    /**
     * 自定义body的协调者Behavior
     */
    fun getBodyLayoutBehavior(): CoordinatorLayout.Behavior<View>? =
        AppBarLayout.ScrollingViewBehavior()

    /**
     * 自定义head的协调者Behavior
     */
    fun getHeadLayoutBehavior(): AppBarLayout.Behavior? = null

    /**
     * 自定义AppBarLayout.LayoutParams参数
     */
    fun initAppBarLayoutLayoutParams(layoutParams: AppBarLayout.LayoutParams) {
        layoutParams.scrollFlags =
            AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED
    }

    /**
     * 自定义CollapsingToolbarLayout.LayoutParams参数
     */
    fun initCollapsingToolbarLayoutLayoutParams(layoutParams: CollapsingToolbarLayout.LayoutParams) {
        layoutParams.collapseMode = CollapsingToolbarLayout.LayoutParams.COLLAPSE_MODE_OFF
    }

    /**
     * 返回Head
     */
    fun getHeadLayoutView(): View

    /**
     * 返回body
     * 当和RecyclerViewPlugin搭配使用时，可不返回
     */
    fun getBodyLayoutView(): View?
}

/**
 * 实现该协议以支持Tab分栏功能
 * RadioGroup + FrameLayout组合
 */
interface TabPlugin {
    /**
     * RadioGroup
     */
    fun getRadioGroup(): NestRadioGroup

    /**
     * FrameLayout
     */
    fun getFrameLayout(): FrameLayout

    /**
     * 当前显示的Fragment的MutableLiveData数据源
     */
    fun getTargetTabFragment(): MutableLiveData<Class<out Fragment>>

    /**
     * 绑定按钮到fragment
     */
    fun bindFragment(map: HashBiMap<Int, Class<out Fragment>>)

    /**
     * 绑定Fragment参数，可选实现
     */
    fun bindArguments(id: Int, fragment: Fragment) {}
}

/**
 * 实现该协议以支持Indicator分栏功能
 * MagicIndicator + ViewPager组合
 * T: 标题类型
 */
interface IndicatorPlugin<T> : ViewPager.OnPageChangeListener {
    /**
     * 获取fragments
     */
    fun getIndicatorFragments(): ArrayList<Fragment>

    /**
     * 获取标题
     */
    fun getIndicatorTitles(): MutableLiveData<ArrayList<T>>

    /**
     * 获取标题栏样式
     */
    fun getPagerTitleView(context: Context, index: Int, title: T): CommonPagerTitleView

    /**
     * 分页器
     */
    fun getMagicIndicator(): MagicIndicator

    /**
     * ViewPager
     */
    fun getViewPager(): ViewPager

    /**
     * ViewPager是否平滑翻页
     */
    fun getPageSmoothScroll() = false

    /**
     * 当前定位的位置
     */
    fun getCurrentPosition(): MutableLiveData<Int>

    /**
     * 构建分页器
     */
    fun buildIndicator(context: Context, owner: LifecycleOwner, fragmentManager: FragmentManager) {
        if (getIndicatorTitles().value == null) {
            throw NullPointerException("IndicatorPlugin's indicatorTitles must initialize first")
        }
        getMagicIndicator().navigator = CommonNavigator(context).apply {
            adapter = object : CommonNavigatorAdapter() {
                override fun getCount() = getIndicatorTitles().value!!.size

                override fun getTitleView(context: Context, index: Int) =
                    getPagerTitleView(context, index, getIndicatorTitles().value!![index]).apply {
                        onClick {
                            getViewPager().bindCurrentItem(index, getPageSmoothScroll())
                        }
                    }

                override fun getIndicator(context: Context?) = null
            }
        }
        getViewPager().addOnPageChangeListener(this)
        getIndicatorTitles().observe(owner) {
            getMagicIndicator().navigator?.notifyDataSetChanged()
            notifyViewPager(context, fragmentManager)
        }
        getCurrentPosition().observe(owner) {
            getViewPager().currentItem = it
        }
    }

    override fun onPageScrollStateChanged(state: Int) {
        getMagicIndicator().onPageScrollStateChanged(state)
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        getMagicIndicator().onPageScrolled(position, positionOffset, positionOffsetPixels)
    }

    override fun onPageSelected(position: Int) {
        getMagicIndicator().onPageSelected(position)
    }

    fun notifyViewPager(context: Context, fragmentManager: FragmentManager) {
        getViewPager().offscreenPageLimit = getIndicatorTitles().value!!.size
        getViewPager().adapter = ViewPagerFragmentAdapter(fragmentManager, getIndicatorFragments())
        getViewPager().currentItem = getCurrentPosition().value ?: 0
    }
}

private class ViewPagerFragmentAdapter(
    fm: FragmentManager,
    private var fragments: ArrayList<Fragment>
) :
    FragmentStatePagerAdapter(fm) {
    override fun getCount() = fragments.size
    override fun getItemPosition(`object`: Any) = POSITION_NONE
    override fun getItem(position: Int) = fragments[position]
    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {}
}

/**
 * 浏览器插件
 */
interface WebViewPlugin {
    /**
     * 允许下载的文件类型
     * 如".apk"
     */
    val downloadType: Array<String>?

    /**
     * 是否支持多窗
     * 返回null则不支持
     */
    val multipleWindow: ((url: String) -> Unit)?

    fun getWebView(): WebView

    /**
     * 浏览器已回退到底
     */
    fun goBackEnd()

    /**
     * 当前页面的标题回调
     */
    fun onReceivedTitle(title: String?)

    /**
     *  返回JS交互的接口
     */
    fun getJavascriptInterface(): HashMap<String, Any>?

    /**
     * 当前页面的加载进度
     */
    fun onProgressChanged(@IntRange(from = 0, to = 100) newProgress: Int)

    /**
     * 开始加载回调
     */
    fun onPageStarted(url: String?)

    /**
     * 加载完成回调
     */
    fun onPageFinished(url: String?)

    /**
     * 加载失败回调
     */
    fun onReceivedError(errorCode: Int)

    /**
     * 请求下载文件回调
     */
    fun onReceivedDownload(url: String)

    /**
     * 刷新当前页面
     */
    fun reload() {
        getWebView().reload()
    }

    /**
     * 回退一页
     * @return 是否执行回退成功
     */
    fun goBack(): Boolean {
        if (getWebView().canGoBack()) {
            getWebView().goBack()
            return true
        }
        return false
    }

    /**
     * 前进一页
     * @return 是否执行前进成功
     */
    fun goForward(): Boolean {
        if (getWebView().canGoForward()) {
            getWebView().goForward()
            return true
        }
        return false
    }

    /**
     * 去指定页
     * @return 是否执行成功
     */
    fun goBackOrForward(steps: Int): Boolean {
        if (getWebView().canGoBackOrForward(steps)) {
            getWebView().goBackOrForward(steps)
            return true
        }
        return false
    }

    /**
     * 选择文件
     */
    fun onShowFileChooser(
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: WebChromeClient.FileChooserParams?
    ): Boolean

    /**
     * 回收WebView
     */
    private fun recycleWebView() {
        getWebView()?.let {
            it.onPause()
            it.pauseTimers()
            (it.parent as? ViewGroup)?.removeView(it)
            it.stopLoading()
            it.settings.javaScriptEnabled = false
            it.clearHistory()
            it.clearFocus()
            it.loadUrl("about:blank")
            it.removeAllViews()
            it.destroy()
        }
    }


    @SuppressLint("JavascriptInterface")
    fun initWebView() {
        getWebView().let {
            //基础配置
            it.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                databaseEnabled = true
                setAppCacheEnabled(true)
                blockNetworkImage = true
                if (multipleWindow != null) {
                    setSupportMultipleWindows(true)
                    javaScriptCanOpenWindowsAutomatically = true
                }
                cacheMode = WebSettings.LOAD_NO_CACHE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
                cacheMode
            }
            getJavascriptInterface()?.let { jsMap ->
                jsMap.forEach { entry ->
                    it.addJavascriptInterface(entry.value, entry.key)
                }
            }
            var isRedirect = true
            it.webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    return onShowFileChooser(filePathCallback, fileChooserParams)
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
                    isRedirect = false
                    Log.d("onPage", "${javaClass.name}:onReceivedTitle")
                    if (title?.contains("网页无法打开", true) == true ||
                        title?.contains("The website can not be found", true) == true ||
                        title?.contains("The page can not be found", true) == true
                    ) {
                        onReceivedError(404)
                    } else {
                        onReceivedTitle(title)
                        onPageFinished(view?.url)
                    }
                }

                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    if (newProgress == 100){
                        it.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                        it.settings.blockNetworkImage = false
                    }
                    onProgressChanged(newProgress)
                }

                override fun onCreateWindow(
                    view: WebView?,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message?
                ): Boolean {
                    return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
                }
            }
            it.setOnKeyListener { v, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
                    val back = goBack()
                    if (!back) {
                        goBackEnd()
                    }
                    back
                } else {
                    keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP
                }
            }
            it.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                onReceivedDownload(url)
            }
            it.webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    onPageStarted(url)
                    isRedirect = true
                    Log.d("onPage", "${javaClass.name}:onPageStarted")
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    isRedirect = false
                    Log.d("onPage", "${javaClass.name}:onPageEnd")
                }

                @RequiresApi(Build.VERSION_CODES.M)
                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    if (request?.isForMainFrame == true) {
                        onReceivedError(error?.errorCode ?: 404)
                    }
                }

                override fun onReceivedSslError(
                    view: WebView?,
                    handler: SslErrorHandler?,
                    error: SslError?
                ) {
                    //处理HTTPS_SSL拦截白屏的问题
                    handler?.proceed()
                }

                override fun onReceivedError(
                    view: WebView?,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                }

                override fun onReceivedHttpError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    errorResponse: WebResourceResponse?
                ) {
                    super.onReceivedHttpError(view, request, errorResponse)
                    if (request?.isForMainFrame == true) {
                        onReceivedError(errorResponse?.statusCode ?: 404)
                    }
                }

                @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    Log.d("onPage", "${javaClass.name}:shouldOverrideUrlLoading")
                    return if (request != null) {
                        overrideUrlLoading(view, request.url.toString())
                    } else {
                        overrideUrlLoading(view, "")
                    }
                }

                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    return overrideUrlLoading(view, url ?: "")
                }

                /**
                 * 检测是否是允许下载的文件
                 */
                private fun checkIsFile(url: String): Boolean {
                    if (!downloadType.isNullOrEmpty()) {
                        val splitPos = url.lastIndexOf("/")
                        if (splitPos >= 0) {
                            val newUrl = url.substring(splitPos)
                            for (i in downloadType!!) {
                                if (newUrl.endsWith(i, true) || newUrl.indexOf(
                                        "${i}?",
                                        0,
                                        true
                                    ) >= 0
                                ) {
                                    return true
                                }
                            }
                        }
                    }
                    return false
                }

                private fun overrideUrlLoading(view: WebView?, requestUrl: String): Boolean {
                    view?.settings?.blockNetworkImage = true
                    view?.settings?.cacheMode = WebSettings.LOAD_NO_CACHE
                    if (view == null) {
                        return false
                    }
                    return if (requestUrl.startsWith("http://", true) || requestUrl.startsWith(
                            "https://",
                            true
                        )
                    ) {
                        if (checkIsFile(requestUrl)) {
                            //如果是下载APK链接
                            onReceivedDownload(requestUrl)
                            true
                        } else {
                            if (multipleWindow != null) {
                                if (!isSameDomain(
                                        view.originalUrl ?: "",
                                        requestUrl
                                    ) && !isRedirect
                                ) {
                                    multipleWindow?.invoke(requestUrl)
                                    true
                                } else {
                                    false
                                }
                            } else {
                                false
                            }
                        }
                    } else {
                        //加载的url是自定义协议地址
                        try {
                            view.context.startActivity(
                                Intent.parseUri(
                                    requestUrl,
                                    Intent.URI_INTENT_SCHEME
                                )
                            )
                        } catch (e: Exception) {
                        }
                        true
                    }
                }

                /**
                 * 检测相同域名
                 */
                private fun isSameDomain(url1: String, url2: String): Boolean {
                    return try {
                        val host1 = URL(url1).host
                        val host2 = URL(url2).host
                        host1 == host2
                    } catch (e: MalformedURLException) {
                        false
                    }
                }
            }
        }
        val lifecycleOwner = when (this) {
            is AppCompatActivity -> lifecycle
            is Fragment -> viewLifecycleOwner.lifecycle
            else -> null
        }
        lifecycleOwner?.apply {
            addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    when (event) {
                        Lifecycle.Event.ON_PAUSE -> {
                            getWebView()?.let {
                                it.onPause()
                                it.pauseTimers()
                                if ((source is Activity && source.isFinishing) || (source is Fragment && source.activity?.isFinishing == true)) {
                                    recycleWebView()
                                }
                            }
                        }
                        Lifecycle.Event.ON_RESUME -> {
                            getWebView()?.let {
                                it.onResume()
                                it.resumeTimers()
                            }
                        }
                        Lifecycle.Event.ON_DESTROY -> {
                            recycleWebView()
                        }
                    }
                }
            })

        }
    }
}
