package org.flepper.geoalert

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_user_info.*
import org.flepper.geoalert.civilian.CivilianMapActivity
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class UserInfo : AppCompatActivity() {

    val REQUEST_IMAGE_CAPTURE = 1

    private var mAuth: FirebaseAuth? = null
    private var mCustomerDatabase: DatabaseReference? = null

    private var userID: String? = null
    private var mName: String? = null
    private var mPhone: String? = null
    private val mProfileImageUrl: String? = null

    private var resultUri: Uri? = null

    private var  photoURI:Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_info)
        val isEmergency = intent.getBooleanExtra("emergency_unit",false)

        isStoragePermissionGranted()

        begin.setOnClickListener {
            saveUserInformation()
        }

        mAuth = FirebaseAuth.getInstance()
        userID = mAuth?.currentUser!!.uid
        mCustomerDatabase =
            FirebaseDatabase.getInstance().reference.child("Users").child(if (isEmergency) "EmergencyUnit" else "Civilians")
                .child(userID!!)

        val items = listOf("Male", "Female")
        val adapter = ArrayAdapter(this, R.layout.list_item, items)
        (gender_field as? AutoCompleteTextView)?.setAdapter(adapter)

        upload.setOnClickListener {
            dispatchTakePictureIntent()
        }

        load.setOnClickListener {
            fromMedia()
        }

    }


    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    toastShort(ex.message.toString())
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                     photoURI = FileProvider.getUriForFile(
                        this,
                        "org.flepper.geoalert.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }


    private fun saveUserInformation() {
        progress.visibility = View.VISIBLE
        val mName = name_field.text.toString()
        val mPhone = phonenumber_field.text.toString()
        val mAge = age_field.text.toString()
        val mGender = gender_field.text.toString()
        val userInfo =
            HashMap<String, Any>()
        userInfo["name"] = mName
        userInfo["phone"] = mPhone
        userInfo["age"] = mAge
        userInfo["gender"] = mGender
        mCustomerDatabase!!.updateChildren(userInfo)
        if (resultUri != null) {
            val filePath =
                FirebaseStorage.getInstance().reference.child("profile_images").child(userID!!)
            var bitmap: Bitmap? = null
            try {
                bitmap = MediaStore.Images.Media.getBitmap(
                    application.contentResolver,
                    resultUri
                )
            } catch (e: IOException) {
                e.printStackTrace()
            }
            val baos = ByteArrayOutputStream()
            bitmap!!.compress(Bitmap.CompressFormat.JPEG, 20, baos)
            val data = baos.toByteArray()
            val uploadTask = filePath.putBytes(data)
            uploadTask.addOnFailureListener(OnFailureListener {
                finish()
                return@OnFailureListener
            })
            uploadTask.addOnSuccessListener(OnSuccessListener { taskSnapshot ->
                progress.visibility = View.GONE
                toastShort("Info Saved Succesfully")

                val downloadUrl: Uri? = taskSnapshot.uploadSessionUri
                val newImage =
                    HashMap<String?, Any?>()
                newImage["profileImageUrl"] = downloadUrl.toString()
                mCustomerDatabase!!.updateChildren(newImage)
                val intent = Intent(this, CivilianMapActivity::class.java)
                startActivity(intent)
                finish()
            })
        } else {
            toast("no image")
        }
    }


    private fun fromMedia(){
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 2)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            resultUri = photoURI
            profile_image.setImageURI(photoURI)
        }else if(requestCode == 2 && resultCode == RESULT_OK){
            val imageUri = data!!.data
            resultUri = imageUri
            profile_image.setImageURI(resultUri)
        }


    }

    private lateinit var currentPhotoPath: String

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    fun isStoragePermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                === PERMISSION_GRANTED
            ) {
                true
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    1
                )
                false
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            true
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
