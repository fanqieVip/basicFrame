package com.frame.module.demo.ipc

import com.frame.basic.base.ipc.IpcService
import com.frame.basic.base.ipc.annotations.IpcApi
import com.frame.basic.base.ipc.annotations.IpcServer

/**
 * @Description:
 * @Author:         fanj
 * @CreateDate:     2022/8/1 15:15
 * @Version:
 */
@IpcServer
class RemoteService : IpcService(){

    @IpcApi
    fun sayHello(say: String, number: Int): String {
        return say + number
    }

    @IpcApi
    fun register(tel: String, code: String): String {
        return "true"
    }

    @IpcApi
    fun register2(data: MutableList<String>): ArrayList<Boolean>? {
        return null
    }
    @IpcApi
    fun sayHello3(say: String, number: Int): String {
        return say + number
    }
}

