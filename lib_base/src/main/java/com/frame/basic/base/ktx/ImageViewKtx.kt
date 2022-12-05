package com.frame.basic.base.ktx

import android.app.Activity
import android.widget.ImageView
import com.bumptech.glide.Glide
import dagger.hilt.android.internal.managers.ViewComponentManager

fun ImageView.loadImage(path: String?) {
    val context = this.context ?: return
    when (context) {
        is Activity -> {
            if (context.isDestroyed || context.isFinishing) {
                return
            }
        }
        is ViewComponentManager.FragmentContextWrapper -> {
            val ctx = context.baseContext ?: return
            if (ctx is Activity && (ctx.isDestroyed || ctx.isFinishing)) {
                return
            }
        }
    }
    Glide.with(context).load(path).into(this)
}
