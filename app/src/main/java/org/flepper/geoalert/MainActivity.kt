package org.flepper.geoalert

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.bambuser.broadcaster.BroadcastStatus
import com.bambuser.broadcaster.Broadcaster
import com.bambuser.broadcaster.CameraError
import com.bambuser.broadcaster.ConnectionError
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val activity = this


    private val broadcasterObserver = object : Broadcaster.Observer {
        override fun onConnectionStatusChange(broadcastStatus: BroadcastStatus) {
            Log.i("Mybroadcastingapp", "Received status change: $broadcastStatus")
            if (broadcastStatus == BroadcastStatus.STARTING) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            if (broadcastStatus == BroadcastStatus.IDLE) {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            send.text = if (broadcastStatus == BroadcastStatus.IDLE) "Broadcast" else "Disconnect"

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




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        send.setOnClickListener {
            if (broadcaster.canStartBroadcasting()) {
                broadcaster.startBroadcast()
            } else {
                broadcaster.stopBroadcast()
            }
        }

    }

    private val APPLICATION_ID = "Xezut3PbTkF0ZfyeVvbmgg"

    private val broadcaster by lazy {
        Broadcaster(this, APPLICATION_ID, broadcasterObserver)
    }

    override fun onPause() {
        super.onPause()
        broadcaster.onActivityPause()
    }

    public override fun onResume() {
        super.onResume()
        // ... permission checks, see above
        broadcaster.setCameraSurface(previewSurface)
        broadcaster.setRotation(this.windowManager.defaultDisplay.rotation)
        broadcaster.onActivityResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        broadcaster.onActivityDestroy()
    }


}

