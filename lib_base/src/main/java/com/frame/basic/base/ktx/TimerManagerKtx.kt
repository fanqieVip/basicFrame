package com.frame.basic.base.ktx

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.annotation.IntRange
import androidx.annotation.MainThread
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.frame.basic.base.BaseApplication
import com.frame.basic.base.R
import com.frame.basic.base.mvvm.c.keepVms
import com.frame.basic.base.mvvm.vm.DeviceStateVM
import com.frame.basic.base.utils.AppUtils
import com.frame.basic.base.utils.DateUtils
import com.frame.basic.base.utils.OSHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.*
import java.util.concurrent.CopyOnWriteArraySet

/**
 * 全局定时任务调度器
 * 1.实现常规的延迟执行任务，定期执行任务，周期执行任务
 * 2.支持协程，最优化利用线程资源
 * 3.支持生命周期绑定，可以及时自动销毁任务
 * 4.支持任务执行线程自定义
 */
class TimerManager(private val scope: CoroutineScope, keepWakeUp: Boolean) {
    private val mDeviceStateVM by keepVms<DeviceStateVM>()
    private val powerManager by lazy { BaseApplication.application.getSystemService(Context.POWER_SERVICE) as PowerManager }
    private var wakelock: PowerManager.WakeLock? = null

    companion object {
        //扫描任务间隔时间
        private const val INTERVAL = 1000L
        private var timerManager: TimerManager? = null

        /**
         * 初始化全局定时任务调度器
         * @param keepWakeUp 是否保持cpu唤醒
         */
        @Synchronized
        fun build(scope: CoroutineScope, keepWakeUp: Boolean) {
            if (timerManager == null) {
                timerManager = TimerManager(scope, keepWakeUp)
            }
        }

        /**
         * 获取当前时间
         */
        fun getNowTime() = timerManager?.getNowTime() ?: System.currentTimeMillis()

        /**
         * 添加任务
         * @param runNow 添加任务的同时，立即执行一次
         */
        @MainThread
        fun put(task: Task, runNow: Boolean = false) {
            timerManager?.put(task, runNow)
        }

        /**
         * 移除任务
         */
        fun remove(task: Task) {
            timerManager?.remove(task)
        }

        /**
         * 矫正时间
         * 用于自校验以及和服务器主动同步时间
         */
        fun rectifyTime(time: Long) {
            timerManager?.rectifyTime(time)
        }
    }

    //存储不需要持久化的待执行的任务
    private val memoryTasks by lazy { CopyOnWriteArraySet<Task>() }

    //任务池
    private val executePool by lazy { Channel<ChannelTask>() }

    //当前时间
    private var systemTime = System.currentTimeMillis()

    init {
        build()
        if (keepWakeUp) {
            keepWakeUp()
        }
    }

    private fun build() {
        scope.launch {
            launch(Dispatchers.IO) {
                while (true) {
                    delay(INTERVAL)
                    //更新系统时间
                    systemTime += INTERVAL
                    rectifyTime(systemTime)
                    //执行任务
                    launch(Dispatchers.IO) {
                        if (!memoryTasks.isNullOrEmpty()) {
                            memoryTasks.forEach {
                                executePool.send(ChannelTask(it, false))
                            }
                        }
                    }
                }
            }
            launch(Dispatchers.IO) {
                val scope = this@launch
                while (true) {
                    val channelTask = executePool.receive()
                    launch(Dispatchers.Default) {
                        try {
                            if (!channelTask.task.isCancel) {
                                //如果任务执行完就可以取消了，则自动移除
                                if (channelTask.task.execute(
                                        scope,
                                        systemTime,
                                        channelTask.runNow
                                    )
                                ) {
                                    remove(channelTask.task)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    /**
     * 矫正时间
     * 用于自校验以及和服务器主动同步时间
     */
    fun rectifyTime(time: Long) {
        val sysTime = System.currentTimeMillis()
        systemTime = if (time <= sysTime) {
            sysTime
        } else {
            time
        }
    }

    /**
     * 添加任务
     */
    fun put(task: Task, runNow: Boolean) {
        if (memoryTasks.contains(task)) {
            return
        }
        if (runNow) {
            scope.launch {
                executePool.send(ChannelTask(task, true))
            }
        }
        memoryTasks.add(task)
    }

    /**
     * 移除任务
     */
    fun remove(task: Task) {
        task.isCancel = true
        memoryTasks.remove(task)
    }

    /**
     * 获取当前时间
     */
    fun getNowTime() = systemTime

    /**
     * 保持唤醒
     */
    private fun keepWakeUp() {
        mDeviceStateVM.screenState.observeForever {
            when (it) {
                1 -> {
                    lockCpuPowerManager()
                }
                else -> {
                    unlockCpuPowerManager()
                }
            }
        }
    }

    /**
     * 锁定cpu电源
     */
    @SuppressLint("NewApi")
    private fun lockCpuPowerManager() {
        if (!AppUtils.isServiceRunning(
                BaseApplication.application,
                WakeUpCpuService::class.java.name
            )
        ) {
            val wakeUpCpuService = Intent(BaseApplication.application, WakeUpCpuService::class.java)
            when(Build.VERSION.SDK_INT){
                in Build.VERSION_CODES.O until Build.VERSION_CODES.S -> {
                    BaseApplication.application.startForegroundService(wakeUpCpuService)
                }
                in 0 until Build.VERSION_CODES.O -> {
                    BaseApplication.application.startService(wakeUpCpuService)
                }
            }
        }
        if (wakelock?.isHeld == true) {//如果已获得唤醒锁但尚未释放，则返回true。
            wakelock?.release()
        }
        wakelock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, javaClass.canonicalName)
        wakelock?.setReferenceCounted(false)
        wakelock?.acquire()
    }

    /**
     * 释放cpu电源
     */
    private fun unlockCpuPowerManager() {
        BaseApplication.application.stopService(
            Intent(
                BaseApplication.application,
                WakeUpCpuService::class.java
            )
        )
        wakelock?.release()
    }
}

/**
 * 任务描述类
 */
internal class ChannelTask(val task: Task, val runNow: Boolean)

/**
 * @Description:    定时任务单元执行
 * @Author:         fanj
 */
abstract class Task(
    internal val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    internal var callback: ((executeCount: Long) -> Unit)? = null
) : LifecycleEventObserver {
    internal var isCancel = false

    //已执行次数
    internal var executeCount = 0L

    /**
     * 绑定生命周期
     */
    fun attachTo(owner: LifecycleOwner) {
        owner.lifecycle.addObserver(this)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_PAUSE -> {
                if ((source is Activity && source.isFinishing) || (source is Fragment && source.activity != null && source.requireActivity().isFinishing)) {
                    callback = null
                    TimerManager.remove(this)
                }
            }
            Lifecycle.Event.ON_DESTROY -> {
                callback = null
                TimerManager.remove(this)
            }
            else -> {}
        }
    }

    /**
     * 执行任务
     * @param curTime 当前时间
     * @param runNow 是否是强制立即执行
     * @return 是否移除任务
     */
    internal abstract fun execute(scope: CoroutineScope, curTime: Long, runNow: Boolean): Boolean

    /**
     * 是否匹配执行时分
     */
    protected fun canExecuteByHourMinutes(
        curTime: Long,
        hourMinutes: HashSet<HourMinute>
    ): Boolean {
        val curHm = DateUtils.getStringByFormat(
            curTime,
            "HH:mm"
        ).let {
            it.split(":")
        }
        val hour = Integer.valueOf(curHm[0])
        val minute = Integer.valueOf(curHm[1])
        return hourMinutes.find { it.hour == hour && it.minute == minute } != null
    }

    /**
     * 是否匹配执行周
     */
    protected fun canExecuteByWeeks(curTime: Long, weeks: HashSet<Week>): Boolean {
        val calendar = Calendar.getInstance(Locale.CHINA).apply {
            timeInMillis = curTime
        }
        val fitWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
        return weeks.find { it.code == fitWeek } != null
    }

    /**
     * 是否匹配执行月
     */
    protected fun canExecuteByMonthDays(curTime: Long, monthDays: HashSet<MonthDay>): Boolean {
        val calendar = Calendar.getInstance(Locale.CHINA).apply {
            timeInMillis = curTime
        }
        val curMonth = calendar.get(Calendar.MONDAY) + 1
        val fitMonths = monthDays.filter { it.month == curMonth }
        if (fitMonths.isNullOrEmpty()) {
            return false
        }
        val curDay = calendar.get(Calendar.DAY_OF_MONTH)
        val curMonthMaxDay = DateUtils.getCurrentMonthLastDay()
        for (monthDay in fitMonths) {
            if (monthDay.day == curDay || (monthDay.day == 0 && curDay == curMonthMaxDay)) {
                return true
            }
        }
        return false
    }

    /**
     * 排除秒后，yyyy-MM-dd HH:mm是否已执行过
     */
    protected fun hasExecuteByHourMinute(curTime: Long, executeTimes: ArrayList<Long>): Boolean {
        val format = "yyyy-MM-dd HH:mm"
        val curTimeStr = DateUtils.getStringByFormat(curTime, format)
        return executeTimes.find { DateUtils.getStringByFormat(it, format) == curTimeStr } != null
    }

}

enum class TimeUnit {
    SECONDS, MINUTES, HOURS;

    fun toMilliscond(value: Long) = when (this) {
        SECONDS -> value * 1000
        MINUTES -> value * 60 * 1000
        HOURS -> value * 60 * 60 * 1000
    }
}

/**
 * 延时任务
 * 延迟一定时间执行任务
 * @param delay 延时时长
 * @param callback 执行回调
 */
open class DelayTask(
    @IntRange(from = 1)
    delay: Long,
    delayUnit: TimeUnit = TimeUnit.SECONDS,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    callback: (executeCount: Long) -> Unit
) : Task(dispatcher, callback) {
    //执行时间
    private val executeTime = TimerManager.getNowTime() + delayUnit.toMilliscond(delay)
    override fun execute(scope: CoroutineScope, curTime: Long, runNow: Boolean): Boolean {
        if (isCancel) {
            return true
        }
        if (curTime >= executeTime) {
            executeCount++
            scope.launch(dispatcher) {
                callback?.invoke(executeCount)
            }
            return true
        }
        if (runNow) {
            scope.launch(dispatcher) {
                callback?.invoke(executeCount)
            }
        }
        return false
    }
}

/**
 * 指定日期时间任务
 * @param dateTime 指定日期 yyyy-MM-dd HH:mm
 */
open class DateTimeTask(
    private val dateTime: Date,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    callback: (executeCount: Long) -> Unit
) : Task(dispatcher, callback) {
    override fun execute(scope: CoroutineScope, curTime: Long, runNow: Boolean): Boolean {
        if (isCancel) {
            return true
        }
        val format = "yyyy-MM-dd HH:mm"
        if (DateUtils.getStringByFormat(
                curTime,
                format
            ) == DateUtils.getStringByFormat(dateTime.time, format)
        ) {
            executeCount++
            scope.launch(dispatcher) {
                callback?.invoke(executeCount)
            }
            return true
        }
        if (runNow) {
            scope.launch(dispatcher) {
                callback?.invoke(executeCount)
            }
        }
        return false
    }
}

/**
 * 周期性任务
 * 每隔一定时间执行一次
 * @param startTime 限定开始执行时间 默认当前时间
 * @param endTime 限定结束执行时间 默认永久
 * @param interval 间隔执行时间
 * @param maxExecutes 限定最大执行次数 默认不限制
 */
open class IntervalTask(
    @IntRange(from = 1)
    interval: Long,
    intervalUnit: TimeUnit = TimeUnit.SECONDS,
    @IntRange(from = 1)
    private val startTime: Long = TimerManager.getNowTime(),
    @IntRange(from = 1)
    private val endTime: Long = Long.MAX_VALUE,
    @IntRange(from = 1)
    private val maxExecutes: Long = Long.MAX_VALUE,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    callback: (executeCount: Long) -> Unit
) : Task(dispatcher, callback) {
    //最后一次执行的时间
    private var lastExecuteTime = 0L

    //间隔执行时间
    private val intervalTime = intervalUnit.toMilliscond(interval)
    override fun execute(scope: CoroutineScope, curTime: Long, runNow: Boolean): Boolean {
        if (isCancel) {
            return true
        }
        //如果是立即执行，则不计入执行次数，也不刷新最后一次执行的时间
        if (runNow) {
            scope.launch(dispatcher) {
                callback?.invoke(executeCount)
            }
            return false
        }
        //当前时间在可执行时间范围内
        if (curTime in startTime..endTime && maxExecutes > executeCount) {
            //当前时间已超出上次执行时间+间隔时间
            if (curTime >= lastExecuteTime + intervalTime) {
                lastExecuteTime = curTime
                executeCount++
                scope.launch(dispatcher) {
                    callback?.invoke(executeCount)
                }
            }
            return false
        }
        return true
    }
}


/**
 * 每天固定时间执行的任务
 * @param hourMinutes 时分集合
 */
open class DailyTask(
    private val hourMinutes: HashSet<HourMinute>,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    callback: (executeCount: Long) -> Unit
) : Task(dispatcher, callback) {
    private val executeTimeList by lazy { ArrayList<Long>() }
    override fun execute(scope: CoroutineScope, curTime: Long, runNow: Boolean): Boolean {
        if (isCancel) {
            return true
        }
        if (runNow || (canExecuteByHourMinutes(curTime, hourMinutes) && !hasExecuteByHourMinute(
                curTime,
                executeTimeList
            ))
        ) {
            executeTimeList.add(curTime)
            executeCount++
            scope.launch(dispatcher) {
                callback?.invoke(executeCount)
            }
        }
        return false
    }
}

/**
 * 每周固定时间执行的任务
 * @param weeks 周集合
 * @param hourMinutes 时分集合
 */
open class WeeklyTask(
    private val weeks: HashSet<Week>,
    private val hourMinutes: HashSet<HourMinute>,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    callback: (executeCount: Long) -> Unit
) : Task(dispatcher, callback) {
    private val executeTimeList by lazy { ArrayList<Long>() }
    override fun execute(scope: CoroutineScope, curTime: Long, runNow: Boolean): Boolean {
        if (isCancel) {
            return true
        }
        if (runNow || (canExecuteByWeeks(curTime, weeks) && canExecuteByHourMinutes(
                curTime,
                hourMinutes
            ) && !hasExecuteByHourMinute(curTime, executeTimeList))
        ) {
            executeTimeList.add(curTime)
            executeCount++
            scope.launch(dispatcher) {
                callback?.invoke(executeCount)
            }
        }
        return false
    }
}

/**
 * 每月固定时间执行任务
 * @param monthDays 月日集合
 * @param hourMinutes 时分集合
 */
open class MonthlyTask(
    private val monthDays: HashSet<MonthDay>,
    private val hourMinutes: HashSet<HourMinute>,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    callback: (executeCount: Long) -> Unit
) : Task(dispatcher, callback) {
    private val executeTimeList by lazy { ArrayList<Long>() }
    override fun execute(scope: CoroutineScope, curTime: Long, runNow: Boolean): Boolean {
        if (isCancel) {
            return true
        }
        if (runNow || (canExecuteByHourMinutes(curTime, hourMinutes) && canExecuteByMonthDays(
                curTime,
                monthDays
            ) && !hasExecuteByHourMinute(curTime, executeTimeList))
        ) {
            executeTimeList.add(curTime)
            executeCount++
            scope.launch(dispatcher) {
                callback?.invoke(executeCount)
            }
        }
        return false
    }
}

data class HourMinute(
    @IntRange(from = 0, to = 23) val hour: Int,
    @IntRange(from = 0, to = 59) val minute: Int
)

enum class Week(val code: Int) {
    Sunday(0),
    Monday(1),
    Tuesday(2),
    Wednesday(3),
    Thursday(4),
    Friday(5),
    Saturday(6),
}

data class MonthDay(
    /**
     * 1~12分别代表1到12月
     */
    @IntRange(from = 1, to = 12) val month: Int,
    /**
     * 1-31代表1到31号，0：代表该月的最后一天
     */
    @IntRange(from = 0, to = 31) val day: Int
)

/**
 * 保持service唤醒服务
 */
internal class WakeUpCpuService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //开启前台通知
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val CHANNEL_ID = BaseApplication.application.packageName
            val CHANNEL_NAME = "timerManager"
            val notificationChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(false)
                lightColor = Color.TRANSPARENT
                setShowBadge(false)
                importance = NotificationManager.IMPORTANCE_HIGH
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            }
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(notificationChannel)
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.base_mask_laucher_icon)
                .setContentTitle("手机管家")
                .setContentText("已自动清理内存：${(2..200).random()}mb")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .build()
            startForeground(
                //伪装成手机管家息屏自动清理内存，避免在亮屏时用户看到一闪而过的通知会觉得奇怪
                (10000..20000).random(), notification
            )
        }
        //播放无声音乐
        playSilentMusic()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
        releaseMediaPlayer()
    }

    /**
     * 播放无声音乐
     */
    private fun playSilentMusic() {
        releaseMediaPlayer()
        //注意，该文件经过严格测试，不要修改，不是任何文件都行的，（用Audacity修改压缩至最小）
        mediaPlayer = if (OSHelper.isHarmonyOs()) {
            //鸿蒙手机必须音量大于1才生效，也就只能用无声文件播放，这个文件是Audacity生成的静音音轨，用其他网上下载的经测试无效
            MediaPlayer.create(this, R.raw.silence_voice_for_harmony).apply {
                setVolume(1f, 1f)
            }
        } else {
            //其他安卓手机不能使用时间过短或无实际声音的音频，否则会失效。这里的文件是30s有声音的，把音量设置到最小了，经测试手机最大音量后依然不可闻
            MediaPlayer.create(this, R.raw.silence_voice_for_other).apply {
                setVolume(0.0001f, 0.0001f)
            }
        }.apply {
            isLooping = true
        }
        mediaPlayer?.start()
    }

    /**
     * 释放播放器
     */
    private fun releaseMediaPlayer() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.stop()
        }
        mediaPlayer?.release()
        mediaPlayer = null
    }

}
