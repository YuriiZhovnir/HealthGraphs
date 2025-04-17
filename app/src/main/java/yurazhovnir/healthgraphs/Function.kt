package yurazhovnir.healthgraphs

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Date.setToStringFormat(outFormat: String? = null): String = this.format(outFormat)
fun Date.format(outFormat: String?): String = outFormat.simpleFormat().format(this)
fun String?.simpleFormat() = SimpleDateFormat(this ?: "yyyy-MM-dd HH:mm:ss", Locale.getDefault())

fun String.setToDate(inFormat: String? = null): Date? = this.parse(inFormat)
fun String.parse(inFormat: String?): Date? = inFormat.simpleFormat().parse(this)


