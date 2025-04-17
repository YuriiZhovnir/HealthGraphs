package yurazhovnir.healthgraphs.model

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

open class HealthRecord : RealmObject {
    @PrimaryKey
    var id: Int = 0
    var type: String= ""
    var doneAt: String? = ""
    var startsAt: String? = ""

    var hydration: Double? = null
    var steps: Int? = null
    var sleep: Int? = null
    var heartRate: Int? = null

    var caloriesBurned: Int? = null
    var activeMinutes: Int? = null
    var weight: Double? = null
    var bloodPressure: String? = null

    var mood: String? = null
    var note: String? = null
    var source: String? = null
    var synced: Boolean = false
}
open class LastTimeAdd : RealmObject {
    @PrimaryKey
    var id: Int = 1
    var lastAt: String? = ""
}

