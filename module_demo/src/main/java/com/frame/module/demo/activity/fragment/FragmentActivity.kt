package com.frame.module.demo.activity.fragment

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.frame.basic.base.mvvm.c.IndicatorPlugin
import com.frame.basic.base.mvvm.c.vms
import com.frame.basic.base.mvvm.v.BaseFragment
import com.frame.basic.base.mvvm.vm.BaseVM
import com.frame.basic.common.demo.ui.CommonBaseActivity
import com.frame.module.demo.databinding.DemoActivityFragmentBinding
import com.frame.module.demo.fragment.InnerFragment
import com.frame.module.demo.fragment.TabFragment
import com.frame.module.demo.fragment.callparams.CallParamsFragment
import com.frame.module.demo.fragment.list.CoordinatorFragment
import com.frame.module.demo.fragment.list.MultiTypeRecyclerViewFragment
import com.frame.module.demo.fragment.list.SingleTypeRecyclerViewFragment
import com.frame.module.demo.fragment.maininteraction.MainInteractionFragment
import com.frame.module.demo.fragment.mainlayout.MainLayoutFragment
import com.frame.module.demo.fragment.page.PageFragment
import com.frame.module.demo.fragment.refreshlayout.RefreshLayoutFragment
import com.frame.module.demo.fragment.shareviewmodel.ShareViewModelsFragment
import dagger.hilt.android.AndroidEntryPoint
import net.lucode.hackware.magicindicator.buildins.commonnavigator.titles.CommonPagerTitleView

/**
 * @Description:
 * @Author:         fanj
 * @CreateDate:     2021/11/15 9:38
 * @Version:        1.0.2
 */
@AndroidEntryPoint
class FragmentActivity : CommonBaseActivity<DemoActivityFragmentBinding, FragmentVM>(),
    IndicatorPlugin<String> {
    override val mBindingVM: FragmentVM by vms()
    override fun title() = "Fragment相关Demo"
    override fun DemoActivityFragmentBinding.initView() {}
    override fun DemoActivityFragmentBinding.initListener() {}

    override fun getPagerTitleView(context: Context, index: Int, title: String) =
        CommonPagerTitleView(context).apply {
            val textView = AppCompatTextView(context).apply {
                text = title
            }
            setContentView(textView)
            onPagerTitleChangeListener = object : CommonPagerTitleView.OnPagerTitleChangeListener {
                override fun onSelected(index: Int, totalCount: Int) {
                    textView.typeface = Typeface.DEFAULT_BOLD
                    textView.setTextColor(Color.RED)
                }

                override fun onDeselected(index: Int, totalCount: Int) {
                    textView.typeface = Typeface.DEFAULT
                    textView.setTextColor(Color.GRAY)
                }

                override fun onLeave(
                    index: Int,
                    totalCount: Int,
                    leavePercent: Float,
                    leftToRight: Boolean
                ) {
                }

                override fun onEnter(
                    index: Int,
                    totalCount: Int,
                    enterPercent: Float,
                    leftToRight: Boolean
                ) {
                }

            }
        }

    override fun getMagicIndicator() = mBinding.tabLayout
    override fun getViewPager() = mBinding.viewPager
    override fun getIndicatorFragments(): ArrayList<Fragment> {
        val exitFragments = supportFragmentManager.fragments.filterIsInstance<BaseFragment<*,*>>()
        supportFragmentManager.beginTransaction()?.let { transaction ->
            exitFragments.forEach { f ->
                transaction.remove(f)
            }
            transaction.commit()
        }
        return ArrayList<Fragment>().apply {
                add(InnerFragment())
                add(MainLayoutFragment())
                add(MainInteractionFragment())
                add(RefreshLayoutFragment())
                add(SingleTypeRecyclerViewFragment())
                add(MultiTypeRecyclerViewFragment())
                add(CoordinatorFragment())
                add(PageFragment())
                add(ShareViewModelsFragment())
                add(CallParamsFragment().putExtra("params", "我是传输的参数，看到我了吗"))
                add(TabFragment())
        }
    }

    override fun getIndicatorTitles(): MutableLiveData<ArrayList<String>> =
        mBindingVM.indicatorTitles

    override fun getCurrentPosition(): MutableLiveData<Int> = mBindingVM.currentPos
}

class FragmentVM(savedStateHandle: SavedStateHandle) : BaseVM(savedStateHandle) {
    val currentPos by savedStateLiveData("currentPos", 0)
    val indicatorTitles by savedStateLiveData(
        "indicatorTitles", arrayListOf(
            "内嵌ViewPager Demo",
            "布局Demo",
            "交互Demo",
            "RefreshLayoutPlugin插件Demo",
            "RecyclerViewBasicPlugin插件Demo",
            "RecyclerViewMultiPlugin插件Demo",
            "CoordinatorPlugin插件Demo",
            "PagingControl控制器Demo",
            "ViewModels共享Demo",
            "界面传参Demo",
            "TabPlugin插件Demo"
        )
    )

    override fun onRefresh(owner: LifecycleOwner) {}

    override fun autoOnRefresh() = false
}
