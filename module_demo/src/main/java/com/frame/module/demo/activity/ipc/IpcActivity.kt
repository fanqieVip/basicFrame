package com.frame.module.demo.activity.ipc

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import com.frame.basic.base.ipc.CallBlock
import com.frame.basic.base.ktx.onClick
import com.frame.basic.base.mvvm.c.vms
import com.frame.basic.base.mvvm.vm.BaseVM
import com.frame.basic.base.utils.ToastUtils
import com.frame.basic.common.demo.ui.CommonBaseActivity
import com.frame.module.demo.databinding.DemoActivityIpcBinding
import com.frame.module.demo.ipc.apt.RemoteServiceCall

/**
 * @Description:
 * @Author:         fanj
 * @CreateDate:     2022/8/1 15:40
 * @Version:
 */
class IpcActivity : CommonBaseActivity<DemoActivityIpcBinding, IpcVM>() {
    override fun DemoActivityIpcBinding.initView() {
    }

    override fun DemoActivityIpcBinding.initListener() {
        send.onClick {
            RemoteServiceCall.sayHello(this@IpcActivity,"你好", 123, object :CallBlock<String>(){
                override fun success(data: String?) {
                    ToastUtils.showShort(data?:"")
                }

            })
        }
    }

    override val mBindingVM: IpcVM by vms()
}

class IpcVM(handle: SavedStateHandle) : BaseVM(handle) {
    override fun onRefresh(owner: LifecycleOwner) {
    }

    override fun autoOnRefresh() = false

}
