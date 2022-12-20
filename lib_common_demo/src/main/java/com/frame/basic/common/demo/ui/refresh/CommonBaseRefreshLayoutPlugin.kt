package com.frame.basic.common.demo.ui.refresh

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import com.scwang.smart.drawable.ProgressDrawable
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import com.scwang.smart.refresh.layout.api.RefreshFooter
import com.scwang.smart.refresh.layout.api.RefreshHeader
import com.scwang.smart.refresh.layout.api.RefreshKernel
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.constant.RefreshState
import com.scwang.smart.refresh.layout.constant.SpinnerStyle
import com.frame.basic.base.ktx.dp
import com.frame.basic.base.mvvm.c.RefreshLayoutPlugin

interface CommonBaseRefreshLayoutPlugin : RefreshLayoutPlugin {
    override fun initSmartRefreshLayout(refreshLayout: SmartRefreshLayout) {
        refreshLayout.setRefreshHeader(CommonBaseRefreshHeader(refreshLayout.context))
        refreshLayout.setRefreshFooter(CommonBaseRefreshFooter(refreshLayout.context))
        refreshLayout.setEnableLoadMore(enableLoadMore())
        refreshLayout.setEnableRefresh(enableRefresh())
        refreshLayout.setEnableAutoLoadMore(enableAutoLoadMore())
    }
}

class CommonBaseRefreshHeader: LinearLayout, RefreshHeader {

    constructor(context: Context): super(context)
    constructor(context: Context, attributeSet: AttributeSet): super(context, attributeSet)
    private val mHeaderText = AppCompatTextView(context)
    private val mArrowView = AppCompatTextView(context)
    private val mProgressView = AppCompatImageView(context)
    private val mProgressDrawable = ProgressDrawable()
    init {
        gravity = Gravity.CENTER
        mProgressView.setImageDrawable(mProgressDrawable)
        addView(mProgressView, 20.dp, 20.dp)
        addView(mArrowView, 20.dp, 20.dp)
        addView(View(context), 20.dp, 20.dp)
        addView(mHeaderText, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        minimumHeight = 60.dp
    }
    @SuppressLint("RestrictedApi")
    override fun onStateChanged(
        refreshLayout: RefreshLayout,
        oldState: RefreshState,
        newState: RefreshState
    ) {
        when(newState){
            RefreshState.PullDownToRefresh -> {
                mHeaderText.text = "下拉开始刷新"
                mArrowView.isVisible = true
                mProgressView.isVisible = false
                mArrowView.animate().rotation(0f)
            }
            RefreshState.Refreshing -> {
                mHeaderText.text = "正在刷新"
                mArrowView.isVisible = false
                mProgressView.isVisible = true
            }
            RefreshState.ReleaseToRefresh -> {
                mHeaderText.text = "释放立即刷新"
                mArrowView.animate().rotation(180f)
            }
            else -> {}
        }
    }

    override fun getView() = this

    override fun getSpinnerStyle(): SpinnerStyle = SpinnerStyle.Translate

    @SuppressLint("RestrictedApi")
    override fun setPrimaryColors(vararg colors: Int) {
    }

    @SuppressLint("RestrictedApi")
    override fun onInitialized(kernel: RefreshKernel, height: Int, maxDragHeight: Int) {
    }

    @SuppressLint("RestrictedApi")
    override fun onMoving(
        isDragging: Boolean,
        percent: Float,
        offset: Int,
        height: Int,
        maxDragHeight: Int
    ) {
    }

    @SuppressLint("RestrictedApi")
    override fun onReleased(refreshLayout: RefreshLayout, height: Int, maxDragHeight: Int) {
    }

    @SuppressLint("RestrictedApi")
    override fun onStartAnimator(refreshLayout: RefreshLayout, height: Int, maxDragHeight: Int) {
        mProgressDrawable.start()
    }

    @SuppressLint("RestrictedApi")
    override fun onFinish(refreshLayout: RefreshLayout, success: Boolean): Int {
        mProgressDrawable.stop()
        mHeaderText.text = if (success){
            "刷新完成"
        }else{
            "刷新失败"
        }
        return 500
    }

    @SuppressLint("RestrictedApi")
    override fun onHorizontalDrag(percentX: Float, offsetX: Int, offsetMax: Int) {
    }

    override fun isSupportHorizontalDrag() = false
}
class CommonBaseRefreshFooter: LinearLayout, RefreshFooter {

    constructor(context: Context): super(context)
    constructor(context: Context, attributeSet: AttributeSet): super(context, attributeSet)
    private val mFooterText = AppCompatTextView(context)
    private val mArrowView = AppCompatTextView(context)
    private val mProgressView = AppCompatImageView(context)
    private val mProgressDrawable = ProgressDrawable()
    private var mNoMoreData = false
    init {
        gravity = Gravity.CENTER
        mProgressView.setImageDrawable(mProgressDrawable)
        addView(mProgressView, 20.dp, 20.dp)
        addView(mArrowView, 20.dp, 20.dp)
        addView(View(context), 20.dp, 20.dp)
        addView(mFooterText, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        minimumHeight = 60.dp
    }
    @SuppressLint("RestrictedApi")
    override fun onStateChanged(
        refreshLayout: RefreshLayout,
        oldState: RefreshState,
        newState: RefreshState
    ) {
        if (!mNoMoreData) {
            when(newState){
                RefreshState.PullUpToLoad -> {
                    mFooterText.text = "上拉加载更多"
                    mArrowView.isVisible = true
                    mProgressView.isVisible = false
                    mArrowView.animate().rotation(0f)
                }
                RefreshState.Loading -> {
                    mFooterText.text = "正在加载"
                    mArrowView.isVisible = false
                    mProgressView.isVisible = true
                }
                RefreshState.ReleaseToLoad -> {
                    mFooterText.text = "释放立即加载"
                    mArrowView.animate().rotation(180f)
                }
                else -> {}
            }
        }else{
            mFooterText.text = "没有更多数据了"
            mArrowView.isVisible = false
            mProgressView.isVisible = false
        }
    }

    override fun getView() = this

    override fun getSpinnerStyle(): SpinnerStyle = SpinnerStyle.Translate

    @SuppressLint("RestrictedApi")
    override fun setPrimaryColors(vararg colors: Int) {
    }

    @SuppressLint("RestrictedApi")
    override fun onInitialized(kernel: RefreshKernel, height: Int, maxDragHeight: Int) {
    }

    @SuppressLint("RestrictedApi")
    override fun onMoving(
        isDragging: Boolean,
        percent: Float,
        offset: Int,
        height: Int,
        maxDragHeight: Int
    ) {
    }

    @SuppressLint("RestrictedApi")
    override fun onReleased(refreshLayout: RefreshLayout, height: Int, maxDragHeight: Int) {
    }

    @SuppressLint("RestrictedApi")
    override fun onStartAnimator(refreshLayout: RefreshLayout, height: Int, maxDragHeight: Int) {
        mProgressDrawable.start()
    }

    @SuppressLint("RestrictedApi")
    override fun onFinish(refreshLayout: RefreshLayout, success: Boolean): Int {
        mProgressDrawable.stop()
        mFooterText.text = if (success){
                "加载完成"
            }else{
                "加载失败"
            }
        return 500
    }

    @SuppressLint("RestrictedApi")
    override fun onHorizontalDrag(percentX: Float, offsetX: Int, offsetMax: Int) {
    }

    override fun isSupportHorizontalDrag() = false
    @SuppressLint("RestrictedApi")
    override fun setNoMoreData(noMoreData: Boolean): Boolean {
        this.mNoMoreData = noMoreData
        return true
    }
}
