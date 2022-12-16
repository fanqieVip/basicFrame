package com.frame.basic.base.ktx

import android.app.Activity
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import dagger.hilt.android.internal.managers.ViewComponentManager

fun ImageView.loadImage(path: String?) {
    setNormalImage(path)
}
private val fragmentContextWrapperFragmentField by lazy {
    ViewComponentManager.FragmentContextWrapper::class.java.getDeclaredField("fragment").apply {
        isAccessible = true
    }
}

fun ImageView.setNormalImage(
    uri: Any?,
    isCenterCrop: Boolean = false,
    errorResId: Int = 0,
    placeholderResId: Int = 0,
    onLoadEnd: ((Boolean, Drawable?) -> Unit)? = null
) {
    try {
        val context = this.context ?: return
        //构造传入的activity和fragment非常关键，它将自动绑定activity和fragment生命周期，特别是fragment可以根据显示/隐藏来自动暂停加载和继续加载
        val requestManager: RequestManager
        when (context) {
            is Activity -> {
                if (context.isDestroyed || context.isFinishing) {
                    return
                }
                requestManager = Glide.with(context)
            }
            is ViewComponentManager.FragmentContextWrapper -> {
                val ctx = context.baseContext ?: return
                if (ctx is Activity && (ctx.isDestroyed || ctx.isFinishing)) {
                    return
                }
                val fragment = fragmentContextWrapperFragmentField.get(context) ?: return
                requestManager = Glide.with(fragment as Fragment)
            }
            else -> {
                requestManager = Glide.with(context)
            }
        }
        var options = RequestOptions().placeholder(placeholderResId).error(errorResId)
        if (isCenterCrop) {
            options = options.centerCrop()
        }
        val glide = requestManager.load(uri).apply(options)
        glide.listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: com.bumptech.glide.request.target.Target<Drawable>?,
                isFirstResource: Boolean
            ): Boolean {
                onLoadEnd?.invoke(false, null)
                return false
            }

            override fun onResourceReady(
                resource: Drawable?,
                model: Any?,
                target: com.bumptech.glide.request.target.Target<Drawable>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                onLoadEnd?.invoke(true, resource)
                return false
            }
        }).into(this)
    } catch (e: Exception) {
        onLoadEnd?.invoke(false, null)
    }
}

/**
 * 加载webp格式的gif
 * */
fun ImageView.loadWebpGif(url: Any?, onLoadEnd: ((Boolean, Drawable?) -> Unit)? = null) {
    val context = this.context
    if (context is Activity) {
        if (context.isDestroyed) {
            return
        }
    }
    try {
        this.run {
            val glide = Glide.with(context).load(url)
            val circleCrop = CenterCrop()
            glide.load(url)
                .apply(
                    RequestOptions()
                        .placeholder(0)
                        .error(0)
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                )
                .optionalTransform(circleCrop)
                .optionalTransform(WebpDrawable::class.java, WebpDrawableTransformation(circleCrop))
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        onLoadEnd?.invoke(false, null)
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        onLoadEnd?.invoke(true, resource)
                        return false
                    }

                })
                .into(this)
        }
    } catch (e: Exception) {
        onLoadEnd?.invoke(false, null)
    }
}
