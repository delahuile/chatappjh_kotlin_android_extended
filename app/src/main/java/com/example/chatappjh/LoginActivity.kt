package com.example.chatappjh

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        button_login.setOnClickListener {
            val email = edit_login_email.text.toString()
            val password = edit_login_password.text.toString()

            Log.d("LoginActivity", "email is " + email)
            Log.d( "LoginActivity", "password is $password")
            Log.d("LoginActivity", "Shows login activity")

            emailLogin(email, password)
        }

        button_google_login.setOnClickListener {
            Log.d("LoginActivity", "redirect to googlelogin")
        }

        button_github_login.setOnClickListener {
            Log.d("Loginactivity", "redirect to githublogin")
        }

        button_redirect_to_signup.setOnClickListener {
            Log.d("LoginActivity", "Redirects to login activity")
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }

    }

    private fun emailLogin(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()){
            Toast.makeText(this, "Please, enter mail and password", Toast.LENGTH_SHORT).show()
            return
        }

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if(!it.isSuccessful) return@addOnCompleteListener

                Log.d("LoginActivity", "Logged in successfully")

                // redirecting  to chat after successful login
                val intent = Intent(this, ChatActivity::class.java)

                //clears off the activity stack
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)

                startActivity(intent)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to login", Toast.LENGTH_SHORT).show()
            }



    }
}