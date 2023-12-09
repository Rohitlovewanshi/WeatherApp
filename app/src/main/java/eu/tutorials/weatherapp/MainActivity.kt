package eu.tutorials.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import eu.tutorials.weatherapp.models.WeatherResponse
import eu.tutorials.weatherapp.network.WeatherService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

class MainActivity : AppCompatActivity() {

    private lateinit var mFusedLocationClient : FusedLocationProviderClient

    private var mProgressDialog: Dialog? = null
    private var tv_main: TextView? = null
    private var tv_main_description: TextView? = null
    private var tv_temp: TextView? = null
    private var tv_humidity: TextView? = null
    private var tv_country: TextView? = null
    private var tv_max: TextView? = null
    private var tv_min: TextView? = null
    private var tv_name: TextView? = null
    private var tv_speed: TextView? = null
    private var tv_sunrise_time: TextView? = null
    private var tv_sunset_time: TextView? = null
    private var iv_main: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tv_main = findViewById(R.id.tv_main)
        tv_main_description = findViewById(R.id.tv_main_description)
        tv_temp = findViewById(R.id.tv_temp)
        tv_humidity = findViewById(R.id.tv_humidity)
        tv_country = findViewById(R.id.tv_country)
        tv_max = findViewById(R.id.tv_max)
        tv_min = findViewById(R.id.tv_min)
        tv_name = findViewById(R.id.tv_name)
        tv_speed = findViewById(R.id.tv_speed)
        tv_sunrise_time = findViewById(R.id.tv_sunrise_time)
        tv_sunset_time = findViewById(R.id.tv_sunset_time)
        iv_main = findViewById(R.id.iv_main)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (!isLocationEnabled()){
            Toast.makeText(this,"Your location provider is turned off. Please turn it.",Toast.LENGTH_SHORT).show()

            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {

            Dexter.withActivity(this)
                .withPermissions(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if (report!!.areAllPermissionsGranted()){
                            requestLocationData()
                        }
                        if (report.isAnyPermissionPermanentlyDenied){
                            Toast.makeText(this@MainActivity,"You have denied location permission. Please enable them as it is mandatory for the app to work.",Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        showRationalDialogForPermission()
                    }
                })
                .onSameThread()
                .check()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData() {

        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper())
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            Toast.makeText(this@MainActivity,"callback",Toast.LENGTH_SHORT).show()
            val mLastLocation: Location? = locationResult.lastLocation
            val latitude = mLastLocation?.latitude
            Log.i("Current Latitude","$latitude")

            val longitude = mLastLocation?.longitude
            Log.i("Current Longitude","$longitude")

            getLocationWeatherDetails(latitude!!,longitude!!)
        }
    }

    private fun getLocationWeatherDetails(latitude: Double, longitude: Double){
        if (Contants.isNetworkAvailable(this)){

            val retrofit : Retrofit = Retrofit.Builder()
                .baseUrl(Contants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service : WeatherService = retrofit.create<WeatherService>(WeatherService::class.java)

            val listCall: Call<WeatherResponse> = service.getWeather(
                latitude, longitude, Contants.METRIC_UNIT, Contants.APP_ID
            )

            showCustomProgressDialog()

            listCall.enqueue(object : Callback<WeatherResponse>{
                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    hideProgressDialog()
                    if (response.isSuccessful){
                        val weatherList: WeatherResponse? = response.body()
                        Log.i("Response Result","$weatherList")
                        setupUI(weatherList!!)
                    } else {
                        val rc = response.code()
                        when(rc){
                            400 -> {
                                Log.e("Error 400","Bad Connection")
                            }
                            404 -> {
                                Log.e("Error 404","Not Found")
                            }
                            else -> {
                                Log.e("Error","Generic Error")
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    hideProgressDialog()
                    Log.e("Error",t.message.toString())
                }

            })

        } else {
            Toast.makeText(this,"No internet connection available",Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRationalDialogForPermission(){
        AlertDialog.Builder(this)
            .setMessage("It looks like you have turned off permissions required for this features. It can be enabled under Application Settings")
            .setPositiveButton("GO TO SETTINGS"){ _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException){
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") {
                dialog, _ -> dialog.dismiss()
            }.show()
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun showCustomProgressDialog(){
        mProgressDialog = Dialog(this)
        mProgressDialog!!.setContentView(R.layout.dialog_custom_progress)
        mProgressDialog!!.show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.action_refresh -> {
                Toast.makeText(this,"hi",Toast.LENGTH_SHORT).show()
                requestLocationData()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun hideProgressDialog() {
        if (mProgressDialog!=null){
            mProgressDialog!!.dismiss()
        }
    }

    private fun setupUI(weatherList: WeatherResponse){
        for (i in weatherList.weather.indices){
            Log.i("Weather Name",weatherList.weather.toString())

            tv_main?.text = weatherList.weather[i].main
            tv_main_description?.text = weatherList.weather[i].description
            tv_temp?.text = weatherList.main.temp.toString() + getUnit(application.resources.configuration.locales.toString())
            tv_humidity?.text = weatherList.main.humidity.toString() + " per cent"
            tv_min?.text = weatherList.main.temp_min.toString() + " min"
            tv_max?.text = weatherList.main.temp_max.toString() + " max"
            tv_speed?.text = weatherList.wind.speed.toString()
            tv_name?.text = weatherList.name
            tv_country?.text = weatherList.sys.country
            tv_sunrise_time?.text = unixTime(weatherList.sys.sunrise.toLong())
            tv_sunset_time?.text = unixTime(weatherList.sys.sunset.toLong())

            // Here we update the main icon
            when (weatherList.weather[i].icon) {
                "01d" -> iv_main?.setImageResource(R.drawable.sunny)
                "02d" -> iv_main?.setImageResource(R.drawable.cloud)
                "03d" -> iv_main?.setImageResource(R.drawable.cloud)
                "04d" -> iv_main?.setImageResource(R.drawable.cloud)
                "04n" -> iv_main?.setImageResource(R.drawable.cloud)
                "10d" -> iv_main?.setImageResource(R.drawable.rain)
                "11d" -> iv_main?.setImageResource(R.drawable.storm)
                "13d" -> iv_main?.setImageResource(R.drawable.snowflake)
                "01n" -> iv_main?.setImageResource(R.drawable.cloud)
                "02n" -> iv_main?.setImageResource(R.drawable.cloud)
                "03n" -> iv_main?.setImageResource(R.drawable.cloud)
                "10n" -> iv_main?.setImageResource(R.drawable.cloud)
                "11n" -> iv_main?.setImageResource(R.drawable.rain)
                "13n" -> iv_main?.setImageResource(R.drawable.snowflake)
            }

        }
    }

    private fun getUnit(value: String): String? {
        Log.i("unitttttt", value)
        var value = "°C"
        if ("US" == value || "LR" == value || "MM" == value) {
            value = "°F"
        }
        return value
    }

    private fun unixTime(timex: Long): String? {
        val date = Date(timex * 1000L)
        @SuppressLint("SimpleDateFormat") val sdf =
            SimpleDateFormat("HH:mm:ss")
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }
}