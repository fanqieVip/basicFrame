package com.frame.basic.base.utils

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Process
import android.text.TextUtils
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader

/**
 * 进程工具类
 */
object ProcessUtils {
    //判断当前运行进程是否是主进程
    fun isMainProcess(app: Application): Boolean {
        //优先通过反射取进程名
        var processName = getProcessNameByReflection(app)
        if (TextUtils.isEmpty(processName)) {
            processName = getProcessNameByCmdline()
        }
        if (TextUtils.isEmpty(processName)) {
            //万不得已采用该方法，可能上架被拒（可能会被判定读取设备安装应用列表）
            processName = getProcessNameByAM(app)
        }
        return app.packageName.equals(processName)
    }

    private fun getProcessNameByAM(context: Context): String? {
        val pid = Process.myPid()
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        var processName: String? = null
        if (manager != null) {
            val infoList: List<ActivityManager.RunningAppProcessInfo>? = manager.runningAppProcesses
            if (infoList != null) {
                for (info in infoList) {
                    if (info.pid == pid) {
                        processName = info.processName
                        break
                    }
                }
            }
        }
        return processName
    }

    private fun getProcessNameByCmdline(): String? {
        var processName: String? = null
        var cmdlineReader: BufferedReader? = null
        try {
            cmdlineReader = BufferedReader(
                InputStreamReader(
                    FileInputStream("/proc/" + Process.myPid() + "/cmdline"),
                    "iso-8859-1"
                )
            )
            var c: Int
            val sb = StringBuilder()
            while (cmdlineReader.read().also { c = it } > 0) {
                sb.append(c.toChar())
            }
            processName = sb.toString()
        } catch (e: Exception) {
        } finally {
            if (cmdlineReader != null) {
                try {
                    cmdlineReader.close()
                } catch (ignored: IOException) {
                }
            }
        }
        return processName
    }

    private fun getProcessNameByReflection(app: Application): String? {
        var processName: String? = null
        try {
            val loadedApkField = app.javaClass.getField("mLoadedApk")
            loadedApkField.isAccessible = true
            val loadedApk: Any = loadedApkField.get(app)
            val activityThreadField = loadedApk.javaClass.getDeclaredField("mActivityThread")
            activityThreadField.isAccessible = true
            val activityThread: Any = activityThreadField.get(loadedApk)
            val getProcessName = activityThread.javaClass.getDeclaredMethod("getProcessName")
            processName = getProcessName.invoke(activityThread) as String
        } catch (e: Exception) {
        }
        return processName
    }

}
