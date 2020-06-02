package org.flepper.geoalert

import org.flepper.geoalert.models.Json4Kotlin_Base
import retrofit2.Response

class DirectionRepository(private val api: MyApi) {
    suspend fun getDirection(originLat:Double,originLong:Double,destLat:Double,destLng:Double,last:String) {

    }
}