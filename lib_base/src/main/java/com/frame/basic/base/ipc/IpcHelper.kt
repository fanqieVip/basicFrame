package com.frame.basic.base.ipc

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import com.frame.basic.base.BaseApplication

object IpcHelper {
    internal const val MSG_FROM_SERVER = 1
    internal const val MSG_FROM_CLIENT = 2
    private val connections by lazy { HashMap<Class<out IpcService>, MyServiceConnection>() }

    fun register(vararg remotes: Class<out IpcService>) {
        val app = BaseApplication.application
        remotes.forEach {
            val conn = MyServiceConnection()
            connections[it] = conn
            app.bindService(Intent(app, it), conn, Context.BIND_AUTO_CREATE)
        }
    }

    fun unRegister(vararg remotes: Class<out IpcService>) {
        val app = BaseApplication.application
        remotes.forEach { remote ->
            connections[remote]?.let { conn ->
                app.unbindService(conn)
            }
        }
    }

    @JvmStatic
    fun <T> sendMsg(
        remote: Class<out IpcService>,
        bundle: Bundle,
        callBlock: CallBlock<T>?
    ) {
        connections[remote]?.binder?.let { binder ->
            val messenger = Messenger(binder)
            val message = Message.obtain(null, MSG_FROM_CLIENT).apply {
                data = bundle
                replyTo = Messenger(object : Handler(Looper.getMainLooper()) {
                    override fun handleMessage(msg: Message) {
                        if (msg.what == MSG_FROM_SERVER) {
                            callBlock?.success(msg.data.getSerializable("result") as? T)
                        }
                    }
                })
            }
            try {
                messenger.send(message)
            } catch (e: RemoteException) {
                e.printStackTrace()
                callBlock?.error(e.localizedMessage)
            }

        }
    }

    private class MyServiceConnection : ServiceConnection {
        var binder: IBinder? = null
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            binder = service
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            binder = null
        }

    }
}
