package org.flepper.geoalert

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.transition.TransitionManager
import android.view.View
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.viewpager2.widget.ViewPager2
import kotlinx.android.synthetic.main.activity_intro.*
import org.flepper.geoalert.adapters.IntroSliderAdapter
import org.flepper.geoalert.emergency.EmergencyUnitLogin
import org.flepper.geoalert.models.IntroSlide

class IntroActivity : AppCompatActivity() {

    private val introSliderAdapter =
        IntroSliderAdapter(
            listOf(
                IntroSlide(
                    R.drawable.ambulance
                ),
                IntroSlide(
                    R.drawable.firestation
                ),
                IntroSlide(
                    R.drawable.police
                )

            )
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)


        isStoragePermissionGranted()
        begin.setOnClickListener {
            Intent(this,LoginActivity::class.java).also {
                startActivity(it)
            }
        }

        geo.setOnClickListener {
            Intent(this,EmergencyUnitLogin::class.java).also {
                startActivity(it)
            }
        }

        introSliderViewPager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback(){
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setCurrentIndicator(position)
                when {
                    introSliderViewPager.currentItem + 1 == introSliderAdapter.itemCount -> {
                        TransitionManager.beginDelayedTransition(transitionsContainer)
                        nextElement.visibility = View.GONE
                        begin.visibility = View.VISIBLE
                        text.visibility = View.GONE
                        life.visibility = View.VISIBLE
                        fire.visibility = View.GONE

                    }
                    introSliderViewPager.currentItem  + 1 == introSliderAdapter.itemCount - 2-> {
                        TransitionManager.beginDelayedTransition(transitionsContainer)
                        nextElement.visibility = View.VISIBLE
                        text.visibility = View.VISIBLE
                        fire.visibility = View.GONE
                        life.visibility = View.GONE
                        begin.visibility = View.GONE
                    }
                    introSliderViewPager.currentItem  == introSliderAdapter.itemCount - 1 -> {
                        TransitionManager.beginDelayedTransition(transitionsContainer)
                        nextElement.visibility = View.VISIBLE
                        text.visibility = View.GONE
                        life.visibility = View.GONE
                        fire.visibility = View.VISIBLE
                        begin.visibility = View.GONE
                    }
                }


            }

        })


        nextElement.setOnClickListener {
            if(introSliderViewPager.currentItem + 1 < introSliderAdapter.itemCount){
                introSliderViewPager.currentItem += 1
            }
        }

        introSliderViewPager.adapter = introSliderAdapter
        //  setupIndicators()
        setCurrentIndicator(0)
    }

    private fun setCurrentIndicator(index:Int){
        val childCount =  indicator_container.childCount
        for(i in 0 until childCount){
            val imageView = indicator_container[i] as ImageView
            if(i == index){
                imageView.setImageDrawable(
                    ContextCompat.getDrawable(
                        applicationContext,
                        R.drawable.indicator_active
                    )
                )
            } else{
                imageView.setImageDrawable(
                    ContextCompat.getDrawable(
                        applicationContext,
                        R.drawable.indicator_inactive
                    )
                )

            }
        }
    }

    private fun isStoragePermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                === PackageManager.PERMISSION_GRANTED
            ) {
                true
            }

            else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO),
                    1
                )
                false
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Utils.REQUEST_CODE_PHOTO_LIBRARY &&
            resultCode == Activity.RESULT_OK &&
            data != null) {

        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            return
            //resume tasks needing this permission
        }
    }
}
