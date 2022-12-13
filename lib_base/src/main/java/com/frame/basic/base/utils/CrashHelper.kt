package com.frame.basic.base.utils

import android.os.Build
import android.os.DeadSystemException
import android.os.Handler
import android.os.Looper
import android.system.ErrnoException
import java.util.concurrent.TimeoutException

/**
 * @Description:    未知异常过滤器
 * @Author:         fanj
 * @CreateDate:     2022/10/18 10:47
 * @Version:
 */
object CrashHelper {

    /**
     * 执行异常过滤
     */
    internal fun filterException() {
        //修复cpu休眠诱发的虚拟机gc回收超时异常
        fixWatchdogDaemon()
        //过滤子线程异常
        val defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
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
                    } else if (e is IllegalArgumentException && (exceptionInfo.contains("reportSizeConfigurations") || exceptionInfo.contains("not attached to a context") || exceptionInfo.contains("Cannot add the same observer with different lifecycles"))) {
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
    /**
     * 修复cpu休眠诱发的虚拟机gc回收超时异常
     */
    private fun fixWatchdogDaemon() {
        try {
            val clazz = Class.forName ("java.lang.Daemons\$FinalizerWatchdogDaemon")
            val method = clazz.superclass.getDeclaredMethod("stop")
            method.isAccessible = true
            val field = clazz.getDeclaredField ("INSTANCE")
            field.isAccessible = true
            method.invoke(field.get(null))
        } catch (e:Throwable) {
            e.printStackTrace()
        }
    }
}
