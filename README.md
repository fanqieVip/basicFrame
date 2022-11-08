# <p align="center"> AndroidBaseFrame </p>

> &emsp;&emsp;**AndroidBaseFrame** 是一整套Android开发框架，所使用技术栈为：**组件化、Kotlin、MVVM、Jetpack、Repository、Kotlin-Coroutine**，本框架除集成了主流依赖库外，更**着重于解决开发过程中遇到的疑难杂症，让开发工作更简单，更专注于业务实现**。该框架作为个人技术积累的产物（实际开发超过两年），已在多个项目稳定运行，是本人所在公司的标准开发架构，可放心使用。 **GitHub** 上提 **Issues**，我会及时进行回应。希望这个框架项目能给大家带来帮助，喜欢可以Start。


## 基础库

1.本项目基于[AndroidBaseFrameMVVM](https://github.com/Quyunshuo/AndroidBaseFrameMVVM)提供的脚手架功能，它有详细的集成库说明，这里不再赘述。
2.移除了Flow、EventBus、PermissionX，并接入了SmartRefreshLayout、ReadyStateSoftware、MagicIndicator、ShapeView、Lottie、Room、XXPermission库，这些库的作用在这里不再赘述

## Demo

- **lib_common_demo:**

  这里重点用来演示基础的base类的二次封装，方便你快速掌握BaseActivity、BaseFragment、BaseDialog、BasePopuwindow的基础自定义。


- **module_demo:**

  这里几乎涵盖了所有本框架的API的演示代码，方便你快捷体验这些API的特点及使用方式。

## 基础UI篇

- **规范APP基本交互流程、避免产生大量模板代码:**

  1.在MainLayoutActivity、MainInteractionActivity中演示了本框架的基本交互路程的定义，核心思想就是将loading、empty、error、success抽象处理单独维护，由框架自动调度，而你需要关心其在不同状态下的呈现样式即可。
  2.除此以外你还可以单独对标题栏，上下左右非交互区做自定义，详请可阅读demo源码
  3.可以阅读ContainerStyle协议，Activity、Fragment、Dialog、Popuwindow四种组件均遵守该协议定义的标准进行交互

- **消除Activity、Fragment、Dialog、Popuwindow的编码习惯差异，让你在不同界面组件下编码也行云流水:**

  1.这四种组件的编码方式基本一模一样（基类除外），甚至只需要改下基类就编程另一种组件了，便于在不同业态下快速切换。
  2.可以阅读UIControl协议，四种组件均遵守该协议定义的标准
  3.Popuwindow本身并不支持MVVM这种架构，本框架基于DialogFragment封装了一个Popuwindow，使其不仅能像Popuwindow一样工作，更能兼容MVVM，且也支持低内存重建恢复等框架特性

- **异形屏、挖孔屏、全面屏、上下导航栏、状态栏颜色、Dialog全屏等各种适配问题，框架自动处理了**

  1.框架默认将页面设置到填充全屏，你只需要给他一个标题栏，他会自动处理适配问题
  2.tab各种嵌套切换、页面跳转等复杂场景的状态栏、导航栏颜色，自动维护，而你只需告诉当前显示的页面或tab应该使用什么颜色即可
  3.Dialog想要真正全屏，只需重写getWidth,getHeight为全屏即可，框架会自动处理兼容问题

- **Dialog常见的样式封装**

  1.可快速定义是否点击允许外部关闭、是否点击允许物理返回关闭、是否允许外部事件穿透、透明度、宽度、高度、对齐方式、进出动画（默认提供了上下左右中五种动画）

- **解决软键盘造成的一些奇奇怪怪的问题**

  1.inputFitKeyBoard返回true，就可以自动将界面顶到键盘上面去
  2.autoHideSoftInput返回true，就可以自动点击外部关闭键盘
  3.自动处理一些软键盘可能造成的内存泄漏问题

- **【陈年顽疾】解决Fragment各种tab多层嵌套的生命周期问题**

  1.该框架提供了onResumeVisible/onStopInvisible两个准确的回调方法，方便你准确的把握当前Fragment的显示状态

## UI插件篇（可混搭使用）

- **RefreshLayoutPlugin:**

  1.界面实现该协议就具备了上下拉交互的功能

- **RecyclerViewPlugin | RecyclerViewBasicPlugin | RecyclerViewBasicPlugin:**

  1.界面实现上述协议就具备了列表的功能,标准列表或多布局列表

- **CoordinatorPlugin:**

  1.界面实现该协议就具备了协调者布局功能

- **ViewModel实现PagingControl协议:**

  1.实现该协议就具备了自动分页，自动适配loading、空页面、错误、无更多等等交互功能
  2.可与前面几个插件任意组合混搭

- **TabPlugin:**

  1.界面实现该协议就具备了常规的Tab主页面功能，并规避了旋转重建的内存泄漏问题
  2.tab按钮是仿的官方的RadioGroup，但更强大是可以任意嵌套，更便于定制

- **IndicatorPlugin:**

  1.界面实现该协议就具备了常见的Tab分栏功能，规避了旋转重建的内存泄漏问题，且支持刷新

- **WebViewPlugin:**

  1.界面实现该协议就具备了简易浏览器功能，框架自动处理多窗、下载、上传、前进、后退等你可能遇到的很多奇怪问题

## 数据通讯篇

- **AutoService实现组件化通讯:**

  1.使用谷歌官方的组件化通讯方案，相较于ARoter那种Mapping方案更加简洁高效
  2.本框架封装了getServiceProvider方法可快速获取实例并使用

- **IpcServer实现多进程通讯:**

  1.使用该注解可自动生成IPC通讯代码，简化到甚至你感觉不到你在多进程通讯

- **简化跨页面通讯:**

  1.putExtra(owner,key,block)，四种界面均扩展该方法。在打开的页面，可以通过ViewModel中by savedStateLiveData<>(key),实现页面回调。**该方案相对于事件总线、onActivityResult等传统方案，优势在于意图和响应更集中、回调更简单。**
  2.by vms<>(),通过该方式加载的ViewModel也可实现跨页面数据共享及通讯
  3.by keepVms<>(),通过该方式加载的ViewModel也可实现跨页面数据共享及通讯,但这种方式的ViewModel是共享应用生命周期的

## 组件化篇

- **ApplicationLifecycle:**

  1.Module组件中实现该协议并使用AutoService注解，即可同步application生命周期
  2.initByFrontDesk、initByBackstage中按需存放需要立即初始化、必须或非必须主线程初始化等函数，可显著提升应用启动速度
  3.resoursePrefix

- **Gradle android.resoursePrefix:**

  1.指定资源名约束可显著降低资源冲突的问题

- **toServiceBean | @ServiceBean:**

  1.尽量使用基本类型参数，通过@ServiceBean文档注解标识类型，通过toServiceBean还原类型，避免对象下沉臃肿

## ShareViewModel篇

- **vms:**

  1.本框架仿照官方API设计的一套可以跨页面共享的VM共享方案，当最后一个持有的LifecycleOwner销毁后才会销毁，这个API**大大降低了跨页面数据共享及事件响应的复杂度**。
  2.对于一些数据高并发的场景，尤其推荐使用

- **keepVms:**

  1.本框架仿照官方API设计的一套可以全局共享的VM共享方案，他的存储对象与vms是共享的。对于一些独立的非页面的全局数据推荐使用

## 数据存取篇

- **by localLiveData:**

  1.该api可自动与mmkv数据层同步，其内部类型是个LiveData,方便数据观察而无需关心存取过程

- **by savedStateLiveData:**

  1.该api可自动同步bundle中的数据，并且可以自动识别putExtra传来的block对象
  2.其内部分支封装了官方的SavedStateHandle对象，实例仍是一个LiveData,方便数据观察而无需关心存取过程
  3.由于其本质是存到bundle中，所以它也是UI重建优化篇中的重要一环

## UI重建优化篇

- **场景概述:**

  1.发生横竖屏切换时页面重建
  2.发生低内存杀页面重建
  3.发生后台杀进程，从ActivityRecord中重建
  4.权限变更重启进程，从ActivityRecord中重建

- **savedStateLiveData + viewModel +  localLiveData:**

  1.该组合可以解决除了杀进程/重启进程后重建的大部分问题，是使用该框架理应有的基操
  2.如果发生杀进程/重启进程后重建可能会导致界面看起来恢复了，但是实际上由于内存数据丢失可能会造成很多不可预知的问题，这个需要你做大量的自测和推导优化

- **兜底方案（参考微信）:**

  1.ProjectBuildConfig.crashReboot开启后，可在发生重启进程重建时自动识别并跳转到启动页重新走启动流程，以根治很多莫名其妙的问题
  2.框架已自动处理了清空栈历史会有短暂白屏或黑屏的问题，过渡会很平滑

## 工具类篇

- **DownloadManager:**
  1.支持网络限定，根据匹配的网络限定情况自动暂停或启动
  2.支持自动重连，速率限制
  3.支持**网络自动负载均衡**（自动让出指定速率宽带给具体业务，如视频通话场景）
  4.支持下载状态及进度多点观察
  5.支持生命周期绑定、断点续传
  6.支持**保持CPU不挂起app**

- **TimeManager:**
  1.全局唯一定时器，降低定时器消耗
  2.支持延迟执行任务、定期执行任务、周期执行任务等
  3.支持生命周期绑定及执行线程定义
  
- **LiveData.watch | LiveData.watchFoever:**
  1.可observer多个LiveData（可跨页面观测）
  2.watch会自动绑定被观察者生命周期，无需担心内存泄漏问题
  3.watchFoever不会绑定被观察者生命周期，最好只在长生命周期keepVms的VM中使用，避免页面内存泄漏或者VM泄漏
  4.注意：父类中的VM的watch语法不会生效，由于自动绑定生命周期是通过反射实现的，考虑到性能问题，本人舍弃了对父类成员变量的获取

###感谢阅读！更多API可参考Demo实例






