package dev.dotworld.telemetrydata

import android.Manifest
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle

import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat

class LocationTrack constructor(context: Context) :LocationListener{
private var TAG=LocationTrack::class.java.simpleName

    private var isGps=false
    private var checkInternet=false
    private var isLOcation=false
    var loc: Location? = null
    var latitude = 0.0
    var longitude = 0.0
   var context:Context = context
    lateinit var locationManager: LocationManager
    private val MIN_DISTANCE_CHANGE_FOR_UPDATES: Float = 10f


    private val MIN_TIME_BW_UPDATES = (1000 * 60 * 1).toLong()

init {
    getLocationData()
}
fun getLocationData(){
    try {
        Log.d(TAG, "getLocationData: ")
        locationManager= context.getSystemService(LOCATION_SERVICE) as LocationManager
        isGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        checkInternet= locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (isGps && checkInternet){
            Log.d(TAG, "getLocationData: isGps and internet")
            isLOcation=true
            if (isGps){
                Log.d(TAG, "getLocationData: gps on")
                if (ActivityCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(context,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES,
                        this
                    )

                    loc =locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    Log.d(TAG, "getLocationData: "+loc.toString())
                    if (loc!=null){
                        latitude = loc!!.latitude
                        longitude = loc!!.longitude
                    }
                }else{
                    Log.d(TAG, "getLocationData: permission issue")
                }

            }
        }
    }catch (e: Exception){
        Log.e(TAG, "getLocationData: $e")
    }
}
    @JvmName("getLongitude1")
    fun getLongitude(): Double {
        if (loc != null) {
            longitude = loc!!.longitude
        }
        return longitude
    }

    @JvmName("getLatitude1")
    fun getLatitude(): Double {
        if (loc != null) {
            latitude = loc!!.latitude
        }
        return latitude
    }

    fun canGetLocation(): Boolean {
        return this.isLOcation
    }

    override fun onLocationChanged(location: Location) {
        Log.d(TAG, "onLocationChanged:latitude "+location.latitude)
        Log.d(TAG, "onLocationChanged: longitude"+location.longitude)
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

    }

    override fun onProviderEnabled(provider: String) {
        try {
            if (isLOcation){
                Log.d(TAG, "onProviderEnabled: ")
                super.onProviderEnabled(provider)
            }

        }catch (e:java.lang.Exception){
            Log.e(TAG, "onProviderEnabled: unavailable location" )
        }
    }

    override fun onProviderDisabled(provider: String) {
        isLOcation=false
    }
}