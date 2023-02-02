package data.remote

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import data.remote.api.ReverseGeocoderApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

const val REVERSE_GEOCODER_BASE_URL = "https://nominatim.openstreetmap.org/"

enum class RetrofitType(val baseUrl: String) {
    REVERSE_GEOCODER(REVERSE_GEOCODER_BASE_URL)
}

object RetrofitClient {

    private fun getClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY
        val httpClient = OkHttpClient.Builder()
        httpClient.addInterceptor(logging)
        return httpClient.build()
    }

    fun getRetrofit(retrofitType: RetrofitType): Retrofit {
        return Retrofit.Builder()
            .baseUrl(retrofitType.baseUrl)
            .client(getClient())
            .addCallAdapterFactory(CoroutineCallAdapterFactory.invoke())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun getReverseGeocoderApi(retrofit: Retrofit): ReverseGeocoderApi {
        return retrofit.create(ReverseGeocoderApi::class.java)
    }
}