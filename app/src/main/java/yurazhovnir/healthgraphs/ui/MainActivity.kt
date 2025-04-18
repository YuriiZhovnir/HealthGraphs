package yurazhovnir.healthgraphs.ui

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import yurazhovnir.healthgraphs.base.BaseBindingActivity
import yurazhovnir.healthgraphs.databinding.ActivityMainBinding
import yurazhovnir.healthgraphs.helper.HealthConnectManager
import yurazhovnir.healthgraphs.helper.RealmHelper
import yurazhovnir.healthgraphs.model.HealthRecord
import yurazhovnir.healthgraphs.model.LastTimeAdd
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date

class MainActivity : BaseBindingActivity<ActivityMainBinding>(ActivityMainBinding::inflate) {

    private lateinit var healthConnectManager: HealthConnectManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        RealmHelper.realm?.query(HealthRecord::class)?.find()?.forEach { country ->
//            RealmHelper.remove(country)
//        }
//        RealmHelper.realm?.query(LastTimeAdd::class)?.find()?.forEach { country ->
//            RealmHelper.remove(country)
//        }
//        healthConnectManager = HealthConnectManager(this)
//        lifecycleScope.launch {
//            fetchAndSaveHistoricalHealthData()
//        }
        replaceFragment(android.R.id.content, FragmentFactory.newHealthConnectFragment())

    }
    suspend fun fetchAndSaveHistoricalHealthData() {
        val now = Instant.now()
        val startTime = now.minus(360, ChronoUnit.DAYS)
        val endTime = now

        val healthData = healthConnectManager.getBatchBiometricData(startTime, endTime)

        for (i in 0 until 360) {
            val dayEnd = now.minus(i.toLong(), ChronoUnit.DAYS)
            val dayStart = dayEnd.minus(1, ChronoUnit.DAYS)

            val dailyData = healthData.getDailyData(dayStart, dayEnd)

            val recordDateTime = LocalDateTime.ofInstant(dayStart, ZoneId.systemDefault())
            val formattedDate = recordDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

            val newRecord = HealthRecord().apply {
                id = Date().time.toInt()
                doneAt = formattedDate
                startsAt = formattedDate
                displayDate = formattedDate

                steps = dailyData.steps
                hydration = dailyData.hydration
                sleep = dailyData.sleepHours?.toInt()
                heartRate = dailyData.heartRate?.toInt()
                activeMinutes = dailyData.physicalActivity?.toInt()
                source = "Health Connect"
            }

            RealmHelper.save(newRecord)
        }
    }
}