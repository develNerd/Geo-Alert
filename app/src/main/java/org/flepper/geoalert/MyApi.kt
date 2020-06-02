package org.flepper.geoalert

import com.google.android.gms.maps.model.LatLng
import okhttp3.OkHttpClient
import org.flepper.geoalert.models.Json4Kotlin_Base
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit


interface MyApi {




    @GET("json")
    suspend fun getDirection(@Query("origin") ll:MyLatlng,
                             @Query("destination") dd:MyLatlng,
                             @Query("sensor") sensor:String,
                             @Query("mode") mode:String,
                             @Query("key") key:String): Response<Json4Kotlin_Base>

    companion object{
        operator  fun invoke(): MyApi {

            val okkHttpclient = OkHttpClient.Builder()
                .callTimeout(8, TimeUnit.SECONDS)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .client(okkHttpclient)
                .baseUrl("https://maps.googleapis.com/maps/api/directions/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(MyApi::class.java)
        }
    }


}