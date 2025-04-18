package yurazhovnir.healthgraphs

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Date.setToStringFormat(outFormat: String? = null): String {
    return this.format(outFormat ?: "yyyy-MM-dd HH:mm:ss") // Якщо формат не передано, використовується за замовчуванням
}

fun Date.format(outFormat: String): String {
    return SimpleDateFormat(outFormat, Locale.getDefault()).format(this)
}

fun String?.simpleFormat(): SimpleDateFormat {
    return SimpleDateFormat(this ?: "yyyy-MM-dd HH:mm:ss", Locale.getDefault())
}

fun String.setToDate(inFormat: String? = null): Date? {
    return this.parse(inFormat)
}

fun String.parse(inFormat: String?): Date? {
    return try {
        SimpleDateFormat(inFormat ?: "yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(this)
    } catch (e: Exception) {
        null // Якщо формат не вірний, повертається null
    }
}


