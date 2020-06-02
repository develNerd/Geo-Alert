package org.flepper.geoalert.emergency

import android.Manifest
import android.content.Intent
import android.content.IntentSender
import android.graphics.Point
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.transition.TransitionManager
import android.util.Log
import android.view.Display
import android.view.View
import android.widget.CompoundButton
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bambuser.broadcaster.BroadcastPlayer
import com.bambuser.broadcaster.PlayerState
import com.bambuser.broadcaster.SurfaceViewWithAutoAR
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import kotlinx.android.synthetic.main.activity_emergency_unit_map.*
import okhttp3.*
import org.flepper.geoalert.*
import org.flepper.geoalert.BuildConfig
import org.flepper.geoalert.R
import org.flepper.geoalert.messages.MessageAdapter
import org.flepper.geoalert.messages.MessageObject
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.DateFormat
import java.util.*
import kotlin.collections.ArrayList

class EmergencyUnitMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private val APPLICATION_ID =
        "Xezut3PbTkF0ZfyeVvbmgg"
    private val API_KEY = "4ZMwefmXutkMybnVcei17V"

    var URL:String? = null

    var firstsize:Int =0

    var i:Int = 0

    var secondSize:Int =0

    private var assigned:Boolean = false



    private val mHistoryLayoutManager: RecyclerView.LayoutManager? = null


    var driverName:String? = null

    var locationLat = 0.0
    var locationLng = 0.0

    var path: Polyline? = null

    val civLocation:Location = Location("")

    // ...
    private val broadcasterObserver = object : BroadcastPlayer.Observer {
        override fun onStateChange(playerState: PlayerState) {
            if (live_label != null) live_label.text = "Status: $playerState"
        }

        override fun onBroadcastLoaded(live: Boolean, width: Int, height: Int) {

            val size = getScreenSize()
            val screenAR = size!!.x / size.y.toFloat()
            val videoAR = width / height.toFloat()
            val arDiff = screenAR - videoAR
            mVideoSurface!!.setCropToParent(Math.abs(arDiff) < 0.2)
        }

    }

    private lateinit var mMap: GoogleMap

    private var Latitude:String? = null

    private var Longitude:String? = null

    private var mLastUpdateTime: String? = null

    private var customerId = ""
    private val destinationLatLng: LatLng? = null
    private  var pickupLatLng:LatLng? = null
    private var rideDistance = 0f

    private val TAG = EmergencyUnitMapActivity::class.java.simpleName

    private var speed:Float? = null

    var mLastLocation: Location? = null


    private var mAuth: FirebaseAuth? = null
    private var mCustomerDatabase: DatabaseReference? = null

    private var userId: String? = null

    private var userID: String? = null

    private var distance:Float? = null


    // location updates interval - 10sec
    private val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 5000

    private val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS: Long = 5000

    private val REQUEST_CHECK_SETTINGS = 100

    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var mSettingsClient: SettingsClient? = null
    private var mLocationRequest: LocationRequest? = null
    private var mLocationSettingsRequest: LocationSettingsRequest? = null
    private var mLocationCallback: LocationCallback? = null
    private var mCurrentLocation: Location? = null
    private var pickUpLocation:Location? = null

    private var status = 0


    private var myLatLy:LatLng? = null

    var mVideoSurface: SurfaceViewWithAutoAR? = null
    var mPlayerStatusTextView: TextView? = null
    var mPlayerContentView: View? = null

    private var viewEnabled = false


    private var mWorkingswitch:Switch? = null
    private var mRequestingLocationUpdates: Boolean? = null

    val api = MyApi()

    var duration:String? = null


    var layoutManager :LinearLayoutManager? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency_unit_map)

        mVideoSurface = findViewById(R.id.VideoSurfaceView);
        mPlayerStatusTextView = findViewById(R.id.live_label);
        mPlayerContentView = findViewById(R.id.PlayerContentView);


        mAuth = FirebaseAuth.getInstance()
        userID = mAuth!!.currentUser!!.uid


        allLayout.isEnableScrolling = false


        list_of_messages.isNestedScrollingEnabled = false
        list_of_messages.setHasFixedSize(true)
        layoutManager = LinearLayoutManager(this)
        layoutManager?.stackFromEnd = true
        list_of_messages.layoutManager = layoutManager
        val mAdapter =
            MessageAdapter(this@EmergencyUnitMapActivity, getDataSetHistory())
        list_of_messages.adapter = mAdapter




        radioButton.setOnCheckedChangeListener { radioButton, isChecked ->
            if (isChecked){
                resultsHistory.clear()
                getUserHistoryIds()
                Handler(Looper.getMainLooper()).postDelayed({

                    TransitionManager.beginDelayedTransition(transitionsContainer)
                    getLatestResourceUri()
                    maplayout.post {
                        allLayout.fullScroll(View.FOCUS_UP)
                        maplayout.fullScroll(View.FOCUS_RIGHT)
                    }

                }, 100)
            }else{
                Handler(Looper.getMainLooper()).postDelayed({

                    TransitionManager.beginDelayedTransition(transitionsContainer)
                    maplayout.post {
                        allLayout.fullScroll(View.FOCUS_UP)
                        maplayout.fullScroll(View.FOCUS_LEFT)

                    }


                }, 100)
            }
        }

        send.setOnClickListener {
            sendMessage()
        }


        maplayout.isEnableScrolling = false

        cancel.setOnClickListener {
            recordRide()
            endRide()
            pickupMarker!!.remove()
            MyMarker!!.remove()
            cancel.visibility = View.GONE
            viewMessage.visibility = View.GONE

        }

        viewMessage.setOnClickListener {
            if (viewEnabled == false){
                allLayout.post {
                    viewEnabled = true
                    viewMessage.text = "View Map"
                    allLayout.fullScroll(View.FOCUS_DOWN)
                    resultsHistory.clear()
                    getUserHistoryIds()
                }
            }else{
                allLayout.post {
                    viewEnabled = false
                    viewMessage.text = "View message"
                    allLayout.fullScroll(View.FOCUS_UP)
                }
            }
        }





        mWorkingswitch = findViewById(R.id.workingswitch) as Switch
        // mWorkingswitch.setChecked(true);
        // mWorkingswitch.setChecked(true);
        mWorkingswitch?.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                resultsHistory.clear()
                getUserHistoryIds()
                connectDriver()
                mWorkingswitch?.setText("Working")
            } else {

                disconnectDriver()
                mWorkingswitch?.setText("Not Working")
            }
        })

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)

        init()


        Dexter.withActivity(this)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {
                    mRequestingLocationUpdates = true
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) {
                    if (response.isPermanentlyDenied) {
                        // open device settings when the permission is
                        // denied permanently
                        openSettings()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            }).check()

        getAssignedCustomer()

    }

    override fun onStop() {
        super.onStop()
        endRide()
        disconnectDriver()

    }



    override fun onMapReady(googleMap: GoogleMap) {


        mMap = googleMap
        if (mCurrentLocation != null) {
            val sydney = LatLng((Latitude!!).toDouble(), Longitude!!.toDouble())
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 15.0f))
            mMap.moveCamera(CameraUpdateFactory.zoomTo(15.0f))

        }


    }

    private fun FetchRideInformation(rideKey: String) {
        val historyDatabase =
            FirebaseDatabase.getInstance().reference.child("messages").child(rideKey)
        historyDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val   size = dataSnapshot.childrenCount.toInt()
                    val rideId = dataSnapshot.key
                    var timestamp: Long? = 0L
                    var mesage:String = ""
                    var  user = ""
                    for (child in dataSnapshot.children) {
                        if (child.key == "timestamp") {
                            timestamp = java.lang.Long.valueOf(child.value.toString())
                        }
                        if (child.key == "message") {
                            mesage = child.value.toString()
                        }
                        if (child.key == "username") {
                            user = child.value.toString()
                        }



                    }

                    val obj = MessageObject(getDate(timestamp!!), mesage, user)
                    resultsHistory.add(obj)
                    list_of_messages.adapter?.notifyDataSetChanged()




                }
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun getDate(time: Long): String? {
        val cal = Calendar.getInstance(Locale.getDefault())
        cal.timeInMillis = time * 1000
        return android.text.format.DateFormat.format("MM-dd-yyyy hh:mm", cal).toString()
    }

    private fun getUserHistoryIds() {
        layoutManager?.reverseLayout = true
        list_of_messages.layoutManager = layoutManager
        val userHistoryDatabase =
            FirebaseDatabase.getInstance().reference.child("Users").child("EmergencyUnit")
                .child(userID!!).child("messages")
        userHistoryDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (message in dataSnapshot.children) {
                        firstsize = message.childrenCount.toInt()

                            FetchRideInformation(message.key!!)

                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private val resultsHistory  = java.util.ArrayList<MessageObject>()

    private fun getDataSetHistory(): List<MessageObject> {
        return resultsHistory.asReversed()
    }
    var lineOptions:PolylineOptions? = null
    private fun updateMapLocation(location: Location?){
        getLatestResourceUri()

        if (applicationContext != null) {
            mLastLocation = location
            if (customerId != "") {
                rideDistance += mLastLocation!!.distanceTo(location)
            }
            if (customerId == "") {
                path?.remove()
            }
            path?.remove()

            if (customerId != "") {
                val mLastLatlng = LatLng(
                    mLastLocation!!.latitude,
                    mLastLocation!!.longitude
                )

                civLocation.latitude = locationLat
                civLocation.longitude = locationLng
                if (civLocation != null) {
                    distance = mLastLocation!!.distanceTo(civLocation)
                    val civLatlng = LatLng(
                        civLocation.latitude,
                        civLocation.longitude
                    )
                    name.text =
                        "You have been Connected with $driverName. You are ${(distance!! / 1000).toString()} km away from your target Please follow all Safety measures to secure the Civilian Thanks. "

                    val ll = MyLatlng(mLastLocation!!.latitude, mLastLocation!!.longitude)
                    val dd = MyLatlng(civLocation!!.latitude, civLocation!!.longitude)

                    path = mMap.addPolyline(
                        PolylineOptions()
                            .add(
                                LatLng(mLastLocation!!.latitude, mCurrentLocation!!.longitude),
                                LatLng(
                                    civLocation.latitude
                                    , civLocation.longitude
                                )

                            )
                    )

                    // Style the polyline

                    // Style the polyline
                    path?.width = 10f
                    path?.color = R.color.colorPrimaryDark

                    if (myLatLy != null) {
                        myLatLy = mLastLatlng
                        muDistace.text = "Time to reach Civilian is approximately $duration"


                    }
                }


            }
            mMap.uiSettings.isCompassEnabled = false
            mMap.uiSettings.isZoomControlsEnabled = false
            mMap.moveCamera(
                CameraUpdateFactory.newLatLng(
                    LatLng(
                        location?.latitude ?: 0.0,
                        location?.longitude ?: 0.0
                    )
                )
            )
            mMap.addCircle(
                CircleOptions().center(
                    LatLng(
                        location?.latitude ?: 0.0,
                        location?.longitude ?: 0.0
                    )
                ).clickable(true).radius(0.1).fillColor(R.color.colorPrimaryDark)
            )
            mMap.isIndoorEnabled = true
            //mMap.moveCamera(CameraUpdateFactory.zoomTo(15.0f))
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isCompassEnabled = false
            val latLng = LatLng(location!!.latitude, location!!.longitude)
            //mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
            // mMap.animateCamera(CameraUpdateFactory.zoomTo(8.00f));
            mMap.setMaxZoomPreference(15f)
            mMap.setMinZoomPreference(11f)
            if (!radioButton.isChecked) {
                userId = FirebaseAuth.getInstance().currentUser!!.uid
                val refAvailable =
                    FirebaseDatabase.getInstance().getReference("espAvailable")
                val refWorking =
                    FirebaseDatabase.getInstance().getReference("driversWorking")
                val geoFireAvailable = GeoFire(refAvailable)
                val geoFireWorking = GeoFire(refWorking)
                when (customerId) {
                    "" -> {
                        geoFireWorking.removeLocation(userId)
                        geoFireAvailable.setLocation(
                            userId,
                            GeoLocation(location!!.latitude, location!!.longitude)
                        )
                    }
                    else -> {
                        geoFireAvailable.removeLocation(userId)
                        geoFireWorking.setLocation(
                            userId,
                            GeoLocation(location!!.latitude, location!!.longitude)
                        )
                    }
                }
            }
        }

    }

    private fun openSettings() {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts(
            "package",
            BuildConfig.APPLICATION_ID, null
        )
        intent.data = uri
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }





    private fun init() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mSettingsClient = LocationServices.getSettingsClient(this)

        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)
                locationResult ?: return


                resultsHistory.clear()
                getUserHistoryIds()

                for (location in locationResult.locations){
                    // Update UI with location data
                    // ...
                    mCurrentLocation = locationResult.lastLocation
                    mLastUpdateTime = DateFormat.getTimeInstance().format(Date())

                    //  showProgressDialog()
                    if (location != null) {

                        updateMapLocation(location)
                        getLatestResourceUri()
                        Latitude = location.latitude.toString()
                        Longitude = location.longitude.toString()

                    }
                }
                // location is received


            }
        }

        mRequestingLocationUpdates = false

        mLocationRequest = LocationRequest()
        mLocationRequest!!.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS)
        mLocationRequest!!.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS)
        mLocationRequest!!.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest!!)
        mLocationSettingsRequest = builder.build()

        // **************************
        builder.setAlwaysShow(true) // this is the key ingredient


    }

    private fun getAssignedCustomer() {
        val driverId = FirebaseAuth.getInstance().currentUser!!.uid
        val assignedCustomerRef =
            FirebaseDatabase.getInstance().reference.child("Users").child("EmergencyUnit")
                .child(driverId).child("customerRequest").child("customerRideId")
        assignedCustomerRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    status = 1
                    customerId = dataSnapshot.value.toString()
                    getAssignedCustomerPickupLocation()
                    getAssignedCustomerInfo()
                } else {
                    endRide()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    var pickupMarker: Marker? = null
    var MyMarker: Marker? = null

    private var assignedCustomerPickupLocationRef: DatabaseReference? = null
    private var assignedCustomerPickupLocationRefListener: ValueEventListener? = null
    private fun getAssignedCustomerPickupLocation() {
        cancel.visibility = View.VISIBLE
        viewMessage.visibility = View.VISIBLE
        assignedCustomerPickupLocationRef =
            FirebaseDatabase.getInstance().reference.child("customerRequest").child(customerId)
                .child("l")
        assignedCustomerPickupLocationRefListener =
            assignedCustomerPickupLocationRef!!.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists() && customerId != "") {
                        if (mCurrentLocation != null) {



                        }
                        val map =
                            dataSnapshot.value as List<Any?>?
                         locationLat = 0.0
                         locationLng = 0.0
                        if (map!![0] != null) {
                            locationLat = map[0].toString().toDouble()
                        }
                        if (map[1] != null) {
                            locationLng = map[1].toString().toDouble()
                        }
                        civLocation?.latitude = locationLat
                        civLocation?.longitude = locationLng
                        pickupLatLng = LatLng(locationLat, locationLng)
                       pickupMarker = mMap.addMarker(
                            MarkerOptions().position(pickupLatLng!!).title("Civilian location")
                       )
                        pickUpLocation?.longitude = locationLng
                        pickUpLocation?.latitude = locationLat

                        MyMarker = mMap.addMarker(
                            MarkerOptions().position(
                                LatLng(
                                    mCurrentLocation!!.latitude,
                                    mCurrentLocation!!.longitude
                                )
                            ).title("Start Location")
                        )




                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
    }


    private fun endRide() {
        path?.remove()
        name.text = ""
        muDistace.text = ""
        pickupMarker?.remove()
        MyMarker?.remove()

        mAuth = FirebaseAuth.getInstance()
        userID = mAuth!!.currentUser!!.uid
        mCustomerDatabase =
            FirebaseDatabase.getInstance().reference.child("Users").child("Civilians")
                .child(userID!!)
        val userInfo =
            HashMap<String?, Any?>()
        userInfo["message"] = ""
        mCustomerDatabase!!.updateChildren(userInfo)
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        val driverRef =
            FirebaseDatabase.getInstance().reference.child("Users").child("EmergencyUnit")
                .child(userId).child("customerRequest")
        driverRef.removeValue()
        val ref = FirebaseDatabase.getInstance().getReference("customerRequest")
        val geoFire = GeoFire(ref)
        geoFire.removeLocation(customerId)
        customerId = ""
        rideDistance = 0f
        if (pickupMarker != null) {
            pickupMarker!!.remove()
        }
        if (MyMarker != null) {
            MyMarker!!.remove()
        }
        if (assignedCustomerPickupLocationRefListener != null) {
            assignedCustomerPickupLocationRef!!.removeEventListener(
                assignedCustomerPickupLocationRefListener!!
            )
            connectDriver()
        }

    }

    private fun getAssignedCustomerInfo() {
        val mCustomerDatabase =
            FirebaseDatabase.getInstance().reference.child("Users").child("Civilians")
                .child(customerId)
        mCustomerDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.childrenCount > 0) {


                    civLocation?.latitude = locationLat
                    civLocation?.longitude = locationLng

                    val map =
                        dataSnapshot.value as Map<String, Any?>?
                    if (map?.get("name") != null) {
                        driverName = map["name"].toString()
                        name.text =
                            "You have been Connected with ${map["name"].toString()}. You are  away from your target Please follow all Safety measures to secure the Civilian Thanks. "

                        if (speed != null &&distance != null) {
                            val time = distance!!/speed!!
                            muDistace.text =
                                "You are supposed to arrive at ${map["name"].toString()} location in the next $time"
                        }
                        }


                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun recordRide() {

        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        val driverRef =
            FirebaseDatabase.getInstance().reference.child("Users").child("EmergencyUnit")
                .child(userId).child("history")


        val customerRef =
            FirebaseDatabase.getInstance().reference.child("Users").child("Civilians")
                .child(customerId).child("history")
        val historyRef =
            FirebaseDatabase.getInstance().reference.child("history")
        val requestId = historyRef.push().key
        driverRef.child(requestId!!).setValue(true)
        customerRef.child(requestId).setValue(true)
        val map = HashMap<String?, Any?>()
        map["driver"] = userId
        map["customer"] = customerId
        map["rating"] = 0
        map["timestamp"] = getCurrentTimestamp()
        if (pickupLatLng != null) {
            map["location/from/lat"] = pickupLatLng!!.latitude
            map["location/from/lng"] = pickupLatLng!!.longitude
        }
        // map.put("location/to/lat", destinationLatLng.latitude);
        //  map.put("location/to/lng", destinationLatLng.longitude);
        map["distance"] = rideDistance
        historyRef.child(requestId).updateChildren(map)
    }

    private fun sendMessage(){


        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        val usrName  = FirebaseAuth.getInstance().currentUser!!.displayName
        val driverRef =
            FirebaseDatabase.getInstance().reference.child("Users").child("EmergencyUnit")
                .child(userId).child("messages")
        val customerRef =
            FirebaseDatabase.getInstance().reference.child("Users").child("Civilians")
                .child(customerId).child("messages")
        val historyRef =
            FirebaseDatabase.getInstance().reference.child("messages")
        val requestId = historyRef.push().key
        driverRef.child(requestId!!).setValue(true)
        customerRef.child(requestId).setValue(true)
        val map = HashMap<String?, Any?>()
        map["emergencyUnit"] = userId
        map["civilian"] = customerId
        map["timestamp"] = getCurrentTimestamp()
        map["username"] = driverName
        map["message"] = message_field.text.toString()

        historyRef.child(requestId).updateChildren(map)
        resultsHistory.clear()
        getUserHistoryIds()




    }

    private fun startLocationUpdates() {
        mSettingsClient!!
            .checkLocationSettings(mLocationSettingsRequest)
            .addOnSuccessListener(this) {
                Log.i(TAG, "All location settings are satisfied.")
                mFusedLocationClient!!.requestLocationUpdates(
                    mLocationRequest,
                    mLocationCallback, Looper.myLooper()
                )
                if (mCurrentLocation != null) {
                    Latitude = mCurrentLocation!!.latitude.toString()
                    Longitude = mCurrentLocation!!.longitude.toString()

                }
            }
            .addOnFailureListener(this) { e ->
                val statusCode = (e as ApiException).statusCode
                when (statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        Log.i(
                            TAG,
                            "Location settings are not satisfied. Attempting to upgrade " + "location settings "
                        )
                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the
                            // result in onActivityResult().
                            val rae = e as ResolvableApiException
                            rae.startResolutionForResult(this, REQUEST_CHECK_SETTINGS)
                        } catch (sie: IntentSender.SendIntentException) {
                            Log.i(TAG, "PendingIntent unable to execute request.")
                        }

                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        val errorMessage =
                            "Location settings are inadequate, and cannot be " + "fixed here. Fix in Settings."
                        Log.e(TAG, errorMessage)

                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }

            }
    }

    private fun getCurrentTimestamp(): Long? {
        return System.currentTimeMillis() / 1000
    }


    private fun connectDriver() {

        startLocationUpdates()
    }



    private fun disconnectDriver() {
        mSettingsClient!!
            .checkLocationSettings(mLocationSettingsRequest)
            .addOnSuccessListener(this) {
                Log.i(TAG, "All location settings are satisfied.")
                mFusedLocationClient!!.removeLocationUpdates(
                    mLocationCallback
                )

            }
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        val ref = FirebaseDatabase.getInstance().getReference("espAvailable")
        val geoFire = GeoFire(ref)
        geoFire.removeLocation(userId)
    }


    override fun onPause() {
        super.onPause()
        mOkHttpClient.dispatcher().cancelAll()
        mVideoSurface = null
        if (mBroadcastPlayer != null)
            mBroadcastPlayer!!.close();
        mBroadcastPlayer = null;
    }

    override fun onResume() {
        super.onResume()
        connectDriver()


        mVideoSurface = findViewById(R.id.VideoSurfaceView)
        mPlayerStatusTextView!!.text = "Loading latest broadcast"
        getLatestResourceUri()
    }

    fun initPlayer(resourceUri: String?) {
        if (resourceUri == null) {
            if (mPlayerStatusTextView != null) mPlayerStatusTextView!!.text =
                "Could not get info about latest broadcast"
            return
        }
        if (mVideoSurface == null) {
            // UI no longer active
            return
        }
        if (mBroadcastPlayer != null) mBroadcastPlayer!!.close()
        mBroadcastPlayer =
            BroadcastPlayer(this, resourceUri, APPLICATION_ID, broadcasterObserver)
        mBroadcastPlayer!!.setSurfaceView(mVideoSurface)
        mBroadcastPlayer!!.load()
    }

    private fun getLatestResourceUri() {
        val request: Request = Request.Builder()
            .url("https://api.bambuser.com/broadcasts")
            .addHeader("Accept", "application/vnd.bambuser.v1+json")
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $API_KEY")
            .get()
            .build()
        mOkHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call?, e: IOException) {
                runOnUiThread {
                    if (mPlayerStatusTextView != null) mPlayerStatusTextView!!.text =
                        "Http exception: $e"
                }
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call?, response: Response) {
                val body: String = response.body()!!.string()
                var resourceUri: String? = null
                try {
                    val json = JSONObject(body)
                    val results: JSONArray = json.getJSONArray("results")
                    val latestBroadcast: JSONObject = results.optJSONObject(0)
                    resourceUri = latestBroadcast.optString("resourceUri")
                } catch (ignored: Exception) {
                }
                val uri = resourceUri
                runOnUiThread { initPlayer(uri) }
            }
        })
    }


    private fun getScreenSize(): Point? {
        if (mDefaultDisplay == null) mDefaultDisplay = windowManager.defaultDisplay
        val size = Point()
        try {
            // this is officially supported since SDK 17 and said to work down to SDK 14 through reflection,
            // so it might be everything we need.
            mDefaultDisplay!!.javaClass.getMethod("getRealSize", Point::class.java)
                .invoke(mDefaultDisplay, size)
        } catch (e: java.lang.Exception) {
            // fallback to approximate size.
            mDefaultDisplay!!.getSize(size)
        }
        return size
    }


    private val mOkHttpClient: OkHttpClient = OkHttpClient()

    var mBroadcastPlayer: BroadcastPlayer? = null

    var mDefaultDisplay: Display? = null


     fun getDirectionalUrl(origin:LatLng, dest: LatLng) : String? {

        return ""

    }




    override fun onDestroy() {
        super.onDestroy()
        endRide()
        disconnectDriver()
    }



    public fun decodePolyline(encoded: String): List<LatLng> {

        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val latLng = LatLng((lat.toDouble() / 1E5),(lng.toDouble() / 1E5))
            poly.add(latLng)
        }

        return poly
    }

    fun paramJson(paramIn: String): String? {
        var paramIn = paramIn
        paramIn = paramIn.replace("=".toRegex(), "\":\"")
        paramIn = paramIn.replace("&".toRegex(), "\",\"")
        return "{\"$paramIn\"}"
    }
}
