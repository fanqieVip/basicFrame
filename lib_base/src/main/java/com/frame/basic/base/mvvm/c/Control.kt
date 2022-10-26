package com.frame.basic.base.mvvm.c

import android.app.Activity
import android.os.Bundle
import android.os.Parcelable
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import com.frame.basic.base.ktx.functionExtras
import com.frame.basic.base.ktx.getFunctionExtraKey
import java.io.Serializable

/**
 * 界面状态
 */
enum class UIStatus {
    LOADING,
    SUCCESS,
    EMPTY,
    ERROR
}

/**
 * 界面状态描述
 */
class UIStatusInfo(val uiStatus: UIStatus, val error: Int = 0, val msg: String? = "") : Serializable

/**
 * 界面显示状态
 */
enum class DisplayStatus {
    PAUSING,
    SHOWING,
    HIDDEN
}

/**
 * 等待框状态
 */
class PopLoadingStatus(val isShow: Boolean, val msg: String = "") : Serializable

/**
 * RefreshLayout状态
 */
enum class RefreshLayoutStatus {
    REFRESH,
    LOAD_MORE,
    NO_MORE,
    STOP
}

/**
 * VM控制规范
 */
interface VMControl {
    /**
     * 用于在页面创建时进行请求接口
     * 1.界面创建后会立即调用
     * 2.当应用异常重启、切换横竖屏、低内存等导致的重启将不会再调用
     * 3.当与PagingControl搭配使用时，用户下拉会调用
     * 4.受autoOnRefresh控制
     * 5.调用的同时会显示loading加载中页面
     */
    fun onRefresh(owner: LifecycleOwner)

    /**
     * 进入界面是否立即调用onRefresh
     */
    fun autoOnRefresh(): Boolean = true

    /**
     * 进入界面后无论如何都会调用，即当应用异常重启、切换横竖屏、低内存等导致的重启后都会调用
     * 1.适合执行用户不可见的任务
     * 2.在onRefresh后执行
     */
    fun executeForever(owner: LifecycleOwner) {}

    /**
     * 加载成功
     * @param empty 数据是否为空
     */
    fun loadSuccess(empty: Boolean = false)

    /**
     * 加载失败
     */
    fun loadError(error: Int, msg: String?)

    /**
     * 加载中
     */
    fun loading()

    /**
     * 显示弹出等待框
     */
    fun showPopLoading(text: String = "")

    /**
     * 关闭弹出等待框
     */
    fun dismissPopLoading()
}

/**
 * UI执行流程规范
 */
interface UIControl<VB : ViewBinding> {

    /**
     * 初始化View
     */
    fun VB.initView()

    /**
     * 绑定事件
     */
    fun VB.initListener()

    /**
     * 显示弹出等待框
     */
    fun showPopLoading(text: String)

    /**
     * 关闭弹出等待框
     */
    fun dismissPopLoading()

    /**
     * 状态栏文字颜色黑色模式
     * true: 黑色  false:白色  null：不处理
     */
    fun statusBarDarkFont(): Boolean? = null

    /**
     * 导航栏图标颜色黑色模式
     * true: 黑色  false:白色  null：不处理
     */
    fun navigationBarDarkIcon(): Boolean? = null

    /**
     * 导航栏背景颜色
     */
    @ColorInt
    fun navigationBarColor(): Int? = null
}

/**
 * 分页控制规范
 */
interface PagingControl {
    /**
     * 首页页码
     */
    fun getFirstPageNo(): Int = 0

    /**
     *  记载分页
     *  @param pageNo 用于请求的页码
     */
    fun onLoadMore(owner: LifecycleOwner, pageNo: Int)
}

/**
 * 重建守护控制器
 */
interface RecreateControl {
    /**
     * 是否重建中
     */
    fun isRecreating(): Boolean

    /**
     * 是否已重建
     */
    fun isRecreated(): Boolean

    /**
     * 返回根View
     */
    fun getLastRootView(): ViewGroup
}

/**
 * fragment传参控制
 */
@Suppress("UNCHECKED_CAST")
interface ArgumentsControl<F : Fragment> {
    fun putExtra(key: String, value: Int): F {
        this as F
        getExtras().putInt(key, value)
        return this
    }

    fun putExtra(key: String, value: String): F {
        this as F
        getExtras().putString(key, value)
        return this
    }

    fun putExtra(key: String, value: Byte): F {
        this as F
        getExtras().putByte(key, value)
        return this
    }

    fun putExtra(key: String, value: Char): F {
        this as F
        getExtras().putChar(key, value)
        return this
    }

    fun putExtra(key: String, value: CharArray): F {
        this as F
        getExtras().putCharArray(key, value)
        return this
    }

    fun putExtra(key: String, value: Short): F {
        this as F
        getExtras().putShort(key, value)
        return this
    }

    fun putExtra(key: String, value: ShortArray): F {
        this as F
        getExtras().putShortArray(key, value)
        return this
    }

    fun putExtra(key: String, value: Boolean): F {
        this as F
        getExtras().putBoolean(key, value)
        return this
    }

    fun putExtra(key: String, value: Long): F {
        this as F
        getExtras().putLong(key, value)
        return this
    }

    fun putExtra(key: String, value: LongArray): F {
        this as F
        getExtras().putLongArray(key, value)
        return this
    }

    fun putExtra(key: String, value: IntArray): F {
        this as F
        getExtras().putIntArray(key, value)
        return this
    }

    fun putExtra(key: String, value: BooleanArray): F {
        this as F
        getExtras().putBooleanArray(key, value)
        return this
    }

    fun putExtra(key: String, value: Double): F {
        this as F
        getExtras().putDouble(key, value)
        return this
    }

    fun putExtra(key: String, value: DoubleArray): F {
        this as F
        getExtras().putDoubleArray(key, value)
        return this
    }

    fun putExtra(key: String, value: Array<String>): F {
        this as F
        getExtras().putStringArray(key, value)
        return this
    }

    fun putExtra(key: String, value: Float): F {
        this as F
        getExtras().putFloat(key, value)
        return this
    }

    fun putExtra(key: String, value: FloatArray): F {
        this as F
        getExtras().putFloatArray(key, value)
        return this
    }

    fun putExtra(key: String, value: ByteArray): F {
        this as F
        getExtras().putByteArray(key, value)
        return this
    }

    fun putExtra(key: String, value: CharSequence): F {
        this as F
        getExtras().putCharSequence(key, value)
        return this
    }

    fun putExtra(key: String, value: Array<Parcelable>): F {
        this as F
        getExtras().putParcelableArray(key, value)
        return this
    }

    fun putParcelableListExtra(key: String, value: ArrayList<Parcelable>): F {
        this as F
        getExtras().putParcelableArrayList(key, value)
        return this
    }

    fun putIntListExtra(key: String, value: ArrayList<Int>): F {
        this as F
        getExtras().putIntegerArrayList(key, value)
        return this
    }

    fun putStringListExtra(key: String, value: ArrayList<String>): F {
        this as F
        getExtras().putStringArrayList(key, value)
        return this
    }

    fun putCharSequenceListExtra(key: String, value: ArrayList<CharSequence>): F {
        this as F
        getExtras().putCharSequenceArrayList(key, value)
        return this
    }

    fun putExtra(key: String, value: Serializable): F {
        this as F
        getExtras().putSerializable(key, value)
        return this
    }

    fun putExtra(key: String, value: Parcelable): F {
        this as F
        getExtras().putParcelable(key, value)
        return this
    }

    /**
     * 仿Eventbus回调方式，无序列化问题，可直接访问当前对象
     * 注意：1.界面发生旋转重建后，界面对象已变化，会造成回调无效的问题，所以尽量只访问viewModel
     *       2.app异常重启恢复时，由于回调存在内存，会造成丢失
     */
    fun putExtra(owner: LifecycleOwner, key: String, value: Function<*>): F {
        this as F
        val realKey = getFunctionExtraKey(key, value)
        getExtras().putString(key, realKey)
        functionExtras[realKey] = value
        owner.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> {
                        if ((source is Activity && source.isFinishing) || (source is Fragment && source.activity != null && source.requireActivity().isFinishing)) {
                            functionExtras.remove(realKey)
                        }
                    }
                    Lifecycle.Event.ON_DESTROY -> {
                        functionExtras.remove(realKey)
                    }
                    else -> {}
                }
            }
        })
        return this
    }

    private fun F.getExtras(): Bundle {
        if (arguments == null) {
            arguments = Bundle()
        }
        return arguments!!
    }
}