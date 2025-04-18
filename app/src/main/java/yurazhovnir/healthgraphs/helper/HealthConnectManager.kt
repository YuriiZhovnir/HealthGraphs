package yurazhovnir.healthgraphs.helper

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import yurazhovnir.healthgraphs.toDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date

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
        } catch (ex: Exception) {
            null
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
        fun getDailyData(dayStart: Date, dayEnd: Date): DailyBiometricSummary {
            val steps = stepsRecords
                .filter { it.startTime.toDate() >= dayStart && it.startTime.toDate() < dayEnd }
                .sumOf { it.count }
                .toInt()

            val hydration = hydrationRecords
                .filter { it.startTime.toDate() >= dayStart && it.startTime.toDate() < dayEnd }
                .sumOf { it.volume.inLiters }

            val physicalActivity = exerciseRecords
                .filter {
                    it.startTime.toDate() >= dayStart && it.endTime.toDate() <= dayEnd ||
                            (it.startTime.toDate() < dayEnd && it.endTime.toDate() > dayStart)
                }
                .sumOf {
                    val overlapStart = maxOf(it.startTime.toDate(), dayStart)
                    val overlapEnd = minOf(it.endTime.toDate(), dayEnd)
                    (overlapEnd.time - overlapStart.time).toDouble() / 1000 / 60
                }

            val sleepHours = sleepRecords
                .filter {
                    it.startTime.toDate() >= dayStart && it.endTime.toDate() <= dayEnd ||
                            (it.startTime.toDate() < dayEnd && it.endTime.toDate() > dayStart)
                }
                .sumOf {
                    val overlapStart = maxOf(it.startTime.toDate(), dayStart)
                    val overlapEnd = minOf(it.endTime.toDate(), dayEnd)
                    (overlapEnd.time - overlapStart.time).toDouble() / 1000 / 60 / 60
                }

            val heartRateSum = heartRateRecords
                .filter { it.startTime.toDate() >= dayStart && it.endTime.toDate() < dayEnd }
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
        startTime: Date,
        endTime: Date
    ): BatchBiometricData = withContext(Dispatchers.IO) {
        val grantedPermissions = getGrantedPermissions()

        val zonedEndTime = ZonedDateTime.ofInstant(endTime.toInstant(), ZoneId.systemDefault())
        val zonedStartTime = ZonedDateTime.ofInstant(startTime.toInstant(), ZoneId.systemDefault())

        val stepsData = if (grantedPermissions.contains(HealthPermission.getReadPermission(StepsRecord::class))) {
            healthConnectClient?.readRecords(
                ReadRecordsRequest(
                    StepsRecord::class,
                    pageSize = 5000,
                    timeRangeFilter = TimeRangeFilter.between(zonedStartTime.toInstant(), zonedEndTime.toInstant())
                )
            )?.records ?: emptyList()
        } else emptyList()


        val hydrationData = if (grantedPermissions.contains(HealthPermission.getReadPermission(HydrationRecord::class))) {
            healthConnectClient?.readRecords(
                ReadRecordsRequest(
                    HydrationRecord::class,
                    pageSize = 5000,
                    timeRangeFilter = TimeRangeFilter.between(startTime.toInstant(), endTime.toInstant())
                )
            )?.records ?: emptyList()
        } else emptyList()

        val sleepData = if (grantedPermissions.contains(HealthPermission.getReadPermission(SleepSessionRecord::class))) {
            healthConnectClient?.readRecords(
                ReadRecordsRequest(
                    SleepSessionRecord::class,
                    pageSize = 5000,
                    timeRangeFilter = TimeRangeFilter.between(startTime.toInstant(), endTime.toInstant())
                )
            )?.records ?: emptyList()
        } else emptyList()

        val exerciseData = if (grantedPermissions.contains(HealthPermission.getReadPermission(ExerciseSessionRecord::class))) {
            healthConnectClient?.readRecords(
                ReadRecordsRequest(
                    ExerciseSessionRecord::class,
                    pageSize = 5000,
                    timeRangeFilter = TimeRangeFilter.between(startTime.toInstant(), endTime.toInstant())
                )
            )?.records ?: emptyList()
        } else emptyList()

        val heartRateData = if (grantedPermissions.contains(HealthPermission.getReadPermission(HeartRateRecord::class))) {
            healthConnectClient?.readRecords(
                ReadRecordsRequest(
                    HeartRateRecord::class,
                    pageSize = 5000,
                    timeRangeFilter = TimeRangeFilter.between(startTime.toInstant(), endTime.toInstant())
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
