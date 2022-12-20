package com.frame.basic.common.demo.di

import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.frame.basic.base.BaseApplication
import com.frame.basic.base.BuildConfig
import com.frame.basic.base.constant.VersionStatus
import com.frame.basic.base.ktx.ProgressInterceptor
import com.frame.basic.common.demo.constant.NetBaseUrlConstant
import com.frame.basic.common.demo.net.AuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.HEADERS
import okhttp3.logging.HttpLoggingInterceptor.Level.NONE
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * 全局作用域的网络层的依赖注入模块
 * @Description:    接口公共地址
 * @Author:         fanj
 * @CreateDate:     2021/11/10 10:43
 * @Version:        1.0.2
 */
@Module
@InstallIn(SingletonComponent::class)
class DINetworkModule {

    /**
     * [OkHttpClient]依赖提供方法
     *
     * @return OkHttpClient
     */
    @Singleton
    @Provides
    fun provideOkHttpClient(): OkHttpClient {
        // 日志拦截器部分 不要用BODY,大文件上传会OOM
        val level = if (BuildConfig.VERSION_TYPE != VersionStatus.RELEASE) HEADERS else NONE
        val logInterceptor = HttpLoggingInterceptor().setLevel(level)
        // 认证令牌拦截器
        val authInterceptor = AuthInterceptor()
        // 下载拦截部分
        val progressInterceptor = ProgressInterceptor()
        return OkHttpClient.Builder()
            .connectTimeout(15L * 1000L, TimeUnit.MILLISECONDS)
            .readTimeout(20L * 1000L, TimeUnit.MILLISECONDS)
            .addInterceptor(logInterceptor)
            .addInterceptor(authInterceptor)
            .addNetworkInterceptor(progressInterceptor)
            .apply {
                //非生产环境启用Chuck日志
                if (BuildConfig.VERSION_TYPE != VersionStatus.RELEASE) {
                    addInterceptor(ChuckerInterceptor.Builder(BaseApplication.application).build())
                }
            }
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * 项目主要服务器地址的[Retrofit]依赖提供方法
     *
     * @param okHttpClient OkHttpClient OkHttp客户端
     * @return Retrofit
     */
    @Singleton
    @Provides
    fun provideMainRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(NetBaseUrlConstant.MAIN_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
    }

}
