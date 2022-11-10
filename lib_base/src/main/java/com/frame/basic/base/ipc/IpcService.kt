package com.frame.basic.base.ipc

import android.app.Service
import android.content.Intent
import android.os.*
import com.frame.basic.base.BaseApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.Serializable
import java.util.concurrent.LinkedBlockingQueue

/**
 * @Description:    IPC通讯服务
 * @Author:         fanj
 * @CreateDate:     2022/8/1 14:10
 * @Version:
 */
open class IpcService : Service() {
    private val replyBlockQueue by lazy { LinkedBlockingQueue<ReplyMessage>() }
    private val replyChannel by lazy { Channel<ReplyMessage>() }
    private val serverHandler by lazy {
        object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                if (msg.what == IpcHelper.MSG_FROM_CLIENT) {
                    replyBlockQueue.offer(ReplyMessage(msg.data, msg.replyTo))
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = Messenger(serverHandler).binder

    private fun executeMethod(bundle: Bundle): Any? {
        (bundle.getSerializable("method") as? MethodDesc)?.let {
            val parameterTypes = ArrayList<Class<*>>()
            val parameterValues = ArrayList<Any?>()
            it.params.forEach { paramsDesc ->
                parameterTypes.add(paramsDesc.type)
                parameterValues.add(paramsDesc.value)
            }
            val method = javaClass.getDeclaredMethod(it.name, *parameterTypes.toTypedArray())
            return method?.invoke(this, *parameterValues.toTypedArray()) ?: null
        }
        return null
    }

    private class ReplyMessage(val data: Bundle, val messenger: Messenger)

    private var job: Job? = null
    override fun onCreate() {
        super.onCreate()
        job = BaseApplication.application.mCoroutineScope.launch {
            launch(Dispatchers.IO) {
                while (true) {
                    replyChannel.send(replyBlockQueue.take())
                }
            }
            launch(Dispatchers.IO) {
                while (true) {
                    val replyMessage = replyChannel.receive()
                    launch(Dispatchers.Default) {
                        val result = executeMethod(replyMessage.data)
                        val messenger = replyMessage.messenger
                        val message = Message.obtain(null, IpcHelper.MSG_FROM_SERVER).apply {
                            data = Bundle().apply {
                                putSerializable("result", result as? Serializable?)
                            }
                        }
                        try {
                            messenger.send(message)
                        } catch (e: RemoteException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        job?.cancel()
        super.onDestroy()
    }
}
