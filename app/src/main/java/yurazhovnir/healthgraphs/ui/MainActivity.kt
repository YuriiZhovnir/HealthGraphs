package yurazhovnir.healthgraphs.ui

import android.os.Bundle
import yurazhovnir.healthgraphs.base.BaseBindingActivity
import yurazhovnir.healthgraphs.databinding.ActivityMainBinding

class MainActivity : BaseBindingActivity<ActivityMainBinding>(ActivityMainBinding::inflate) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        replaceFragment(android.R.id.content, FragmentFactory.newHealthConnectFragment())
    }
}