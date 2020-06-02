package org.flepper.geoalert

data class MyLatlng(  val lat:Double,
                      val lng:Double) {



    override fun toString(): String {
        return java.lang.String.format("%.1f,%.1f", lat, lng)
    }
}