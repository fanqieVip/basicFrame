package com.frame.basic.base.ktx

import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.listener.OnItemChildClickListener
import com.chad.library.adapter.base.listener.OnItemClickListener

/**
 * @Description:    安全的列表点击，避免暴力点击
 * @Author:         fanj
 * @CreateDate:     2022/12/26 10:02
 * @Version:
 */
class OnItemChildClickBlockListener(
    private val delay: Long = 500L,
    private val onItemClick: (view: View, position: Int) -> Unit
) :
    OnItemChildClickListener {
    var enableTime = System.currentTimeMillis()
    override fun onItemChildClick(adapter: BaseQuickAdapter<*, *>, view: View, position: Int) {
        if (enableTime + delay > System.currentTimeMillis()) {
            return
        }
        enableTime = System.currentTimeMillis()
        onItemClick(view, position)
    }
}

/**
 * @Description:    安全的列表点击，避免暴力点击
 * @Author:         fanj
 * @CreateDate:     2022/12/26 10:02
 * @Version:
 */
class OnItemClickBlockListener(
    private val delay: Long = 500L,
    private val onItemClick: (view: View, position: Int) -> Unit
) :
    OnItemClickListener {
    var enableTime = System.currentTimeMillis()
    override fun onItemClick(adapter: BaseQuickAdapter<*, *>, view: View, position: Int) {
        if (enableTime + delay > System.currentTimeMillis()) {
            return
        }
        enableTime = System.currentTimeMillis()
        onItemClick(view, position)
    }
}
