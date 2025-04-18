package yurazhovnir.healthgraphs.ui.health

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.viewpager.widget.ViewPager
import yurazhovnir.healthgraphs.HealthDataPeriod
import yurazhovnir.healthgraphs.Period
import yurazhovnir.healthgraphs.base.BaseBindingFragment
import yurazhovnir.healthgraphs.base.ViewPagerAdapter
import yurazhovnir.healthgraphs.databinding.FragmentHealthConnectBinding
import yurazhovnir.healthgraphs.ui.FragmentFactory

class HealthConnectFragment : BaseBindingFragment<FragmentHealthConnectBinding>(FragmentHealthConnectBinding::inflate) {
    override val TAG: String
        get() = "HealthConnectFragment"
    private var viewPagerAdapter: ViewPagerAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.connectFragment = this
        viewPager()
        selectPeriod(Period.Week)
    }

    private fun viewPager() {
        viewPagerAdapter = ViewPagerAdapter(childFragmentManager).apply {
            addFragment(FragmentFactory.newChartsFragment(HealthDataPeriod.Week))
            addFragment(FragmentFactory.newChartsFragment(HealthDataPeriod.Month))
            addFragment(FragmentFactory.newChartsFragment(HealthDataPeriod.Year))
        }
        binding.viewPager.apply {
            adapter = viewPagerAdapter
            addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int,
                ) {
                }

                @SuppressLint("SetTextI18n")
                override fun onPageSelected(position: Int) {
                }

                override fun onPageScrollStateChanged(state: Int) {
                }
            })
        }
    }

    fun onConnectGoogleHealthClick() {

    }

    fun onWeekClick() {
        selectPeriod(Period.Week)
    }

    fun onMonthClick() {
        selectPeriod(Period.Month)
    }

    fun onYearClick() {
        selectPeriod(Period.Year)
    }

    private fun selectPeriod(period: Period) {
        binding.viewPager.currentItem = period.position
        binding.positionSelected = period.position
    }
}