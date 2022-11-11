package com.frame.basic.base.ipc

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.frame.basic.base.BaseApplication

object IpcHelper {
    internal const val MSG_FROM_SERVER = 1
    internal const val MSG_FROM_CLIENT = 2
    private val connections by lazy { HashMap<Class<out IpcService>, MyServiceConnection>() }
    private val messengers by lazy { HashMap<Class<out IpcService>, Messenger>() }

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
        owner: LifecycleOwner?,
        bundle: Bundle,
        callBlock: CallBlock<T>?
    ) {
        connections[remote]?.binder?.let { binder ->
            var messenger = messengers[remote]
            if (messenger == null || messenger.binder != binder) {
                messenger = Messenger(binder)
                messengers[remote] = messenger
            }
            val message = Message.obtain(null, MSG_FROM_CLIENT).apply {
                data = bundle
                if (callBlock != null){
                    replyTo = Messenger(LifecycleHandler(owner, Looper.getMainLooper(), callBlock))
                }
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

    internal class LifecycleHandler<T>(
        private var owner: LifecycleOwner?,
        looper: Looper,
        private var callBlock: CallBlock<T>?
    ) : Handler(looper), LifecycleEventObserver {
        init {
            owner?.lifecycle?.addObserver(this)
        }

        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if ((source is Activity && source.isFinishing) || (source is Fragment && source.activity != null && source.requireActivity().isFinishing)) {
                        recycle()
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    recycle()
                }
                else -> {}
            }
        }

        override fun handleMessage(msg: Message) {
            if (msg.what == MSG_FROM_SERVER) {
                callBlock?.success(msg.data.getSerializable("result") as? T)
                recycle()
            }
        }

        private fun recycle(){
            callBlock = null
            owner?.lifecycle?.removeObserver(this)
            owner = null
        }
    }
}
