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
import com.frame.basic.base.ktx.bindCurrentItem
import com.frame.basic.base.ktx.onClick
import com.frame.basic.base.widget.NestRadioGroup
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.common.collect.HashBiMap
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import net.lucode.hackware.magicindicator.MagicIndicator
import net.lucode.hackware.magicindicator.buildins.commonnavigator.CommonNavigator
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.CommonNavigatorAdapter
import net.lucode.hackware.magicindicator.buildins.commonnavigator.titles.CommonPagerTitleView
import java.net.MalformedURLException
import java.net.URL


/**
 * ?????????????????????????????????????????????
 * ??????????????????????????????????????????ViewModel?????????PagingControl?????????
 * ????????????????????????????????????enableLoadMore?????????false
 */
interface RefreshLayoutPlugin {
    /**
     * ??????SmartRefreshLayout??????
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
 * ??????????????????????????????????????????
 */
interface RecyclerViewPlugin {
    fun getRecyclerView(): RecyclerView
    fun initRecyclerView(recyclerView: RecyclerView)
    fun getAdapter(context: Context): RecyclerView.Adapter<out RecyclerView.ViewHolder>
}

/**
 * ????????????????????????"????????????"????????????
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
     * ????????????
     */
    fun bindAdapterListener(adapter: BaseQuickAdapter<T, BaseDataBindingHolder<VB>>)

    /**
     * ???????????????
     */
    fun getMutableLiveData(): MutableLiveData<MutableList<T>>

    /**
     * ??????item???xml
     */
    @LayoutRes
    fun getItemId(): Int

    /**
     * ??????item
     */
    fun convert(holder: BaseDataBindingHolder<VB>, item: T)

}

/**
 * ????????????????????????"???????????????"????????????
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
     * ??????item???????????????
     */
    fun getItemType(data: MutableList<T>, position: Int): Int

    /**
     * ???????????????
     */
    fun getMutableLiveData(): MutableLiveData<MutableList<T>>

    /**
     * ????????????????????????
     */
    fun getMultiItemProviders(): MutableList<MultiItemProvider<T, out ViewDataBinding>>
}

/**
 * ????????????item????????????
 */
abstract class MultiItemProvider<T, VB : ViewDataBinding> : BaseItemProvider<T>() {
    final override fun convert(helper: BaseViewHolder, item: T) {
        convert(BaseDataBindingHolder(helper.itemView), item)
    }

    abstract fun convert(holder: BaseDataBindingHolder<VB>, item: T)
}

/**
 * ???????????????????????????????????????
 */
interface CoordinatorPlugin {
    /**
     * ?????????????????????
     */
    fun initCoordinator(context: Context, bodyView: View? = null): ViewGroup {
        //????????????body????????????????????????????????????body
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
     * ?????????CoordinatorLayout
     */
    fun initCoordinatorLayout(
        coordinatorLayout: CoordinatorLayout,
        headLayoutView: View,
        bodyLayoutView: View?
    ) {
    }

    /**
     * ?????????AppBarLayout
     */
    fun initAppBarLayout(appBarLayout: AppBarLayout, headLayoutView: View, bodyLayoutView: View?) {
    }

    /**
     *
     * ?????????CollapsingToolbarLayout
     */
    fun initCollapsingToolbarLayout(
        collapsingToolbarLayout: CollapsingToolbarLayout,
        headLayoutView: View,
        bodyLayoutView: View?
    ) {
    }

    /**
     * ?????????body????????????Behavior
     */
    fun getBodyLayoutBehavior(): CoordinatorLayout.Behavior<View>? =
        AppBarLayout.ScrollingViewBehavior()

    /**
     * ?????????head????????????Behavior
     */
    fun getHeadLayoutBehavior(): AppBarLayout.Behavior? = null

    /**
     * ?????????AppBarLayout.LayoutParams??????
     */
    fun initAppBarLayoutLayoutParams(layoutParams: AppBarLayout.LayoutParams) {
        layoutParams.scrollFlags =
            AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED
    }

    /**
     * ?????????CollapsingToolbarLayout.LayoutParams??????
     */
    fun initCollapsingToolbarLayoutLayoutParams(layoutParams: CollapsingToolbarLayout.LayoutParams) {
        layoutParams.collapseMode = CollapsingToolbarLayout.LayoutParams.COLLAPSE_MODE_OFF
    }

    /**
     * ??????Head
     */
    fun getHeadLayoutView(): View

    /**
     * ??????body
     * ??????RecyclerViewPlugin??????????????????????????????
     */
    fun getBodyLayoutView(): View?
}

/**
 * ????????????????????????Tab????????????
 * RadioGroup + FrameLayout??????
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
     * ???????????????Fragment???MutableLiveData?????????
     */
    fun getTargetTabFragment(): MutableLiveData<Class<out Fragment>>

    /**
     * ???????????????fragment
     */
    fun bindFragment(map: HashBiMap<Int, Class<out Fragment>>)

    /**
     * ??????Fragment?????????????????????
     */
    fun bindArguments(id: Int, fragment: Fragment) {}
}

/**
 * ????????????????????????Indicator????????????
 * MagicIndicator + ViewPager??????
 * T: ????????????
 */
interface IndicatorPlugin<T> : ViewPager.OnPageChangeListener {
    /**
     * ??????fragments
     */
    fun getIndicatorFragments(): ArrayList<Fragment>

    /**
     * ????????????
     */
    fun getIndicatorTitles(): MutableLiveData<ArrayList<T>>

    /**
     * ?????????????????????
     */
    fun getPagerTitleView(context: Context, index: Int, title: T): CommonPagerTitleView

    /**
     * ?????????
     */
    fun getMagicIndicator(): MagicIndicator

    /**
     * ViewPager
     */
    fun getViewPager(): ViewPager

    /**
     * ViewPager??????????????????
     */
    fun getPageSmoothScroll() = false

    /**
     * ?????????????????????
     */
    fun getCurrentPosition(): MutableLiveData<Int>

    /**
     * ???????????????
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
 * ???????????????
 */
interface WebViewPlugin {
    /**
     * ???????????????????????????
     * ???".apk"
     */
    val downloadType: Array<String>?

    /**
     * ??????????????????
     * ??????null????????????
     */
    val multipleWindow: ((url: String) -> Unit)?

    fun getWebView(): WebView

    /**
     * ????????????????????????
     */
    fun goBackEnd()

    /**
     * ???????????????????????????
     */
    fun onReceivedTitle(title: String?)

    /**
     *  ??????JS???????????????
     */
    fun getJavascriptInterface(): HashMap<String, Any>?

    /**
     * ???????????????????????????
     */
    fun onProgressChanged(@IntRange(from = 0, to = 100) newProgress: Int)

    /**
     * ??????????????????
     */
    fun onPageStarted(url: String?)

    /**
     * ??????????????????
     */
    fun onPageFinished(url: String?)

    /**
     * ??????????????????
     */
    fun onReceivedError(errorCode: Int)

    /**
     * ????????????????????????
     */
    fun onReceivedDownload(url: String)

    /**
     * ??????????????????
     */
    fun reload() {
        getWebView().reload()
    }

    /**
     * ????????????
     * @return ????????????????????????
     */
    fun goBack(): Boolean {
        if (getWebView().canGoBack()) {
            getWebView().goBack()
            return true
        }
        return false
    }

    /**
     * ????????????
     * @return ????????????????????????
     */
    fun goForward(): Boolean {
        if (getWebView().canGoForward()) {
            getWebView().goForward()
            return true
        }
        return false
    }

    /**
     * ????????????
     * @return ??????????????????
     */
    fun goBackOrForward(steps: Int): Boolean {
        if (getWebView().canGoBackOrForward(steps)) {
            getWebView().goBackOrForward(steps)
            return true
        }
        return false
    }

    /**
     * ????????????
     */
    fun onShowFileChooser(
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: WebChromeClient.FileChooserParams?
    ): Boolean

    /**
     * ??????WebView
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
            //????????????
            it.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                databaseEnabled = true
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
                    if (title?.contains("??????????????????", true) == true ||
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
                    if (newProgress == 100) {
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
                    //??????HTTPS_SSL?????????????????????
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
                 * ????????????????????????????????????
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
                    if (view == null) {
                        return false
                    }
                    return if (requestUrl.startsWith("http://", true) || requestUrl.startsWith(
                            "https://",
                            true
                        )
                    ) {
                        if (checkIsFile(requestUrl)) {
                            //???????????????APK??????
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
                        //?????????url????????????????????????
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
                 * ??????????????????
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
                        else -> {}
                    }
                }
            })

        }
    }
}
