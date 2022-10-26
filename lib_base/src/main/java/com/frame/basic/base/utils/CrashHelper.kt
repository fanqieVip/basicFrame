package com.frame.basic.base.utils

import android.os.Build
import android.os.DeadSystemException
import android.os.Handler
import android.os.Looper
import android.system.ErrnoException
import java.util.concurrent.TimeoutException

object CrashHelper {
    /**
     * 执行异常过滤
     */
    internal fun filterException() {
        val defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        //过滤子线程异常
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            //过滤cpu休眠诱发的虚拟机gc回收超时异常
            if (!(thread.name == "FinalizerWatchdogDaemon" && throwable is TimeoutException)) {
                defaultUncaughtExceptionHandler?.uncaughtException(thread, throwable)
            }
        }
        //过滤UI线程异常
        Handler(Looper.getMainLooper()).post {
            while (true) {
                try {
                    Looper.loop()
                } catch (e: Exception) {
                    val exceptionInfo = e.toString()
                    if (exceptionInfo.contains("RemoteServiceException")) {
                        return@post
                    } else if (exceptionInfo.contains("ForegroundServiceDidNotStartInTimeException")) {
                        return@post
                    } else if (exceptionInfo.contains("DeadSystemException") || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && e is DeadSystemException)) {
                        return@post
                    } else if (e is IllegalArgumentException && exceptionInfo.contains("reportSizeConfigurations")) {
                        return@post
                    } else if (e is ErrnoException) {
                        return@post
                    } else {
                        throw e
                    }
                }
            }
        }
    }

}