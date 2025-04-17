package yurazhovnir.healthgraphs.helper

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

data class DailyBiometricSummary(
    val steps: Int?,
    val hydration: Double?,
    val physicalActivity: Double?,
    val sleepHours: Double?,
    val heartRate: Double?
)

class HealthConnectManager(private val context: Context) {
    private val healthConnectClient by lazy {
        try {
            val availabilityStatus = HealthConnectClient.getSdkStatus(context)
            if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE) {
                null
            } else {
                HealthConnectClient.getOrCreate(context)
            }
        }catch (ex: Exception){
           null
        }
    }

    private val requiredPermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HydrationRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
    )

    suspend fun getDailyBiometricSummary(
        startTime: Instant = Instant.now().minusSeconds(24 * 60 * 60),
        endTime: Instant = Instant.now()
    ): DailyBiometricSummary = withContext(Dispatchers.IO) {
        val grantedPermissions = getGrantedPermissions()

        val steps = if (grantedPermissions.contains(HealthPermission.getReadPermission(StepsRecord::class))) {
            readSteps(startTime, endTime)
        } else null

        val hydration = if (grantedPermissions.contains(HealthPermission.getReadPermission(HydrationRecord::class))) {
            readHydration(startTime, endTime)
        } else null

        val physicalActivity = if (grantedPermissions.contains(HealthPermission.getReadPermission(ExerciseSessionRecord::class))) {
            readExercise(startTime, endTime)
        } else null

        val sleepHours = if (grantedPermissions.contains(HealthPermission.getReadPermission(SleepSessionRecord::class))) {
            readSleep(startTime, endTime)
        } else null

        DailyBiometricSummary(
            steps = steps,
            hydration = hydration,
            physicalActivity = physicalActivity,
            sleepHours = sleepHours,
            heartRate = 0.0
        )
    }

    private suspend fun readSteps(startTime: Instant, endTime: Instant): Int? {
        val response = healthConnectClient?.readRecords(
            ReadRecordsRequest(
                StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
        )
        return response?.records?.sumOf { it.count }?.toInt()
    }

    private suspend fun readHydration(startTime: Instant, endTime: Instant): Double? {
        return try {
            val response = healthConnectClient?.readRecords(
                ReadRecordsRequest(
                    HydrationRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            response?.records?.sumOf { it.volume.inLiters }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun readSleep(startTime: Instant, endTime: Instant): Double? {
        return try {
            val response = healthConnectClient?.readRecords(
                ReadRecordsRequest(
                    SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            response?.records?.sumOf {
                java.time.Duration.between(it.startTime, it.endTime).toHours().toDouble()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun readExercise(startTime: Instant, endTime: Instant): Double? {
        return try {
            val response = healthConnectClient?.readRecords(
                ReadRecordsRequest(
                    ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            response?.records?.sumOf {
                java.time.Duration.between(it.startTime, it.endTime).toMinutes().toDouble()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun checkAndRequestPermissions(): Boolean {
        return try {
            val grantedPermissions =
                healthConnectClient?.permissionController?.getGrantedPermissions()
            grantedPermissions?.any { it in requiredPermissions } == true
        } catch (ex: Exception) {
            false
        }
    }

    suspend fun getGrantedPermissions(): Set<String> {
        return try {
            healthConnectClient?.permissionController?.getGrantedPermissions() ?: emptySet()
        } catch (ex: Exception) {
            emptySet()
        }
    }
}
