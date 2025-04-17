package yurazhovnir.healthgraphs.ui

import android.os.Bundle
import yurazhovnir.healthgraphs.base.BaseBindingActivity
import yurazhovnir.healthgraphs.databinding.ActivityMainBinding
import yurazhovnir.healthgraphs.helper.RealmHelper
import yurazhovnir.healthgraphs.model.HealthRecord
import yurazhovnir.healthgraphs.model.LastTimeAdd

class MainActivity : BaseBindingActivity<ActivityMainBinding>(ActivityMainBinding::inflate) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        RealmHelper.realm?.query(HealthRecord::class)?.find()?.forEach { country ->
//            RealmHelper.remove(country)
//        }
//        RealmHelper.realm?.query(LastTimeAdd::class)?.find()?.forEach { country ->
//            RealmHelper.remove(country)
//        }
        replaceFragment(android.R.id.content, FragmentFactory.newHealthConnectFragment())
    }
}