package com.matfess.app.network

import com.matfess.app.util.DateUtils
import java.time.LocalDate
import java.time.ZoneId

/**
 * Retrofit-backed data source with curated offline fallbacks, so a weak campus
 * connection never leaves an empty screen during the demo.
 */
object RemoteRepository {

    suspend fun holidays(year: Int): HolidaysResult {
        return try {
            val items = ApiClient.api.publicHolidays(year)
                .mapNotNull { dto ->
                    val date = runCatching { LocalDate.parse(dto.date) }.getOrNull() ?: return@mapNotNull null
                    Holiday(
                        date = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        localName = dto.localName?.takeIf { it.isNotBlank() } ?: dto.name.orEmpty(),
                        englishName = dto.name.orEmpty(),
                        isNational = dto.types?.contains("Public") ?: dto.global
                    )
                }
                .sortedBy { it.date }
            if (items.isEmpty()) HolidaysResult(fallbackHolidays(year), false)
            else HolidaysResult(items, true)
        } catch (_: Exception) {
            HolidaysResult(fallbackHolidays(year), false)
        }
    }

    suspend fun quote(): Quote {
        return try {
            val dto = ApiClient.api.randomQuote(ApiClient.QUOTE_ENDPOINT)
            if (dto.quote.isBlank()) fallbackQuotes.random() else Quote(dto.quote, dto.author)
        } catch (_: Exception) {
            fallbackQuotes.random()
        }
    }

    private fun fallbackHolidays(year: Int): List<Holiday> {
        val fixed = listOf(
            Triple(1, 1, "Tahun Baru Masehi"),
            Triple(5, 1, "Hari Buruh Internasional"),
            Triple(6, 1, "Hari Lahir Pancasila"),
            Triple(8, 17, "Hari Kemerdekaan Republik Indonesia"),
            Triple(12, 25, "Hari Raya Natal")
        )
        return fixed.map { (month, day, name) ->
            Holiday(
                date = LocalDate.of(year, month, day)
                    .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                localName = name,
                englishName = name,
                isNational = true
            )
        }.sortedBy { it.date }
    }

    private val fallbackQuotes = listOf(
        Quote("Pelan-pelan yang penting selesai. Konsistensi mengalahkan motivasi sesaat.", "Matfess"),
        Quote("Kamu tidak harus sempurna hari ini, cukup satu langkah lebih dekat.", "Matfess"),
        Quote("Fokus bukan tentang melakukan banyak hal, tapi menyelesaikan yang penting.", "Matfess"),
        Quote("Istirahat bukan kemalasan. Otak butuh ruang untuk menyusun ulang ide.", "Matfess"),
        Quote("Deadline terasa berat kalau dikerjakan di menit terakhir. Mulai kecil sekarang.", "Matfess"),
        Quote("Belajar bukan balapan. Bandingkan dirimu dengan versi kemarin.", "Matfess"),
        Quote("Satu halaman hari ini lebih baik daripada rencana sepuluh halaman besok.", "Matfess")
    )

    fun todayLabel(): String = DateUtils.formatFull(System.currentTimeMillis())
}
