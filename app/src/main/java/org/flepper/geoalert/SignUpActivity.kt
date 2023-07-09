package org.flepper.geoalert

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_sign_up.*

class SignUpActivity : AppCompatActivity() {

    private var mAuth: FirebaseAuth? = null
    private var firebaseAuthListener: AuthStateListener? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        val isEmergency = intent.getBooleanExtra("emergency_unit",false)
        Log.e("isEmergency",isEmergency.toString())
        mAuth = FirebaseAuth.getInstance()


        Signin.setOnClickListener {
            onBackPressed()
        }

        signup.setOnClickListener {

            val email = email_field.text.toString().trim()
            val password = repeatPassword_field.text.toString().trim()
            when {
                email.isEmpty() -> {
                    email_field.requestFocus()
                    toast("Email Required")
                }
                password.isEmpty() -> {
                    repeatPassword_field.requestFocus()
                    toast("Password is required")
                }
                password_field.text.toString() != password -> {
                    toast("Passwords Do not match")
                }
                else -> {
                    val snackbar = Snackbar.make(it,"User Registered Successfully",Snackbar.LENGTH_INDEFINITE)

                    progress.visibility = View.VISIBLE
                    mAuth?.createUserWithEmailAndPassword(email,password)
                        ?.addOnCompleteListener {task ->
                            if (task.isSuccessful){
                                val userId = mAuth!!.currentUser!!.uid
                                val currentUserDb =
                                    FirebaseDatabase.getInstance().reference.child("Users")
                                        .child(if (isEmergency) "EmergencyUnit" else "Civilians").child(userId)
                                currentUserDb.setValue(true)
                                progress.visibility = View.GONE
                                snackbar.setAction("Next"){
                                    singUpSuccess(isEmergency)
                                }
                                snackbar.show()

                            }else{
                                toastShort(
                                    task.exception?.message.toString()
                                )
                                progress.visibility = View.GONE

                            }

                        }



                }
            }



        }



    }

    fun singUpSuccess(isEmergency:Boolean){
        val intent =
            Intent(this, UserInfo::class.java)
        intent.putExtra("emergency_unit",isEmergency)
        startActivity(intent)
        finish()
    }

    override fun onStart() {
        super.onStart()
        firebaseAuthListener?.let { mAuth?.addAuthStateListener(it) }
    }

    override fun onStop() {
        super.onStop()
        firebaseAuthListener?.let { mAuth!!.removeAuthStateListener(it) }
    }
}
