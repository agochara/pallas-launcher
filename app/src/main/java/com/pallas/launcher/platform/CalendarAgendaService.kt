package com.pallas.launcher.platform

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.pallas.launcher.domain.search.LauncherSearchResult
import com.pallas.launcher.domain.search.SmartCommandType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object CalendarAgendaService {

    private data class AgendaEvent(
        val title: String,
        val beginMillis: Long,
        val endMillis: Long,
        val allDay: Boolean,
        val location: String
    )

    fun hasCalendarPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    suspend fun fetchAgendaCard(
        context: Context,
        is24Hour: Boolean
    ): LauncherSearchResult.RichCard = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission(context)) {
            return@withContext LauncherSearchResult.RichCard(
                id = "cal_perm_needed",
                type = SmartCommandType.CALENDAR,
                title = "Calendar Agenda",
                primaryValue = "Calendar permission required",
                subtitle = "Grant READ_CALENDAR to view offline agenda"
            )
        }

        val now = System.currentTimeMillis()
        val startCal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = startCal.timeInMillis
        val tomorrowStart = (startCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
        val dayAfterTomorrowStart = (startCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 2) }.timeInMillis
        val weekEnd = (startCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 7) }.timeInMillis

        // All-day CalendarContract Instances store BEGIN/END at UTC midnight rather than local midnight.
        val utcTz = TimeZone.getTimeZone("UTC")
        val todayUtcCal = Calendar.getInstance(utcTz).apply {
            clear()
            set(startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH))
        }
        val todayUtcStart = todayUtcCal.timeInMillis
        val tomorrowUtcStart = (todayUtcCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
        val dayAfterTomorrowUtcStart = (todayUtcCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 2) }.timeInMillis

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, todayStart)
        ContentUris.appendId(builder, weekEnd)

        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.STATUS
        )

        val events = mutableListOf<AgendaEvent>()
        try {
            context.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )?.use { cursor ->
                val titleCol = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
                val beginCol = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endCol = cursor.getColumnIndex(CalendarContract.Instances.END)
                val allDayCol = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)
                val locCol = cursor.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)
                val statusCol = cursor.getColumnIndex(CalendarContract.Instances.STATUS)

                while (cursor.moveToNext()) {
                    val status = if (statusCol >= 0) cursor.getInt(statusCol) else CalendarContract.Events.STATUS_CONFIRMED
                    if (status == CalendarContract.Events.STATUS_CANCELED) continue

                    val begin = if (beginCol >= 0) cursor.getLong(beginCol) else 0L
                    val end = if (endCol >= 0) cursor.getLong(endCol) else begin
                    val allDay = if (allDayCol >= 0) cursor.getInt(allDayCol) != 0 else false
                    if (allDay && end <= todayUtcStart) continue
                    if (!allDay && end < now) continue

                    val title = if (titleCol >= 0) cursor.getString(titleCol).orEmpty().trim() else ""
                    if (title.isEmpty()) continue
                    val location = if (locCol >= 0) cursor.getString(locCol).orEmpty().trim() else ""
                    events += AgendaEvent(title, begin, end, allDay, location)
                }
            }
        } catch (_: SecurityException) {
            return@withContext LauncherSearchResult.RichCard(
                id = "cal_err_perm",
                type = SmartCommandType.CALENDAR,
                title = "Calendar Agenda",
                subtitle = "Calendar permission denied"
            )
        } catch (e: Exception) {
            return@withContext LauncherSearchResult.RichCard(
                id = "cal_err_query",
                type = SmartCommandType.CALENDAR,
                title = "Calendar Agenda",
                subtitle = "Unable to read calendar (${e.javaClass.simpleName})"
            )
        }

        if (events.isEmpty()) {
            return@withContext LauncherSearchResult.RichCard(
                id = "cmd_cal_agenda",
                type = SmartCommandType.CALENDAR,
                title = "Calendar Agenda",
                primaryValue = "Clear week ahead",
                badgeText = "0 events",
                isBadgePositive = true,
                subtitle = "Next 7 days • Tap to open Calendar",
                execute = { SystemActionsManager.openCalendar(it) }
            )
        }

        val timeFormat = SimpleDateFormat(if (is24Hour) "HH:mm" else "h:mm a", Locale.getDefault())
        val dayFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        val utcDayFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).apply { timeZone = utcTz }

        val todayCount = events.count { ev ->
            if (ev.allDay) ev.beginMillis < tomorrowUtcStart && ev.endMillis > todayUtcStart
            else ev.beginMillis in todayStart until tomorrowStart
        }
        val maxDisplayed = 14
        val displayed = events.take(maxDisplayed)
        val lines = mutableListOf<String>()
        var lastBucket = ""

        for (ev in displayed) {
            val bucket = if (ev.allDay) {
                when {
                    ev.beginMillis < tomorrowUtcStart -> "TODAY"
                    ev.beginMillis < dayAfterTomorrowUtcStart -> "TOMORROW"
                    else -> utcDayFormat.format(Date(ev.beginMillis)).uppercase(Locale.getDefault())
                }
            } else {
                when {
                    ev.beginMillis < tomorrowStart -> "TODAY"
                    ev.beginMillis < dayAfterTomorrowStart -> "TOMORROW"
                    else -> dayFormat.format(Date(ev.beginMillis)).uppercase(Locale.getDefault())
                }
            }
            if (bucket != lastBucket) {
                if (lines.isNotEmpty()) lines += ""
                lines += bucket
                lastBucket = bucket
            }
            val timeStr = if (ev.allDay) {
                "All day"
            } else {
                "${timeFormat.format(Date(ev.beginMillis))}–${timeFormat.format(Date(ev.endMillis))}"
            }
            val locSuffix = if (ev.location.isNotEmpty()) " · ${ev.location}" else ""
            lines += "$timeStr  ${ev.title}$locSuffix"
        }
        if (events.size > maxDisplayed) {
            lines += ""
            lines += "+${events.size - maxDisplayed} more this week"
        }

        val primarySummary = when {
            todayCount == 1 -> "1 event today"
            todayCount > 1 -> "$todayCount events today"
            else -> "Nothing left today"
        }

        LauncherSearchResult.RichCard(
            id = "cmd_cal_agenda",
            type = SmartCommandType.CALENDAR,
            title = "Calendar Agenda",
            primaryValue = primarySummary,
            badgeText = "${events.size} event${if (events.size == 1) "" else "s"}",
            isBadgePositive = true,
            bodyText = lines.joinToString("\n"),
            subtitle = "Next 7 days • Tap to open Calendar",
            execute = { SystemActionsManager.openCalendar(it) }
        )
    }
}
