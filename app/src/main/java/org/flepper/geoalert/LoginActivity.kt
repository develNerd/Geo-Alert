package org.flepper.geoalert

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import kotlinx.android.synthetic.main.activity_login.*
import org.flepper.geoalert.civilian.CivilianMapActivity


class LoginActivity : AppCompatActivity() {


    private var mAuth: FirebaseAuth? = null
    private var firebaseAuthListener: AuthStateListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val firebaseAuth = FirebaseAuth.getInstance()

        mAuth = FirebaseAuth.getInstance()

        firebaseAuthListener = AuthStateListener {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                val intent =
                    Intent(this, CivilianMapActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        login.setOnClickListener {
            val email = email_field.text.toString().trim()
            val password = password_field.text.toString().trim()
            when {
                email.isEmpty() -> {
                    email_field.requestFocus()
                    toast("Email Required")
                }
                password_field.text.toString() != password -> {
                    toast("Passwords Do not match")
                }
                else -> {
                    progress.visibility = View.VISIBLE
                    mAuth?.signInWithEmailAndPassword(email, password)?.
                    addOnCompleteListener{ task ->
                            progress.visibility = View.GONE
                            if (!task.isSuccessful) {
                                toast(task.exception?.message.toString())
                            }else{
                                Intent(this,CivilianMapActivity::class.java).also {
                                    startActivity(it)
                                }
                            }
                        }
                }
            }
        }

        signUp.setOnClickListener {
            Intent(this,SignUpActivity::class.java).also {
                startActivity(it)
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
