package yurazhovnir.healthgraphs.ui.health

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.kotlin.ext.copyFromRealm
import yurazhovnir.healthgraphs.HealthDataPeriod
import yurazhovnir.healthgraphs.helper.RealmHelper
import yurazhovnir.healthgraphs.model.HealthRecord
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ChartsViewModel @Inject constructor() : ViewModel() {

    private val _healthRecords = MutableLiveData<List<HealthRecord>>()
    private val _chartLabels = MutableLiveData<List<String>>()
    val chartLabels: LiveData<List<String>> get() = _chartLabels

    private val _dataMaps = MutableLiveData<Map<String, Map<String, Int>>>()
    val dataMaps: LiveData<Map<String, Map<String, Int>>> get() = _dataMaps

    var healthDataPeriod: HealthDataPeriod = HealthDataPeriod.Week

    fun loadHealthData() {
        val fetched = RealmHelper.realm?.query(HealthRecord::class)?.find()?.copyFromRealm() ?: emptyList()
        val filtered = when (healthDataPeriod) {
            HealthDataPeriod.Week -> filterByDays(fetched, 7)
            HealthDataPeriod.Month -> filterByDays(fetched, 30)
            HealthDataPeriod.Year -> filterByMonths(fetched, 12)
        }
        _healthRecords.value = filtered
        _chartLabels.value = generateLabels(filtered.size)
        _dataMaps.value = generateAllDataMaps(filtered, _chartLabels.value.orEmpty())
    }

    private fun filterByDays(records: List<HealthRecord>, days: Int): List<HealthRecord> {
        val lastDates = getLastDates(days)
        return lastDates.mapNotNull { date -> records.firstOrNull { it.startsAt == date } }
    }

    private fun filterByMonths(records: List<HealthRecord>, months: Int): List<HealthRecord> {
        val lastMonths = getLastMonths(months)
        return lastMonths.flatMap { month -> records.filter { it.startsAt?.startsWith(month) == true } }
    }

    private fun getLastDates(days: Int): List<String> {
        val format = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return (0 until days).map { LocalDate.now().minusDays(it.toLong()).format(format) }.reversed()
    }

    private fun getLastMonths(months: Int): List<String> {
        val format = DateTimeFormatter.ofPattern("yyyy-MM")
        return (0 until months).map { LocalDate.now().minusMonths(it.toLong()).format(format) }.reversed()
    }

    private fun generateLabels(count: Int): List<String> {
        val calendar = Calendar.getInstance()
        val format = when (healthDataPeriod) {
            HealthDataPeriod.Week -> SimpleDateFormat("EEE", Locale("en", "GB"))
            HealthDataPeriod.Month -> SimpleDateFormat("dd", Locale("en", "GB"))
            HealthDataPeriod.Year -> SimpleDateFormat("MMM", Locale("en", "GB"))
        }
        return (0 until count).map {
            val label = format.format(calendar.time)
            calendar.add(if (healthDataPeriod == HealthDataPeriod.Year) Calendar.MONTH else Calendar.DAY_OF_YEAR, -1)
            label
        }.reversed()
    }

    private fun generateAllDataMaps(records: List<HealthRecord>, labels: List<String>): Map<String, Map<String, Int>> {
        return mapOf(
            "steps" to createDataMap(records, labels) { it.steps ?: 0 },
            "hydration" to createDataMap(records, labels) { (it.hydration ?: 0.0).toInt() },
            "sleep" to createDataMap(records, labels) { it.sleep ?: 0 },
            "heartRate" to createDataMap(records, labels) { it.heartRate ?: 0 },
            "exercise" to createDataMap(records, labels) { it.activeMinutes ?: 0 }
        )
    }

    private fun createDataMap(records: List<HealthRecord>, labels: List<String>, selector: (HealthRecord) -> Int): Map<String, Int> {
        val map = labels.associateWith { 0 }.toMutableMap()

        if (healthDataPeriod == HealthDataPeriod.Year) {
            val formatter = SimpleDateFormat("MMM", Locale.getDefault())
            records.groupBy {
                it.startsAt?.let {
                    formatter.format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it) ?: Date())
                } ?: ""
            }.forEach { (label, list) ->
                if (label in map) {
                    map[label] = list.sumOf { selector(it) }
                }
            }
        } else {
            records.forEachIndexed { index, record ->
                if (index < labels.size) {
                    map[labels[index]] = selector(record)
                }
            }
        }

        return map
    }
}
