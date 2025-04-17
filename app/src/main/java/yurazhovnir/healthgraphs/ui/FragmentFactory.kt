package yurazhovnir.healthgraphs.ui

import yurazhovnir.healthgraphs.HealthDataPeriod
import yurazhovnir.healthgraphs.ui.health.ChartsFragment
import yurazhovnir.healthgraphs.ui.health.HealthConnectFragment

object FragmentFactory {
    fun newHealthConnectFragment() = HealthConnectFragment()
    fun newChartsFragment(healthDataPeriod: HealthDataPeriod) = ChartsFragment().apply {
        this.healthDataPeriod = healthDataPeriod
    }
}