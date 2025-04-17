package yurazhovnir.healthgraphs.base

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

class ViewPagerAdapter(manager: FragmentManager) : FragmentStatePagerAdapter(manager) {
    private val mFragmentList = ArrayList<Fragment>()
    private val mFragmentTitleList = ArrayList<String>()
    fun addFragment(fragment: Fragment, title: String = "") {
        val parentView = fragment.view?.parent as? ViewGroup
        parentView?.removeView(fragment.view)
        mFragmentList.add(fragment)
        mFragmentTitleList.add(title)
    }

    fun getFragments() = mFragmentList

    override fun getItem(position: Int) = mFragmentList[position]

    override fun getCount() = mFragmentList.size

    override fun getPageTitle(position: Int) = mFragmentTitleList[position]
}