package com.frame.basic.buildsrc

/**
 * 项目依赖版本统一管理
 *
 * @author Qu Yunshuo
 * @since 4/24/21 4:00 PM
 */
object DependencyConfig {

    /**
     * 依赖版本号
     *
     * @author Qu Yunshuo
     * @since 4/24/21 4:01 PM
     */
    object Version {

        // AndroidX--------------------------------------------------------------
        const val AppCompat = "1.2.0"
        const val CoreKtx = "1.3.1"
        const val ConstraintLayout = "2.0.1"                // 约束布局
        const val TestExtJunit = "1.1.2"
        const val TestEspresso = "3.3.0"
        const val ActivityKtx = "1.1.0"
        const val FragmentKtx = "1.2.5"
        const val MultiDex = "2.0.1"

        // Android---------------------------------------------------------------
        const val Junit = "4.13"
        const val Material = "1.2.0"                        // 材料设计UI套件

        // Kotlin----------------------------------------------------------------
        const val Kotlin = "1.5.10"
        const val Coroutines = "1.5.0"                      // 协程

        // JetPack---------------------------------------------------------------
        const val Lifecycle = "2.3.1"                       // Lifecycle相关（ViewModel & LiveData & Lifecycle）
        const val Room = "2.2.5"                            // Room 数据库
        const val Hilt = "2.35.1"                           // DI框架-Hilt
        const val HiltAndroidx = "1.0.0"
        const val DataBinding = "4.2.1"                     //databind相关

        // GitHub----------------------------------------------------------------
        const val OkHttp = "4.9.0"                          // OkHttp
        const val OkHttpInterceptorLogging = "4.9.1"        // OkHttp 请求Log拦截器
        const val Retrofit = "2.9.0"                        // Retrofit
        const val RetrofitConverterGson = "2.9.0"           // Retrofit Gson 转换器
        const val Gson = "2.8.7"                            // Gson
        const val MMKV = "1.2.9"                            // 腾讯 MMKV 替代SP
        const val AutoSize = "1.2.1"                        // 屏幕适配
        const val RecyclerViewAdapter = "3.0.4"             // RecyclerViewAdapter
        const val StatusBar = "1.5.1"                       // 状态栏
        const val XXPermission = "13.5"                     // 权限申请
        const val LeakCanary = "2.9.1"                        // 检测内存泄漏
        const val AutoService = "1.0"                       // 自动生成SPI暴露服务文件
        const val BlankJ = "1.29.0"                         // BlankJ工具包
        const val SmartRefreshLayoutKernel = "2.0.3"        // SmartRefreshLayout核心
        const val SmartRefreshLayoutHeaderClassics = "2.0.3"// SmartRefreshLayout经典下拉刷新样式
        const val SmartRefreshLayoutFooterClassics = "2.0.3"// SmartRefreshLayout经典上拉加载样式
        const val ReadyStateSoftware = "1.1.0"              // ReadyStateSoftware启用网络日志
        const val ReadyStateSoftwareNoOp = "1.1.0"          // ReadyStateSoftware不启用网络日志
        const val MagicIndicator = "1.7.0"                  // MagicIndicator分页控件
        const val ShapeView = "8.2"                         // ShapeView圆角阴影框架 https://github.com/getActivity/ShapeView
        const val GlideWebpDecoder = "2.0.4.11.0"           // Glide加载webp格式得gif
        const val Glide = "4.12.0"                          // Glide图片加载框架
        const val GlideProcessor = "4.11.0"                 // GlideProcessor
        const val GlideIntegration = "4.11.0"               // GlideIntegration
        const val GlideGifDecoder = "4.1.0"                 // Glide加载Gif
        const val Lottie = "3.7.2"                          // Lottie动画框架
        const val UnPeekLiveData = "7.2.0-beta1"            // UnPeekLiveData粘性LiveData解决方案 https://toscode.gitee.com/superbro/UnPeek-LiveData
    }

    /**
     * AndroidX相关依赖
     *
     * @author Qu Yunshuo
     * @since 4/24/21 4:01 PM
     */
    object AndroidX {
        const val AndroidJUnitRunner = "androidx.test.runner.AndroidJUnitRunner"
        const val AppCompat = "androidx.appcompat:appcompat:${Version.AppCompat}"
        const val CoreKtx = "androidx.core:core-ktx:${Version.CoreKtx}"
        const val ConstraintLayout =
            "androidx.constraintlayout:constraintlayout:${Version.ConstraintLayout}"
        const val TestExtJunit = "androidx.test.ext:junit:${Version.TestExtJunit}"
        const val TestEspresso = "androidx.test.espresso:espresso-core:${Version.TestEspresso}"
        const val ActivityKtx = "androidx.activity:activity-ktx:${Version.ActivityKtx}"
        const val FragmentKtx = "androidx.fragment:fragment-ktx:${Version.FragmentKtx}"
        const val MultiDex = "androidx.multidex:multidex:${Version.MultiDex}"
    }

    /**
     * Android相关依赖
     *
     * @author Qu Yunshuo
     * @since 4/24/21 4:02 PM
     */
    object Android {
        const val Junit = "junit:junit:${Version.Junit}"
        const val Material = "com.google.android.material:material:${Version.Material}"
    }

    /**
     * JetPack相关依赖
     *
     * @author Qu Yunshuo
     * @since 4/24/21 4:02 PM
     */
    object JetPack {
        const val ViewModel = "androidx.lifecycle:lifecycle-viewmodel-ktx:${Version.Lifecycle}"
        const val ViewModelSavedState =
            "androidx.lifecycle:lifecycle-viewmodel-savedstate:${Version.Lifecycle}"
        const val LiveData = "androidx.lifecycle:lifecycle-livedata-ktx:${Version.Lifecycle}"
        const val Lifecycle = "androidx.lifecycle:lifecycle-runtime-ktx:${Version.Lifecycle}"
        const val LifecycleProcess = "androidx.lifecycle:lifecycle-process:${Version.Lifecycle}"
        const val LifecycleCompilerAPT =
            "androidx.lifecycle:lifecycle-compiler:${Version.Lifecycle}"
        const val Room = "androidx.room:room-runtime:${Version.Room}"
        const val RoomApt = "androidx.room:room-compiler:${Version.Room}"
        const val RoomCoroutines = "androidx.room:room-ktx:${Version.Room}"
        const val HiltCore = "com.google.dagger:hilt-android:${Version.Hilt}"
        const val HiltApt = "com.google.dagger:hilt-compiler:${Version.Hilt}"
        const val HiltAndroidx = "androidx.hilt:hilt-compiler:${Version.HiltAndroidx}"
        const val DataBindingAdapter = "androidx.databinding:databinding-adapters:${Version.DataBinding}"
        const val DataBindingCommon = "androidx.databinding:databinding-common:${Version.DataBinding}"
        const val DataBindingCompilerCommon = "androidx.databinding:databinding-compiler-common:${Version.DataBinding}"
        const val DataBindingCompiler = "androidx.databinding:databinding-compiler:${Version.DataBinding}"
        const val DataBindingRuntime = "androidx.databinding:databinding-runtime:${Version.DataBinding}"
    }

    /**
     * Kotlin相关依赖
     *
     * @author Qu Yunshuo
     * @since 4/24/21 4:02 PM
     */
    object Kotlin {
        const val Kotlin = "org.jetbrains.kotlin:kotlin-stdlib:${Version.Kotlin}"
        const val CoroutinesCore =
            "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Version.Coroutines}"
        const val CoroutinesAndroid =
            "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Version.Coroutines}"
    }

    /**
     * GitHub及其他相关依赖
     *
     * @author Qu Yunshuo
     * @since 4/24/21 4:02 PM
     */
    object GitHub {
        const val OkHttp = "com.squareup.okhttp3:okhttp:${Version.OkHttp}"
        const val OkHttpInterceptorLogging = "com.squareup.okhttp3:logging-interceptor:${Version.OkHttpInterceptorLogging}"
        const val Retrofit = "com.squareup.retrofit2:retrofit:${Version.Retrofit}"
        const val RetrofitConverterGson = "com.squareup.retrofit2:converter-gson:${Version.RetrofitConverterGson}"
        const val Gson = "com.google.code.gson:gson:${Version.Gson}"
        const val MMKV = "com.tencent:mmkv-static:${Version.MMKV}"
        const val AutoSize = "me.jessyan:autosize:${Version.AutoSize}"
        const val RecyclerViewAdapter = "com.github.CymChad:BaseRecyclerViewAdapterHelper:${Version.RecyclerViewAdapter}"
        const val StatusBar = "com.jaeger.statusbarutil:library:${Version.StatusBar}"
        const val XXPermission = "com.github.getActivity:XXPermissions:${Version.XXPermission}"
        const val LeakCanary = "com.squareup.leakcanary:leakcanary-android:${Version.LeakCanary}"
        const val AutoService = "com.google.auto.service:auto-service:${Version.AutoService}"
        const val AutoServiceAnnotations = "com.google.auto.service:auto-service-annotations:${Version.AutoService}"
        const val BlankJ = "com.blankj:utilcodex:${Version.BlankJ}"
        const val SmartRefreshLayoutKernel = "com.scwang.smart:refresh-layout-kernel:${Version.SmartRefreshLayoutKernel}"
        const val SmartRefreshLayoutHeaderClassics = "com.scwang.smart:refresh-header-classics:${Version.SmartRefreshLayoutHeaderClassics}"
        const val SmartRefreshLayoutFooterClassics = "com.scwang.smart:refresh-footer-classics:${Version.SmartRefreshLayoutFooterClassics}"
        const val ReadyStateSoftware = "com.readystatesoftware.chuck:library:${Version.ReadyStateSoftware}"
        const val ReadyStateSoftwareNoOp = "com.readystatesoftware.chuck:library-no-op:${Version.ReadyStateSoftwareNoOp}"
        const val MagicIndicator = "com.github.hackware1993:MagicIndicator:${Version.MagicIndicator}"
        const val ShapeView = "com.github.getActivity:ShapeView:${Version.ShapeView}"
        const val GlideWebpDecoder = "com.zlc.glide:webpdecoder:${Version.GlideWebpDecoder}"
        const val Glide = "com.github.bumptech.glide:glide:${Version.Glide}"
        const val GlideProcessor = "com.github.bumptech.glide:compiler:${Version.GlideProcessor}"
        const val GlideIntegration = "com.github.bumptech.glide:okhttp3-integration:${Version.GlideIntegration}"
        const val GlideGifDecoder = "jp.wasabeef:glide-transformations:${Version.GlideGifDecoder}"
        const val Lottie = "com.airbnb.android:lottie:${Version.Lottie}"
        const val UnPeekLiveData = "com.kunminx.arch:unpeek-livedata:${Version.UnPeekLiveData}"
    }

    /**
     * SDK相关依赖
     *
     * @author Qu Yunshuo
     * @since 4/24/21 4:02 PM
     */
    object SDK {
    }
}
