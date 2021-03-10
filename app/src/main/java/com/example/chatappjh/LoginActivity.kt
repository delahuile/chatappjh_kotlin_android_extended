package com.example.chatappjh

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
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
}