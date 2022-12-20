package com.frame.module.demo.service

import com.google.auto.service.AutoService
import com.frame.basic.base.mvvm.c.ServiceBean
import com.frame.basic.base.mvvm.c.toServiceBean
import com.frame.basic.base.utils.ToastUtils
import com.frame.basic.provider.service.DemoService
import com.frame.module.demo.bean.User2Bean

/**
 * @Description:    对外服务
 * @Author:         fanj
 * @CreateDate:     2021/11/17 12:08
 * @Version:        1.0.2
 */
@AutoService(DemoService::class)
class DemoServiceImpl: DemoService {
    override fun toastSomething(text: String) {
        ToastUtils.showShort(text)
    }

    /**
     * 加上JsonBean文档注释以标记数据的样例，调用端方便做适配
     */
    override fun speakSomething(@ServiceBean(User2Bean::class) user: Any, desc: String): @ServiceBean(User2Bean::class) Any {
        val bean = user.toServiceBean<User2Bean>()
        bean.speak(desc)
        return bean
    }
}
