package eu.tutorials.weatherapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object Contants {

    const val APP_ID: String = "caba5e6a809d348d2681f6f6c75dce05"
    const val BASE_URL: String = "https://api.openweathermap.org/data/"
    const val METRIC_UNIT: String = "metric"

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return when{
                activeNetork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                activeNetork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo!=null && networkInfo.isConnectedOrConnecting
        }
    }
}