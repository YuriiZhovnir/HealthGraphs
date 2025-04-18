package yurazhovnir.healthgraphs.ui

import android.os.Bundle
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import yurazhovnir.healthgraphs.PERMISSIONS
import yurazhovnir.healthgraphs.base.BaseBindingActivity
import yurazhovnir.healthgraphs.databinding.ActivityMainBinding
import yurazhovnir.healthgraphs.helper.HealthConnectManager
import yurazhovnir.healthgraphs.helper.RealmHelper
import yurazhovnir.healthgraphs.model.HealthRecord
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : BaseBindingActivity<ActivityMainBinding>(ActivityMainBinding::inflate) {

    private lateinit var healthConnectManager: HealthConnectManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.connectFragment = this
        healthConnectManager = HealthConnectManager(this)
    }

    fun onConnectHealthClick() {
        try {
            val healthConnectClient = HealthConnectClient.getOrCreate(this)
            lifecycleScope.launch {
                checkPermissionsAndRun(healthConnectClient)
            }
        } catch (ex: Exception) {
        }
    }

    fun onOpenChartsClick() {
        replaceFragment(android.R.id.content, FragmentFactory.newHealthConnectFragment())
    }

    private suspend fun fetchAndSaveHistoricalHealthData() {
        RealmHelper.realm?.query(HealthRecord::class)?.find()?.forEach { record ->
            RealmHelper.remove(record)
        }

        val now = Date()
        val calendar = Calendar.getInstance()
        calendar.time = now
        calendar.add(Calendar.DAY_OF_YEAR, -360)
        val startDate = calendar.time

        val healthData = healthConnectManager.getBatchBiometricData(startDate, now)

        for (i in 0 until 360) {
            val dayEndCal = Calendar.getInstance().apply {
                time = now
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            val dayEnd = dayEndCal.time

            val dayStartCal = Calendar.getInstance().apply {
                time = now
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dayStart = dayStartCal.time

            val dailyData = healthData.getDailyData(dayStart, dayEnd)

            val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(dayStart)

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


    private suspend fun checkPermissionsAndRun(healthConnectClient: HealthConnectClient) {
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        if (granted.any { it in PERMISSIONS }) {
            fetchAndSaveHistoricalHealthData()
        } else {
            requestPermissions.launch(PERMISSIONS)
        }
    }

    private val requestPermissions = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.any { it in PERMISSIONS }) {
            lifecycleScope.launch {
                fetchAndSaveHistoricalHealthData()
            }
        }
    }
}