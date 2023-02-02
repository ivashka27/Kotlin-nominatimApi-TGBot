import algo.PubsHandler
import bot.NominatimBot
import data.remote.RetrofitClient
import data.remote.RetrofitType
import data.remote.repository.NominatimRepository

fun main() {
    val reverseGeocoderRetrofitClient = RetrofitClient.getRetrofit(RetrofitType.REVERSE_GEOCODER)

    val bot = NominatimBot(
        NominatimRepository(
            reverseGeocoderApi = RetrofitClient.getReverseGeocoderApi(reverseGeocoderRetrofitClient)
        ),
        PubsHandler()
    ).createBot()

    bot.startPolling()
}