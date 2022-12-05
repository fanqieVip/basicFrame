package com.frame.basic.base.ktx

import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import androidx.annotation.ColorInt
import androidx.lifecycle.LifecycleOwner
import com.blankj.utilcode.util.SpanUtils

/**
 * @Description:
 * @Author:         fanj
 * @CreateDate:     2022/12/5 11:31
 * @Version:
 */
/**
 * 安全的设置ClickSpan,避免内存泄漏
 */
fun SpanUtils.setClickSpanSafely(
    owner: LifecycleOwner,
    @ColorInt color: Int,
    underlineText: Boolean,
    listener: (() -> Unit)?
): SpanUtils {
    var safeListener: (() -> Unit)? = listener
    val clickableSpan = object : ClickableSpan() {
        override fun onClick(widget: View) {
            safeListener?.invoke()
        }

        override fun updateDrawState(paint: TextPaint) {
            super.updateDrawState(paint)
            paint.color = color
            paint.isUnderlineText = underlineText
        }
    }
    setClickSpan(clickableSpan)
    owner.lifecycle.addObserver(object : androidx.lifecycle.LifecycleEventObserver {
        override fun onStateChanged(
            source: LifecycleOwner,
            event: androidx.lifecycle.Lifecycle.Event
        ) {
            if (event == androidx.lifecycle.Lifecycle.Event.ON_DESTROY) {
                safeListener = null
            }
        }
    })
    return this
}
