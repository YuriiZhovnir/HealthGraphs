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
    data class BatchBiometricData(
        val stepsRecords: List<StepsRecord>,
        val hydrationRecords: List<HydrationRecord>,
        val sleepRecords: List<SleepSessionRecord>,
        val exerciseRecords: List<ExerciseSessionRecord>,
        val heartRateRecords: List<HeartRateRecord>
    ) {
        fun getDailyData(dayStart: Instant, dayEnd: Instant): DailyBiometricSummary {
            val steps = stepsRecords
                .filter { it.startTime >= dayStart && it.endTime < dayEnd }
                .sumOf { it.count }
                .toInt()

            val hydration = hydrationRecords
                .filter { it.startTime >= dayStart && it.startTime < dayEnd }
                .sumOf { it.volume.inLiters }

            val physicalActivity = exerciseRecords
                .filter { it.startTime >= dayStart && it.endTime <= dayEnd ||
                        (it.startTime < dayEnd && it.endTime > dayStart) }
                .sumOf {
                    val overlapStart = maxOf(it.startTime, dayStart)
                    val overlapEnd = minOf(it.endTime, dayEnd)
                    java.time.Duration.between(overlapStart, overlapEnd).toMinutes().toDouble()
                }

            val sleepHours = sleepRecords
                .filter { it.startTime >= dayStart && it.endTime <= dayEnd ||
                        (it.startTime < dayEnd && it.endTime > dayStart) }
                .sumOf {
                    val overlapStart = maxOf(it.startTime, dayStart)
                    val overlapEnd = minOf(it.endTime, dayEnd)
                    java.time.Duration.between(overlapStart, overlapEnd).toHours().toDouble()
                }

            val heartRateSum = heartRateRecords
                .filter { it.startTime >= dayStart && it.endTime < dayEnd }
                .flatMap { it.samples }
                .map { it.beatsPerMinute.toDouble() }

            val heartRate = if (heartRateSum.isNotEmpty()) {
                heartRateSum.average()
            } else null

            return DailyBiometricSummary(
                steps = if (steps > 0) steps else null,
                hydration = if (hydration > 0) hydration else null,
                physicalActivity = if (physicalActivity > 0) physicalActivity else null,
                sleepHours = if (sleepHours > 0) sleepHours else null,
                heartRate = heartRate
            )
        }
    }

    suspend fun getBatchBiometricData(
        startTime: Instant,
        endTime: Instant
    ): BatchBiometricData = withContext(Dispatchers.IO) {
        val grantedPermissions = getGrantedPermissions()

        // Fetch all steps data in one request
        val stepsData = if (grantedPermissions.contains(HealthPermission.getReadPermission(StepsRecord::class))) {
            healthConnectClient?.readRecords(
                ReadRecordsRequest(
                    StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )?.records ?: emptyList()
        } else emptyList()

        // Fetch all hydration data in one request
        val hydrationData = if (grantedPermissions.contains(HealthPermission.getReadPermission(HydrationRecord::class))) {
            healthConnectClient?.readRecords(
                ReadRecordsRequest(
                    HydrationRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )?.records ?: emptyList()
        } else emptyList()

        // Fetch all sleep data in one request
        val sleepData = if (grantedPermissions.contains(HealthPermission.getReadPermission(SleepSessionRecord::class))) {
            healthConnectClient?.readRecords(
                ReadRecordsRequest(
                    SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )?.records ?: emptyList()
        } else emptyList()

        // Fetch all exercise data in one request
        val exerciseData = if (grantedPermissions.contains(HealthPermission.getReadPermission(ExerciseSessionRecord::class))) {
            healthConnectClient?.readRecords(
                ReadRecordsRequest(
                    ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )?.records ?: emptyList()
        } else emptyList()

        // Fetch all heart rate data in one request
        val heartRateData = if (grantedPermissions.contains(HealthPermission.getReadPermission(HeartRateRecord::class))) {
            healthConnectClient?.readRecords(
                ReadRecordsRequest(
                    HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )?.records ?: emptyList()
        } else emptyList()

        BatchBiometricData(
            stepsRecords = stepsData,
            hydrationRecords = hydrationData,
            sleepRecords = sleepData,
            exerciseRecords = exerciseData,
            heartRateRecords = heartRateData
        )
    }
}
