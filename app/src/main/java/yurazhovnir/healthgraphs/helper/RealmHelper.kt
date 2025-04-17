package yurazhovnir.healthgraphs.helper

import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.notifications.ResultsChange
import io.realm.kotlin.types.RealmObject
import kotlinx.coroutines.flow.Flow
import yurazhovnir.healthgraphs.model.HealthRecord

object RealmHelper {
    private val config =
        RealmConfiguration.Builder(
            schema = setOf(
                HealthRecord::class,
            )
        )
            .schemaVersion(1)
            .build()

    var realm: Realm? = Realm.open(config)

    fun <E : RealmObject> save(obj: E) {
        realm?.writeBlocking {
            copyToRealm(obj, UpdatePolicy.ALL)
        }
    }

    fun <E : RealmObject> save(objects: List<E>) {
        realm?.writeBlocking {
            objects.map { copyToRealm(it, UpdatePolicy.ALL) }
        }
    }

    fun <E : RealmObject> removeAll(objects: ArrayList<E>) {
        realm?.writeBlocking {
            objects.forEach { obj ->
                this.findLatest(obj)?.also {
                    delete(it)
                }
            }
        }
    }

    fun <E : RealmObject> remove(obj: E) {
        realm?.writeBlocking {
            this.findLatest(obj)?.also {
                delete(it)
            }
        }
    }
    fun getHealthRecordFlow(): Flow<ResultsChange<HealthRecord>>? {
        return realm?.query(HealthRecord::class)?.asFlow()
    }
}