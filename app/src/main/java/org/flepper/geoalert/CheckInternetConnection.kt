package org.flepper.geoalert

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build

class CheckInternetConnection(context: Context) {

    private val applicationContext = context.applicationContext

    fun check(){
        if (!isInternetAvailable())
            throw NoInternetException("Make sure you have an active data connection")
        return
    }

    

     fun isInternetAvailable(): Boolean {
        var result = false
        val connectivityManager =
            applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        connectivityManager?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                it.getNetworkCapabilities(connectivityManager.activeNetwork)?.apply {
                    result = when {
                        hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                        hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                        else -> false
                    }
                }
            } else {

                val activeNetwork: NetworkInfo? = it.activeNetworkInfo
                result = when (activeNetwork?.getType()) {
                    ConnectivityManager.TYPE_WIFI -> {
                        true
                    }
                    ConnectivityManager.TYPE_MOBILE -> {
                        true
                    }
                    else -> {
                        false
                    }

                }
            }
        }
        return result
    }


}