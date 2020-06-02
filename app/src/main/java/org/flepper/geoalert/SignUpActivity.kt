package org.flepper.geoalert

import android.content.Intent
import android.os.Bundle
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

        mAuth = FirebaseAuth.getInstance()



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
                                        .child("Civilians").child(userId)
                                currentUserDb.setValue(true)
                                progress.visibility = View.GONE
                                snackbar.setAction("ok"){
                                    val intent =
                                        Intent(this, UserInfo::class.java)
                                    startActivity(intent)
                                    finish()
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

    override fun onStart() {
        super.onStart()
        firebaseAuthListener?.let { mAuth?.addAuthStateListener(it) }
    }

    override fun onStop() {
        super.onStop()
        firebaseAuthListener?.let { mAuth!!.removeAuthStateListener(it) }
    }
}
