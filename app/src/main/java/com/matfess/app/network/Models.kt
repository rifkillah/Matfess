package com.matfess.app.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url

data class HolidayDto(
    @SerializedName("date") val date: String = "",
    @SerializedName("localName") val localName: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("global") val global: Boolean = false,
    @SerializedName("types") val types: List<String>? = null
)

data class QuoteDto(
    @SerializedName("quote") val quote: String = "",
    @SerializedName("author") val author: String = ""
)

interface MatfessApi {
    @GET("api/v3/PublicHolidays/{year}/ID")
    suspend fun publicHolidays(@Path("year") year: Int): List<HolidayDto>

    @GET
    suspend fun randomQuote(@Url url: String): QuoteDto
}

data class Holiday(
    val date: Long,
    val localName: String,
    val englishName: String,
    val isNational: Boolean
)

data class Quote(val text: String, val author: String)

data class HolidaysResult(
    val items: List<Holiday>,
    val online: Boolean
)
