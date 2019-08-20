package com.improveit.face.retrofit


import com.improveit.face.BASE_URL_CONF
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


object RetrofitFactory {
    private var retrofit: InterfaceBackendAPI? = null

    val retrofitInstance: InterfaceBackendAPI?
        get() {
            if (retrofit == null) {
                retrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL_CONF)
                    .client(getClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build()
                    .create(InterfaceBackendAPI::class.java)
            }
            return retrofit
        }


    private fun getClient(): OkHttpClient {

        val httpClient = OkHttpClient.Builder()

        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY
        httpClient
            .addNetworkInterceptor(logging)
            .connectTimeout(90, TimeUnit.SECONDS) // connect timeout
            .writeTimeout(90, TimeUnit.SECONDS) // write timeout
            .readTimeout(90, TimeUnit.SECONDS) // read timeout

        return httpClient.build()
    }
}