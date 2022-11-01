package com.frame.module.demo.activity.main

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.frame.basic.base.BaseApplication
import com.frame.basic.base.ktx.watch
import com.frame.basic.base.mvvm.c.getViewModel
import com.frame.basic.base.mvvm.vm.BaseVM
import com.frame.basic.base.mvvm.vm.VMCall
import com.frame.basic.base.utils.ResourcesUtil
import com.frame.module.demo.activity.shareviewmodel.ShareViewModelsKeepVM
import com.frame.module.demo.bean.Menu
import com.frame.module.demo.repository.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject

/**
 * 首页的VM层
 */
@HiltViewModel
class MainVM @Inject constructor(
    handle: SavedStateHandle,
    private val mRepository: HomeRepository
) : BaseVM(handle) {
    val menus by savedStateLiveData<MutableList<Menu>>("menus")

    //watch语法，用于观察指定的VM对象，当指定的VM对象更新了，自己可以自动处理自己的数据，如一个用户在多个列表中有在线状态，
    //当因为某种动作触发状态变更时，多个关联列表可以自动响应
    val testWatch = MutableLiveData<Int>().watch(this, getViewModel<ShareViewModelsKeepVM>()?.data) { data, self ->
        self.postValue(data)
    }

    override fun onRefresh(owner: LifecycleOwner) {
        launch(object : VMCall {
            override val execute: suspend CoroutineScope.() -> Unit = {
                mRepository.getMenu().let {
                    menus.postValue(it)
                }
                loadSuccess()
            }

            override fun onError(code: Int, error: String?, e: Throwable) {
            }

        })
    }

    private fun backupData() {
        BaseApplication.application.externalCacheDir?.let {
            backupData(
                ResourcesUtil.resources.assets.open("test.so"),
                File("${it.absolutePath}/test.so")
            )
        }
    }

    private fun backupData(inputStream: InputStream, targetFile: File) {
        launch(object : VMCall {
            override val execute: suspend CoroutineScope.() -> Unit = {
                if (targetFile.isFile && targetFile.exists()) {
                    targetFile.delete()
                }
                var bytesum = 0
                var byteread = 0
                val inStream: InputStream = inputStream
                val fs = FileOutputStream(targetFile)
                val buffer = ByteArray(1024)
                while (inStream.read(buffer).also { byteread = it } != -1) {
                    bytesum += byteread //字节数 文件大小
                    fs.write(buffer, 0, byteread)
                }
                fs.flush()
                fs.close()
                inStream.close()
            }

            override fun onError(code: Int, error: String?, e: Throwable) {}

        })
    }

}
