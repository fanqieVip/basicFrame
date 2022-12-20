package com.frame.basic.buildsrc

/**
 * 项目相关参数配置
 *
 * @author Qu Yunshuo
 * @since 4/24/21 5:56 PM
 */
object ProjectBuildConfig {
    const val compileSdkVersion = 33
    const val buildToolsVersion = "33.0.1"
    const val applicationId = "com.frame.basic"
    const val minSdkVersion = 21
    const val targetSdkVersion = 33
    const val versionCode = 1
    const val versionName = "1.0"

    //是否启用系统查杀进程导致重启，不恢复当前栈顶而自动回到启动页的功能
    const val crashReboot = true
    /**
     * 项目当前的版本状态
     * 该状态直接反映当前App是测试版 还是正式版 或者预览版
     * 正式版:RELEASE、预发布:ALPHA、测试版BETA-开发:DEVELOP
     */
    object Version {

        const val RELEASE = "VERSION_STATUS_RELEASE"

        const val ALPHA = "VERSION_STATUS_ALPHA"

        const val BETA = "VERSION_STATUS_BETA"

        const val DEVELOP = "VERSION_STATUS_DEVELOP"
    }
}
