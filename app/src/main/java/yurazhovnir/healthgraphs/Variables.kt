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

enum class HealthDataPeriod {
    Week,
    Month,
    Year
}

enum class Period(val position: Int) {
    Week(0),
    Month(1),
    Year(2);

    companion object {
        fun fromPosition(position: Int): Period {
            return values().firstOrNull { it.position == position } ?: Week
        }
    }
}