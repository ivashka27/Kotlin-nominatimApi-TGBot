package data.remote.repository

import data.remote.api.ReverseGeocoderApi
import data.remote.models.Place
import data.remote.models.ReversedCountry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NominatimRepository(
    private val reverseGeocoderApi: ReverseGeocoderApi
) {

    suspend fun getCountryNameByCoordinates(latitude: String, longitude: String, format: String): ReversedCountry {
        return withContext(Dispatchers.IO) {
            reverseGeocoderApi.getCountryNameByCoordinates(latitude, longitude, format)
        }.await()
    }

    suspend fun getObjectsBetween(typeOfObjects: String, format: String, isArea: String, area: String, count: String): Place {
        return withContext(Dispatchers.IO) {
            reverseGeocoderApi.getObjectsBetween(typeOfObjects, format, area, isArea, count)
        }.await()
    }

    suspend fun getCoordiantesByName(name: String, format: String, count: String): Place {
        return withContext(Dispatchers.IO) {
            reverseGeocoderApi.getCoordiantesByName(name, format, count)
        }.await()
    }
}