package com.example.helpmeplease

import android.Manifest
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Process.THREAD_PRIORITY_BACKGROUND
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import android.telephony.SmsManager
import android.widget.Toast

class SendLocationService: Service() {

    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRes: Location
    private var phoneNumber: String = ""

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    override fun onCreate() {
        super.onCreate()

        //setUp callback function
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return

                onLocationReceived(locationResult.lastLocation)

            }
        }

        //initialize location services client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        //initialize request object
        createLocationRequest()
        getLastLocation()
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        requestLocationUpdates()

        if (intent != null) {
            phoneNumber = intent.getStringExtra("NUMBER")!!
            println("onStartCommand(): number received")
        }

        Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();
//        val notification: Notification = Notification()
//        startForeground(notification, FOREGROUND_SERVICE_TYPE_LOCATION
        return START_NOT_STICKY // leave it in the started state but don't retain this delivered intent. Later the system will try to re-create the service
    }
    
    override fun onDestroy() {

        fusedLocationClient.removeLocationUpdates(locationCallback)
        println("Stopping Service!")
        stopForeground(true)
        stopSelf()
        super.onDestroy()
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.create()?.apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }!!


    }

    private fun onLocationReceived(lastLocation: Location){
        locationRes = lastLocation
        val smsManager = SmsManager.getDefault()

        println("Location Received!: longitude: ${locationRes.longitude} | latitude: ${locationRes.latitude}")
        val msgString =  "I'M IN DANGER! \nMy location is: " +
                "\nlongitude: ${locationRes.longitude} " +
                "\nlatitude: ${locationRes.latitude}"

        smsManager.sendTextMessage(phoneNumber, null, msgString, null, null)
    }


    private  fun getLastLocation(){


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location : Location? ->
                        if (location != null) {
                            locationRes = location
                            println("the location is ${location.toString()}")
                        }else {
                            println("ERROR!!!!! COULD NOT FECTH LOCATION IN getLastLocation")
                        }
        }


    }

    private fun requestLocationUpdates(){
        println("Requesting for updates...")
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
           println("ERROR!! could not complete request for updates No Location Permission")
            return
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        println("Updates Requested...")
    }

}