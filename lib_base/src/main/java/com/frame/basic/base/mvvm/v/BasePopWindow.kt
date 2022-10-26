package com.frame.basic.base.mvvm.v

import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AnimationUtils
import androidx.annotation.AnimRes
import androidx.fragment.app.FragmentManager
import androidx.viewbinding.ViewBinding
import com.blankj.utilcode.util.ScreenUtils
import com.frame.basic.base.R
import com.frame.basic.base.mvvm.c.NotFoundIdException
import com.frame.basic.base.mvvm.c.RecreateControl
import com.frame.basic.base.mvvm.vm.BaseVM
import com.frame.basic.base.utils.ActivityStackManager
import com.frame.basic.base.utils.NotchUtils
import java.io.Serializable

/**
 * @Description:    基于DialogFragment实现的PopWindow
 * @Author:         fanj
 * @CreateDate:     2021/11/29 10:49
 * @Version:        1.0.2
 */
abstract class BasePopWindow<VB : ViewBinding, VM : BaseVM> : BaseDialog<VB, VM>() {
    final override fun getGravity() = Gravity.TOP or Gravity.START
    final override fun getAnimationStyle() = 0
    final override fun getDimAmount() = 0f
    final override fun isShowTitleBar() = false

    final override fun getHeight() = if (mBindingVM.popWindowLocationInfo.value != null){
        if (getPopHeight() == WindowManager.LayoutParams.MATCH_PARENT || getPopHeight() == WindowManager.LayoutParams.WRAP_CONTENT) {
            if (mBindingVM.popWindowLocationInfo.value!!.gravity == TargetGravity.TOP_LEFT || mBindingVM.popWindowLocationInfo.value!!.gravity == TargetGravity.TOP_RIGHT || mBindingVM.popWindowLocationInfo.value!!.gravity == TargetGravity.TOP) {
                mBindingVM.popWindowLocationInfo.value!!.targetViewY - mBindingVM.popWindowLocationInfo.value!!.verticalMargin
            } else {
                mBindingVM.popWindowLocationInfo.value!!.screenHeight - (mBindingVM.popWindowLocationInfo.value!!.targetViewY + mBindingVM.popWindowLocationInfo.value!!.targetViewHeight + mBindingVM.popWindowLocationInfo.value!!.verticalMargin)
            }
        } else {
            if (mBindingVM.popWindowLocationInfo.value!!.gravity == TargetGravity.TOP_LEFT || mBindingVM.popWindowLocationInfo.value!!.gravity == TargetGravity.TOP_RIGHT || mBindingVM.popWindowLocationInfo.value!!.gravity == TargetGravity.TOP) {
                if (getPopHeight() > mBindingVM.popWindowLocationInfo.value!!.targetViewY - mBindingVM.popWindowLocationInfo.value!!.verticalMargin) {
                    mBindingVM.popWindowLocationInfo.value!!.targetViewY - mBindingVM.popWindowLocationInfo.value!!.verticalMargin
                } else {
                    getPopHeight()
                }
            } else {
                if (getPopHeight() > (mBindingVM.popWindowLocationInfo.value!!.screenHeight - (mBindingVM.popWindowLocationInfo.value!!.targetViewY + mBindingVM.popWindowLocationInfo.value!!.targetViewHeight + mBindingVM.popWindowLocationInfo.value!!.verticalMargin))) {
                    mBindingVM.popWindowLocationInfo.value!!.screenHeight - (mBindingVM.popWindowLocationInfo.value!!.targetViewY + mBindingVM.popWindowLocationInfo.value!!.targetViewHeight + mBindingVM.popWindowLocationInfo.value!!.verticalMargin)
                } else {
                    getPopHeight()
                }
            }
        }
    }else{
        0
    }

    final override fun getWidth() =  if (mBindingVM.popWindowLocationInfo.value != null){
        if (fitTargetViewWidth()) {
            if (mBindingVM.popWindowLocationInfo.value!!.targetViewWidth > mBindingVM.popWindowLocationInfo.value!!.screenWidth - mBindingVM.popWindowLocationInfo.value!!.horizontalMargin) {
                mBindingVM.popWindowLocationInfo.value!!.screenWidth - mBindingVM.popWindowLocationInfo.value!!.horizontalMargin
            } else {
                mBindingVM.popWindowLocationInfo.value!!.targetViewWidth
            }
        } else {
            if (getPopWidth() == WindowManager.LayoutParams.MATCH_PARENT || getPopWidth() == WindowManager.LayoutParams.WRAP_CONTENT) {
                mBindingVM.popWindowLocationInfo.value!!.screenWidth - mBindingVM.popWindowLocationInfo.value!!.horizontalMargin
            } else {
                if (getPopWidth() > mBindingVM.popWindowLocationInfo.value!!.screenWidth - mBindingVM.popWindowLocationInfo.value!!.horizontalMargin) {
                    mBindingVM.popWindowLocationInfo.value!!.screenWidth - mBindingVM.popWindowLocationInfo.value!!.horizontalMargin
                } else {
                    getPopWidth()
                }
            }
        }
    }else{
        0
    }

    open fun getPopWidth(): Int = WindowManager.LayoutParams.WRAP_CONTENT
    open fun getPopHeight(): Int = WindowManager.LayoutParams.WRAP_CONTENT
    final override fun getTitleBar(): ViewGroup? = null
    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            dialog?.window?.decorView?.setOnApplyWindowInsetsListener { v, insets ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    insets.displayCutout?.apply {
                        if (safeInsetLeft > 0) {
                            dialog?.window?.attributes?.let {
                                it.y = popLocation.y
                                it.x = popLocation.x + safeInsetLeft
                                dialog?.window?.attributes = it
                            }
                        }
                    }
                }
                insets
            }
        }
        if (getHeight() <= 0 || getWidth() <= 0 && !isRecreated()){
            dismissDialog()
        }else{
            dialog?.window?.attributes?.let {
                it.y = popLocation.y
                it.x = popLocation.x
                dialog?.window?.attributes = it
            }
            context?.let {
                val anim = AnimationUtils.loadAnimation(it, getInAnimation())
                getLastRootView().startAnimation(anim)
            }
        }
    }

    final override fun initWindowStyleBefore() {
        //重建后恢复数据
        if (isRecreated()) {
            mBindingVM.popWindowLocationInfo.value?.let {
                it.targetViewWidth = 0
                it.targetViewHeight = 0
                it.screenWidth = getDisplayWidth()
                it.screenHeight = getDisplayHeight()
                val host = host
                if (host != null && host is RecreateControl) {
                    val targetView =
                        host.getLastRootView().findViewById<View>(it.targetViewId)
                    if (targetView == null) {
                        dismissDialog()
                    } else {
                        targetView.post {
                            it.targetViewWidth = targetView.width
                            it.targetViewHeight = targetView.height
                            val location = IntArray(2)
                            targetView.getLocationOnScreen(location)
                            it.targetViewX = support27TargetViewX(location[0])
                            it.targetViewY = support27TargetViewY(location[1])
                            initPopLocation()
                            val height = getHeight()
                            if (height <= 0) {
                                dismissDialog()
                                return@post
                            }
                            val width = getWidth()
                            if (width <= 0) {
                                dismissDialog()
                                return@post
                            }
                            dialog?.window?.attributes?.let { lp ->
                                lp.y = popLocation.y
                                lp.x = popLocation.x
                                lp.width = width
                                lp.height = height
                                dialog?.window?.attributes = lp
                            }
                        }
                    }
                }
            }
            popLocation = Location().apply {
                x = 0
                y = 0
            }
        } else {
            initPopLocation()
        }

    }

    /**
     * 返回进场动画
     * 已预置以下动画，如果没有配置则使用默认动画
     * base_dialog_slide_in_from_bottom： 底部弹出
     * base_dialog_slide_in_from_top： 顶部弹出
     */
    @AnimRes
    open fun getInAnimation(): Int = if(mBindingVM.popWindowLocationInfo.value == null){
        R.anim.base_dialog_slide_in_from_top
    }else{
        when (mBindingVM.popWindowLocationInfo.value!!.gravity) {
            TargetGravity.TOP, TargetGravity.TOP_RIGHT, TargetGravity.TOP_LEFT -> {
                R.anim.base_dialog_slide_in_from_bottom
            }
            else -> {
                R.anim.base_dialog_slide_in_from_top
            }
        }
    }


    /**
     * 高度是否根据目标View的宽度自适应
     * 如果开启，则根据targetView设置宽度，否则则以getWidth设置宽度
     */
    private fun fitTargetViewWidth() = if(mBindingVM.popWindowLocationInfo.value == null){
        true
    }else{
        mBindingVM.popWindowLocationInfo.value!!.gravity == TargetGravity.TOP || mBindingVM.popWindowLocationInfo.value!!.gravity == TargetGravity.BOTTOM
    }

    /**
     * 显示popWindow
     * @param targetView 目标View
     * @param gravity 相对于targetView的对齐方式
     * @param verticalMargin 与targetView的垂直间距
     * @param horizontalMargin 与targetView的水平间距。当左对齐时，即左边间距； 当右对齐时，即右间距； 当居中时无效
     */
    fun showAtLocation(
        fragmentManager: FragmentManager,
        targetView: View,
        gravity: TargetGravity,
        verticalMargin: Int = 0,
        horizontalMargin: Int = 0,
    ) {
        if (targetView.id <= 0) {
            throw NotFoundIdException("File:${javaClass.name}.showAtLocation(), targetView must set id")
        }
        fragmentManager.fragments.filterIsInstance<BasePopWindow<*, *>>().forEach { popWindow ->
            popWindow.dismissDialog()
        }
        val location = IntArray(2)
        targetView.getLocationOnScreen(location)
        putExtra("VMControl_popWindowLocationInfo", initLocationInfo(
            targetView.id,
            targetView.width,
            targetView.height,
            location[0],
            location[1],
            verticalMargin,
            horizontalMargin,
            gravity
        ))
        showDialog(fragmentManager)
    }

    private lateinit var popLocation: Location
    private fun initLocationInfo(
        targetViewId: Int,
        targetViewWidth: Int,
        targetViewHeight: Int,
        targetViewX: Int,
        targetViewY: Int,
        verticalMargin: Int,
        horizontalMargin: Int,
        gravity: TargetGravity
    ) = LocationInfo(
        targetViewId,
        targetViewWidth,
        targetViewHeight,
        getDisplayWidth(),
        getDisplayHeight(),
        support27TargetViewX(targetViewX),
        support27TargetViewY(targetViewY),
        verticalMargin,
        horizontalMargin,
        gravity
    )

    private fun support27TargetViewY(targetViewY: Int): Int{
        return if (Build.VERSION.SDK_INT < 28 && ActivityStackManager.getCurrentActivity() != null && NotchUtils.hasNotchInScreen(ActivityStackManager.getCurrentActivity()) && ScreenUtils.isPortrait()) {
            return targetViewY - getStatusBarHeight()
        }else{
            targetViewY
        }
    }
    private fun support27TargetViewX(targetViewX: Int): Int{
        return if (Build.VERSION.SDK_INT < 28 && ActivityStackManager.getCurrentActivity() != null && NotchUtils.hasNotchInScreen(ActivityStackManager.getCurrentActivity()) && ScreenUtils.isLandscape()) {
            return targetViewX - getStatusBarHeight()
        }else{
            targetViewX
        }
    }

    private fun getDisplayHeight() = if (getNavigationBarHeight() > 0) {
        ScreenUtils.getScreenHeight() - getNavigationBarHeight() + getStatusBarHeight()
    } else {
        ScreenUtils.getScreenHeight()
    }

    private fun getDisplayWidth() = ScreenUtils.getAppScreenWidth()

    /**
     * 获取popWindow的屏幕绝对坐标位置
     */
    private fun initPopLocation() {
        popLocation = Location()
        mBindingVM.popWindowLocationInfo.value?.let {
            when (it.gravity) {
                TargetGravity.TOP_LEFT, TargetGravity.TOP -> {
                    popLocation.y = it.targetViewY - it.verticalMargin - getHeight()
                    popLocation.x = getPopLocationLeftX()
                }
                TargetGravity.TOP_RIGHT -> {
                    popLocation.y = it.targetViewY - it.verticalMargin - getHeight()
                    popLocation.x = getPopLocationRightX()
                }
                TargetGravity.BOTTOM_LEFT, TargetGravity.BOTTOM -> {
                    popLocation.y = it.targetViewY + it.verticalMargin + it.targetViewHeight
                    popLocation.x = getPopLocationLeftX()
                }
                TargetGravity.BOTTOM_RIGHT -> {
                    popLocation.y = it.targetViewY + it.verticalMargin + it.targetViewHeight
                    popLocation.x = getPopLocationRightX()
                }
            }
        }
    }

    private fun getPopLocationLeftX(): Int {
        if (mBindingVM.popWindowLocationInfo.value == null){
            return 0
        }else{
            val maxLeftMargin = mBindingVM.popWindowLocationInfo.value!!.screenWidth - getWidth()
            return if ((mBindingVM.popWindowLocationInfo.value!!.targetViewX + mBindingVM.popWindowLocationInfo.value!!.horizontalMargin) <= maxLeftMargin) {
                mBindingVM.popWindowLocationInfo.value!!.targetViewX + mBindingVM.popWindowLocationInfo.value!!.horizontalMargin
            } else {
                //如果对齐间距实在不够，就居中对齐
                maxLeftMargin / 2
            }
        }
    }

    private fun getPopLocationRightX(): Int {
        if (mBindingVM.popWindowLocationInfo.value == null){
            return 0
        }else{
            val maxRightMargin = mBindingVM.popWindowLocationInfo.value!!.screenWidth - getWidth()
            return if ((mBindingVM.popWindowLocationInfo.value!!.screenWidth - (mBindingVM.popWindowLocationInfo.value!!.targetViewX + mBindingVM.popWindowLocationInfo.value!!.targetViewWidth - mBindingVM.popWindowLocationInfo.value!!.horizontalMargin)) <= maxRightMargin) {
                mBindingVM.popWindowLocationInfo.value!!.targetViewX + mBindingVM.popWindowLocationInfo.value!!.targetViewWidth - mBindingVM.popWindowLocationInfo.value!!.horizontalMargin - getWidth()
            } else {
                //如果对齐间距实在不够，就居中对齐
                maxRightMargin / 2
            }
        }
    }

    final override fun showDialog(fragmentManager: FragmentManager) {
        super.showDialog(fragmentManager)
    }

    final override fun showDialog(fragmentManager: FragmentManager, hasPriority: Boolean) {
        super.showDialog(fragmentManager, hasPriority)
    }

    final override fun showDialog(
        fragmentManager: FragmentManager,
        tag: String,
        hasPriority: Boolean
    ) {
        super.showDialog(fragmentManager, tag, hasPriority)
    }

    final override fun realShowDialog(fragmentManager: FragmentManager, tag: String) {
        super.realShowDialog(fragmentManager, tag)
    }
}
/**
 * 坐标位置
 */
internal class Location {
    var x = 0
    var y = 0
}

internal class LocationInfo(
    var targetViewId: Int,
    var targetViewWidth: Int,
    var targetViewHeight: Int,
    var screenWidth: Int,
    var screenHeight: Int,
    var targetViewX: Int,
    var targetViewY: Int,
    val verticalMargin: Int,
    val horizontalMargin: Int,
    val gravity: TargetGravity
): Serializable

/**
 * 对齐方式
 */
enum class TargetGravity {
    //在target上方且与target左对齐
    TOP_LEFT,

    //在target上方且与target右对齐
    TOP_RIGHT,

    //在target下方且与target左对齐
    BOTTOM_LEFT,

    //在target下方且与target右对齐
    BOTTOM_RIGHT,

    //在target上方且与宽度和target一样
    TOP,

    //在target下方且与宽度和target一样
    BOTTOM
}