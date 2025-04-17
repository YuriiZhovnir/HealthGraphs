package yurazhovnir.healthgraphs

import android.content.res.Resources
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord

val PERMISSIONS = setOf(
    HealthPermission.getReadPermission(StepsRecord::class),
    HealthPermission.getReadPermission(HydrationRecord::class),
    HealthPermission.getReadPermission(SleepSessionRecord::class),
    HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    HealthPermission.getReadPermission(HeartRateRecord::class)
)

val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density + 0.5f).toInt()

enum class HealthDataPeriod(val days: Int) {
    Day(1),
    Week(7),
    Month(30),
    Year(12)
}

enum class Period(val position: Int) {
    Day(0),
    Week(1),
    Month(2),
    Year(3);

    companion object {
        fun fromPosition(position: Int): Period {
            return values().firstOrNull { it.position == position } ?: Day
        }
    }
}