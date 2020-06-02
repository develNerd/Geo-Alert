package org.flepper.geoalert.civilian

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.transition.TransitionManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bambuser.broadcaster.BroadcastStatus
import com.bambuser.broadcaster.Broadcaster
import com.bambuser.broadcaster.CameraError
import com.bambuser.broadcaster.ConnectionError
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQuery
import com.firebase.geofire.GeoQueryEventListener
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import kotlinx.android.synthetic.main.activity_civilian_map.*
import org.flepper.geoalert.*
import org.flepper.geoalert.BuildConfig
import org.flepper.geoalert.R
import org.flepper.geoalert.messages.MessageAdapter
import org.flepper.geoalert.messages.MessageObject
import java.text.DateFormat
import java.util.*

class CivilianMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private val activity = this

    private val APPLICATION_ID = "Xezut3PbTkF0ZfyeVvbmgg"

    private val handler = Handler()
    private var thread: Thread? = null
    private var isCounting = false

    private val broadcaster by lazy {
        Broadcaster(this, APPLICATION_ID, broadcasterObserver)
    }


    private val broadcasterObserver = object : Broadcaster.Observer {
        override fun onConnectionStatusChange(broadcastStatus: BroadcastStatus) {
            Log.i("Mybroadcastingapp", "Received status change: $broadcastStatus")
            if (broadcastStatus == BroadcastStatus.STARTING) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            if (broadcastStatus == BroadcastStatus.IDLE) {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            video.text = if (broadcastStatus == BroadcastStatus.IDLE) "Broadcast" else "Disconnect"

        }
        override fun onStreamHealthUpdate(i: Int) {}
        override fun onConnectionError(connectionError: ConnectionError, s: String?) {
            Log.i("Mybroadcastingapp","Received connection error: $connectionError, $s")
        }
        override fun onCameraError(cameraError: CameraError) {
            Log.i("Mybroadcastingapp","Received camera error: $cameraError")
        }
        override fun onChatMessage(s: String) {
            Log.i("Mybroadcastingapp","Received chat messsage: $s")
        }
        override fun onResolutionsScanned() {}
        override fun onCameraPreviewStateChanged() {}
        override fun onBroadcastInfoAvailable(s: String, s1: String) {
            Log.i("Mybroadcastingapp","Received broadcast info: $s, $s1")
        }
        override fun onBroadcastIdAvailable(id: String) {
            Log.i("Mybroadcastingapp","Received broadcast id: $id")
            toast(id)
        }
    }

    private var firstZise:Int = 0
    private var pickupMarker: Marker? = null

    private val destination: String? =
        null
    private  var requestService:kotlin.String? = null

    private var requestBol = false
    private var radius = 1
    private var driverFound = false
    private var driverFoundID: String? = null
    private var mLastUpdateTime: String? = null

   private var destinationLatLng = LatLng(0.0, 0.0)

    private lateinit var mMap: GoogleMap


    private val TAG = CivilianMapActivity::class.java.simpleName


    private var mAuth: FirebaseAuth? = null
    private var mCustomerDatabase: DatabaseReference? = null

    private var userID: String? = null
    // location updates interval - 10sec
    private val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 500


    private var successDialog:Dialog? = null

    private var i:Int = 0

    private var Latitude:String? = null

    private var Longitude:String? = null

    private lateinit var pickupLocation:LatLng

    // fastest updates interval - 5 sec
    // location updates will be received if another app is requesting the locations
    // than your app can handle
    private val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS: Long = 5000

    private val REQUEST_CHECK_SETTINGS = 5000

    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var mSettingsClient: SettingsClient? = null
    private var mLocationRequest: LocationRequest? = null
    private var mLocationSettingsRequest: LocationSettingsRequest? = null
    private var mLocationCallback: LocationCallback? = null
    private var mCurrentLocation: Location? = null

    private var mRequestingLocationUpdates: Boolean? = null

    private var viewEnabled = false

    private var customerOrDriver: String? = null
    private  var userId:kotlin.String? = null



    private var layoutManager:LinearLayoutManager? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_civilian_map)



        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)

        init()

        list_of_messages.isNestedScrollingEnabled = false
        list_of_messages.setHasFixedSize(true)
        layoutManager = LinearLayoutManager(this)
        layoutManager?.reverseLayout = true
        list_of_messages.layoutManager = layoutManager
        val mAdapter = MessageAdapter(this, getDataSetHistory())
        list_of_messages.adapter = mAdapter


        userId = FirebaseAuth.getInstance().currentUser!!.uid
        getUserHistoryIds()

        sendthemessage.setOnClickListener {
            SendMessage()
        }

        maplayout.isEnableScrolling = false


        successDialog = Dialog(this)

        nointernet = Dialog(this)

        scroll.isEnableScrolling = false


        sendMessage.setOnClickListener {
            if (!viewEnabled){
                scroll.post {
                    viewEnabled = true
                    sendMessage.text = "View Map"
                    scroll.fullScroll(View.FOCUS_DOWN)
                }
            }else{
                scroll.post {
                    viewEnabled = false
                    sendMessage.text = "Send a short message"
                    scroll.fullScroll(View.FOCUS_UP)
                }
            }

        }

        radioButton.setOnCheckedChangeListener { radioButton, isChecked ->
            if (isChecked){
                Handler(Looper.getMainLooper()).postDelayed({

                    TransitionManager.beginDelayedTransition(transitionsContainer)
                    maplayout.post {
                        maplayout.fullScroll(View.FOCUS_RIGHT)
                        scroll.fullScroll(View.FOCUS_UP)

                    }

                }, 100)
            }else{
                Handler(Looper.getMainLooper()).postDelayed({

                    TransitionManager.beginDelayedTransition(transitionsContainer)
                    maplayout.post {
                        maplayout.fullScroll(View.FOCUS_LEFT)
                        scroll.fullScroll(View.FOCUS_UP)

                    }


                }, 100)
            }
        }


        video.setOnClickListener {
            if (broadcaster.canStartBroadcasting()) {
                broadcaster.setSaveOnServer(false)
                broadcaster.startBroadcast()
                TransitionManager.beginDelayedTransition(transitionsContainer)
                maplayout.post {
                    maplayout.fullScroll(View.FOCUS_RIGHT)
                }
                startCounting()
            } else {
                broadcaster.stopBroadcast()
                TransitionManager.beginDelayedTransition(transitionsContainer)
                maplayout.post {
                    maplayout.fullScroll(View.FOCUS_LEFT)
                }
                stopCounting()

            }
        }






        Dexter.withActivity(this)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {
                    mRequestingLocationUpdates = true
                    startLocationUpdates()
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


        settings.setOnClickListener {
            Intent(this, CivilianSettingsActivity::class.java).also {
                startActivity(it)
            }
        }


        val checkInternetConnection = CheckInternetConnection(this)



        help.setOnClickListener {
            if(requestBol){
                endRide()
            }else if(mCurrentLocation.toString().isEmpty()){
                return@setOnClickListener
            }
            else if (!police.isChecked && !Ambulance.isChecked && !fire.isChecked){
                toastShort("Please Select an Esp")
                return@setOnClickListener

            }
            else if(!checkInternetConnection.isInternetAvailable()){
                showSuccesDialog()

            }
            else{
                val li: LayoutInflater = LayoutInflater.from(this)
                val view: View = li.inflate(R.layout.custompopup, null)
                val btnOkay: Button = view.findViewById(R.id.okay) as Button
                successDialog?.setContentView(view)
                successDialog?.setOnCancelListener {
                    successDialog?.dismiss()
                }
                successDialog?.show()
                btnOkay.setOnClickListener {
                    successDialog?.dismiss()
                    circularProgressBar.indeterminateMode = true
                    val selectId: Int = mRadioGroup.checkedRadioButtonId
                    val radioButton = findViewById(selectId) as RadioButton
                    if (!radioButton.isChecked) {
                        toastShort("Please Select ")
                        return@setOnClickListener
                    }


                    requestService = radioButton.text.toString()

                    requestBol = true

                    val userId = FirebaseAuth.getInstance().currentUser!!.uid

                    val ref =
                        FirebaseDatabase.getInstance().getReference("customerRequest")
                    val geoFire = GeoFire(ref)

                    geoFire.setLocation(
                        userId,
                        GeoLocation(mCurrentLocation!!.latitude, mCurrentLocation!!.longitude)
                    )


                    pickupLocation =
                        LatLng(mCurrentLocation!!.latitude, mCurrentLocation!!.longitude)
                    pickupMarker = mMap.addMarker(
                        MarkerOptions().position(pickupLocation).title("Help location")

                    )

                    getClosestDriver()


                }
                startLocationUpdates()
            }

        }

        val circularProgressBar = findViewById<CircularProgressBar>(R.id.circularProgressBar)
        circularProgressBar.apply {
            // Set Progress
            progress = 65f
            // or with animation
            setProgressWithAnimation(65f, 1000) // =1s

            // Set Progress Max
            progressMax = 200f

            // Set ProgressBar Color
            progressBarColor = R.color.colorPrimaryDark
            // or with gradient
            progressBarColorStart = R.color.colorPrimaryDark
            progressBarColorEnd = R.color.colorPrimaryDark
            progressBarColorDirection = CircularProgressBar.GradientDirection.TOP_TO_BOTTOM

            // Set background ProgressBar Color
            backgroundProgressBarColor = R.color.colorPrimaryDark
            // or with gradient
            backgroundProgressBarColorStart = Color.WHITE
            backgroundProgressBarColorEnd =  R.color.colorPrimaryDark
            backgroundProgressBarColorDirection = CircularProgressBar.GradientDirection.TOP_TO_BOTTOM

            // Set Width
            progressBarWidth = 7f // in DP
            backgroundProgressBarWidth = 3f // in DP

            // Other
            roundBorder = true
            startAngle = 180f
            progressDirection = CircularProgressBar.ProgressDirection.TO_RIGHT
        }
    }

    private fun startCounting() {
        isCounting = true
        live_label.text = getString(R.string.publishing_label, 0L.format(), 0L.format())
        live_label.visibility = View.VISIBLE
        val startedAt = System.currentTimeMillis()
        var updatedAt = System.currentTimeMillis()
        thread = Thread {
            while (isCounting) {
                if (System.currentTimeMillis() - updatedAt > 1000) {
                    updatedAt = System.currentTimeMillis()
                    handler.post {
                        val diff = System.currentTimeMillis() - startedAt
                        val second = diff / 1000 % 60
                        val min = diff / 1000 / 60
                        live_label.text = getString(R.string.publishing_label, min.format(), second.format())
                    }
                }
            }
        }
        thread?.start()
    }

    private fun Long.format(): String {
        return String.format("%02d", this)
    }

    private fun stopCounting() {
        isCounting = false
        live_label.text = ""
        live_label.visibility = View.GONE
        thread?.interrupt()
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
                    Lat.text = Latitude
                    longitude.text = Longitude
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

    var geoQuery: GeoQuery? = null
    private fun getClosestDriver() {
        val driverLocation =
            FirebaseDatabase.getInstance().reference.child("espAvailable")
        val geoFire = GeoFire(driverLocation)
        if (mCurrentLocation != null) {
            geoQuery = geoFire.queryAtLocation(
                GeoLocation(
                    mCurrentLocation!!.latitude,
                    mCurrentLocation!!.longitude
                ), radius.toDouble()
            )
            geoQuery!!.removeAllListeners()
        }
        geoQuery!!.addGeoQueryEventListener(object : GeoQueryEventListener {
            override fun onKeyEntered(key: String, location: GeoLocation) {
                if (!driverFound && requestBol) {
                    val mCustomerDatabase =
                        FirebaseDatabase.getInstance().reference.child("Users")
                            .child("EmergencyUnit").child(key)
                    mCustomerDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.exists() && dataSnapshot.childrenCount > 0) {
                                val driverMap =
                                    dataSnapshot.value as Map<String, Any>?
                                if (driverFound) {

                                    return
                                }
                                if (driverMap!!["service"] == requestService) {
                                    driverFound = true
                                    updateMapLocation(mCurrentLocation)
                                    driverFoundID = dataSnapshot.key
                                    val driverRef =
                                        FirebaseDatabase.getInstance().reference.child("Users")
                                            .child("EmergencyUnit").child(driverFoundID!!)
                                            .child("customerRequest")
                                    val customerId =
                                        FirebaseAuth.getInstance().currentUser!!.uid
                                    val map =
                                        HashMap<String?, Any?>()
                                    map["customerRideId"] = customerId
                                    map["message"] = destination
                                    map["destinationLat"] = destinationLatLng.latitude
                                    map["destinationLng"] = destinationLatLng.longitude
                                    driverRef.updateChildren(map)
                                    getDriverLocation()
                                   getDriverInfo()
                                   getHasRideEnded()
                                    mRequest.text = "Calling for help...."
                                }
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {}
                    })
                }
            }

            override fun onKeyExited(key: String) {}
            override fun onKeyMoved(key: String, location: GeoLocation) {}
            override fun onGeoQueryReady() {
                if (!driverFound) {
                    radius++
                    getClosestDriver()
                }
            }

            override fun onGeoQueryError(error: DatabaseError) {}
        })
    }

    // Check for google Services

    private fun getDriverInfo() {
        phone.setVisibility(View.GONE)
        val mCustomerDatabase =
            FirebaseDatabase.getInstance().reference.child("Users").child("EmergencyUnit")
                .child(driverFoundID!!)
        mCustomerDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.childrenCount > 0) {
                    if (dataSnapshot.child("phone") != null) {
                        val phoneNumber = dataSnapshot.child("phone").value.toString()
                        phone.text = dataSnapshot.child("phone").value.toString()
                        call.setOnClickListener(View.OnClickListener {
                            val intent = Intent(
                                Intent.ACTION_CALL,
                                Uri.fromParts("tel", phoneNumber, null)
                            )
                            if (ContextCompat.checkSelfPermission(
                                    this@CivilianMapActivity,
                                    Manifest.permission.CALL_PHONE
                                ) !== PackageManager.PERMISSION_GRANTED
                            ) {
                                ActivityCompat.requestPermissions(
                                    this@CivilianMapActivity,
                                    arrayOf(Manifest.permission.CALL_PHONE),
                                    1
                                )
                            } else {
                                startActivity(intent)
                            }
                        })
                    }


                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }


    private var mDriverMarker: Marker? = null
    private var driverLocationRef: DatabaseReference? = null
    private var driverLocationRefListener: ValueEventListener? = null
    private fun getDriverLocation() {
        driverLocationRef = FirebaseDatabase.getInstance().reference.child("driversWorking")
            .child(driverFoundID!!).child("l")
        driverLocationRefListener =
            driverLocationRef!!.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists() && requestBol) {
                        val map =
                            dataSnapshot.value as List<Any?>?
                        var locationLat = 0.0
                        var locationLng = 0.0
                        if (map!![0] != null) {
                            locationLat = map[0].toString().toDouble()
                        }
                        if (map[1] != null) {
                            locationLng = map[1].toString().toDouble()
                        }
                        val driverLatLng = LatLng(locationLat, locationLng)
                        if (mDriverMarker != null) {
                            mDriverMarker!!.remove()
                        }
                        val loc1 = Location("")
                        loc1.latitude = mCurrentLocation!!.latitude
                        loc1.longitude = mCurrentLocation!!.longitude
                        val loc2 = Location("")
                        loc2.latitude = driverLatLng.latitude
                        loc2.longitude = driverLatLng.longitude
                        val distance = loc1.distanceTo(loc2)
                        if (distance < 0.1) {
                            text2.text = "Help has Arrived"
                        } else {
                            val inkm = distance/1000
                            patience.text = "We are $inkm km away from you which is approximately 10 minutes away from you. kindly exercise patients "
                        }
                        mDriverMarker = mMap.addMarker(
                            MarkerOptions().position(driverLatLng).title("Your Help")
                        )
                        updateMapLocation(mCurrentLocation)
                        TransitionManager.beginDelayedTransition(transitionsContainer)
                        first.visibility = View.GONE
                        second.visibility = View.VISIBLE
                        radioView.visibility = View.GONE
                        patience.visibility = View.VISIBLE
                        mMap.moveCamera(CameraUpdateFactory.zoomTo(15.0f))
                        updateMapLocation(mCurrentLocation)


                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
    }

    private var driveHasEndedRef: DatabaseReference? = null
    private var driveHasEndedRefListener: ValueEventListener? = null
    private fun getHasRideEnded() {
        driveHasEndedRef =
            FirebaseDatabase.getInstance().reference.child("Users").child("EmergencyUnit")
                .child(driverFoundID!!).child("customerRequest").child("customerRideId")
        driveHasEndedRefListener =
            driveHasEndedRef?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                    } else {
                        broadcaster.stopBroadcast()
                        endRide()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
    }

    private fun endRide() {
        mAuth = FirebaseAuth.getInstance()
        userID = mAuth?.currentUser?.getUid()
        mCustomerDatabase =
            FirebaseDatabase.getInstance().reference.child("Users").child("Civilians")
                .child(userID!!)
        val userInfo =
            HashMap<String?, Any?>()
        userInfo["message"] = ""
        mCustomerDatabase!!.updateChildren(userInfo)
        requestBol = false
        geoQuery!!.removeAllListeners()
        if (driverLocationRef != null && driveHasEndedRef != null) {
            driverLocationRef!!.removeEventListener(driverLocationRefListener!!)
            driveHasEndedRef?.removeEventListener(driveHasEndedRefListener!!)
        }
        if (driverFoundID != null) {
            val driverRef =
                FirebaseDatabase.getInstance().reference.child("Users").child("EmergencyUnit")
                    .child(driverFoundID!!).child("customerRequest")
            driverRef.removeValue()
            driverFoundID = null
        }
        driverFound = false
        radius = 1
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        val ref = FirebaseDatabase.getInstance().getReference("customerRequest")
        val geoFire = GeoFire(ref)
        geoFire.removeLocation(userId)
        if (pickupMarker != null) {
            pickupMarker!!.remove()
        }
        if (mDriverMarker != null) {
            mDriverMarker!!.remove()
        }
        TransitionManager.beginDelayedTransition(transitionsContainer)
        mRequest.text = ""
        first.visibility = View.VISIBLE
        second.visibility = View.GONE
        radioView.visibility = View.VISIBLE
        patience.visibility = View.GONE
        circularProgressBar.indeterminateMode = false

    }
    private fun SendMessage(){
        resultsHistory.clear()
        getUserHistoryIds()
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        val usrName  = FirebaseAuth.getInstance().currentUser!!.displayName
        val driverRef =
            FirebaseDatabase.getInstance().reference.child("Users").child("Civilians")
                .child(userId).child("messages")
        val customerRef =
            FirebaseDatabase.getInstance().reference.child("Users").child("EmergencyUnit")
                .child(driverFoundID!!).child("messages")
        val messageRef =
            FirebaseDatabase.getInstance().reference.child("messages")
        val requestId = messageRef.push().key
        driverRef.child(requestId!!).setValue(true)
        customerRef.child(requestId).setValue(true)
        val map = HashMap<String?, Any?>()
        map["emergencyUnit"] = driverFoundID
        map["civilian"] = userId
        map["timestamp"] = getCurrentTimestamp()
        map["username"] = "isaac"
        map["message"] = message_field.text.toString()
        messageRef.child(requestId).updateChildren(map)



    }

    public override fun onResume() {
        super.onResume()
        // ... permission checks, see above
        broadcaster.setCameraSurface(previewSurface)
        broadcaster.setSaveOnServer(false)
        broadcaster.setRotation(this.windowManager.defaultDisplay.rotation)
        broadcaster.onActivityResume()
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
                    mCurrentLocation = locationResult!!.lastLocation
                    mLastUpdateTime = DateFormat.getTimeInstance().format(Date())

                    //  showProgressDialog()
                    if (location != null) {
                        updateMapLocation(location)
                        Latitude = location.latitude.toString()
                        Longitude = location.longitude.toString()
                        Lat.text = Latitude
                        longitude.text = Longitude
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

    override fun onDestroy() {
        super.onDestroy()
        broadcaster.onActivityDestroy()
        endRide()
    }

    override fun onMapReady(googleMap: GoogleMap) {


        mMap = googleMap
        if (mCurrentLocation != null) {
            val sydney = LatLng((Latitude!!).toDouble(), Longitude!!.toDouble())
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 15.0f))
            mMap.moveCamera(CameraUpdateFactory.zoomTo(15.0f))

        }


    }



    private fun updateMapLocation(location: Location?){
        mMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(location?.latitude ?:0.0,location?.longitude?:0.0)))
        mMap.addCircle(CircleOptions().center(LatLng(location?.latitude ?:0.0,location?.longitude?:0.0)).clickable(true).radius(0.1).fillColor(R.color.colorPrimaryDark))
        mMap.isIndoorEnabled = true
        mMap.moveCamera(CameraUpdateFactory.zoomTo(15.0f))
        mMap.isMyLocationEnabled = true
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


    override fun onPause() {
        super.onPause()
        broadcaster.onActivityPause()
    }

    private var nointernet: Dialog? = null


    private fun showSuccesDialog(){
        val li: LayoutInflater = LayoutInflater.from(this)
        val view: View = li.inflate(R.layout.no_internet, null)
        val police: Button = view.findViewById(R.id.police) as Button
        val fireservice: Button = view.findViewById(R.id.fireservice) as Button
        val ambulance: Button = view.findViewById(R.id.Ambulance) as Button


        police.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_CALL,
                Uri.fromParts("tel", "*193#", null)
            )
            if (ContextCompat.checkSelfPermission(
                    this@CivilianMapActivity,
                    Manifest.permission.CALL_PHONE
                ) !== PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this@CivilianMapActivity,
                    arrayOf(Manifest.permission.CALL_PHONE),
                    1
                )
            } else {
                startActivity(intent)
            }

        }
        fireservice.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_CALL,
                Uri.fromParts("tel", "*193#", null)
            )
            if (ContextCompat.checkSelfPermission(
                    this@CivilianMapActivity,
                    Manifest.permission.CALL_PHONE
                ) !== PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this@CivilianMapActivity,
                    arrayOf(Manifest.permission.CALL_PHONE),
                    1
                )
            } else {
                startActivity(intent)
            }
        }
        ambulance.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_CALL,
                Uri.fromParts("tel", "*193#", null)
            )
            if (ContextCompat.checkSelfPermission(
                    this@CivilianMapActivity,
                    Manifest.permission.CALL_PHONE
                ) !== PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this@CivilianMapActivity,
                    arrayOf(Manifest.permission.CALL_PHONE),
                    1
                )
            } else {
                startActivity(intent)
            }
        }

        nointernet?.setContentView(view)
        nointernet?.setCanceledOnTouchOutside(false)
        nointernet?.setOnCancelListener {
            nointernet?.dismiss()
        }
        nointernet?.show()

    }

    private fun FetchRideInformation(rideKey: String) {
        val historyDatabase =
            FirebaseDatabase.getInstance().reference.child("messages").child(rideKey)
        historyDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
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
                    val obj = MessageObject(getDate(timestamp!!),mesage,user)
                    resultsHistory.add(obj)
                   list_of_messages.adapter?.notifyDataSetChanged()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })

    }

    private fun getUserHistoryIds() {
        layoutManager?.reverseLayout = true
        list_of_messages.layoutManager = layoutManager
        val userHistoryDatabase =
            FirebaseDatabase.getInstance().reference.child("Users").child("Civilians")
                .child(userId!!).child("messages")
        userHistoryDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (message in dataSnapshot.children) {
                        firstZise = message.childrenCount.toInt()
                        if (firstZise >= resultsHistory.size){
                            FetchRideInformation(message.key!!)
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private val resultsHistory  = ArrayList<MessageObject>()

    private fun getDataSetHistory(): List<MessageObject> {
        return resultsHistory.asReversed()
    }

    private fun getDate(time: Long): String? {
        val cal = Calendar.getInstance(Locale.getDefault())
        cal.timeInMillis = time * 1000
        return android.text.format.DateFormat.format("MM-dd-yyyy hh:mm", cal).toString()
    }




    private fun getCurrentTimestamp(): Long? {
        return System.currentTimeMillis() / 1000
    }

}
