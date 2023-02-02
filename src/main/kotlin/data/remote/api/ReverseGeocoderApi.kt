package data.remote.api

import data.remote.models.Place
import data.remote.models.ReversedCountry
import kotlinx.coroutines.Deferred
import retrofit2.http.GET
import retrofit2.http.Query

interface ReverseGeocoderApi {

    @GET("reverse")
    fun getCountryNameByCoordinates(
        @Query("lat") latitude: String,
        @Query("lon") longitude: String,
        @Query("format") dataFormat: String
    ): Deferred<ReversedCountry>

    @GET("search")
    fun getObjectsBetween(
        @Query("q") typeOfObjects: String,
        @Query("format") dataFormat: String,
        @Query("viewbox") area: String,
        @Query("bounded") isArea: String,
        @Query("limit") count: String,
    ): Deferred<Place>

    @GET("search")
    fun getCoordiantesByName(
        @Query("q") name: String,
        @Query("format") dataFormat: String,
        @Query("limit") count: String
    ): Deferred<Place>
}