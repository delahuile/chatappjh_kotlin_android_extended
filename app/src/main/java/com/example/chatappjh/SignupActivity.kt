package com.example.chatappjh

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_signup.*

class SignupActivity : AppCompatActivity() {

    companion object {
        val TAG = "SignupActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        FirebaseAuth.getInstance()


        button_signup.setOnClickListener {
            val email = edit_signup_email.text.toString()
            val password = edit_signup_password.text.toString()

            Log.d("SignupActivity", "email is " + email)
            Log.d( "SignupActivity", "password is $password")
            Log.d("SignupActivity", "Shows signup activity")

            registerUserToFirebase(email, password)
        }

        button_google_signup.setOnClickListener {
            Log.d("SignupActivity", "redirect to googlesignup")
        }

        button_github_signup.setOnClickListener {
            Log.d("SignupActivity", "redirect to githubsignup")
        }

        button_redirect_to_login.setOnClickListener {
            Log.d("SignupActivity", "Redirects to login activity")
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun registerUserToFirebase(email: String, password: String) {
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if(!it.isSuccessful) return@addOnCompleteListener
                Log.d("TAG", "Registered new user successfully")
                sendUserdataToDatabase()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to register user: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendUserdataToDatabase() {
        val uid = FirebaseAuth.getInstance().uid ?: ""
        val user = User(edit_signup_username.text.toString(), uid)

        val ref = FirebaseDatabase.getInstance().getReference("/userID_Names/$uid")


        ref.setValue(user)
            .addOnSuccessListener {
                Log.d("TAG", "successfully added a new user to Firebase database")

                // redirecting  to chat after successful registration
                val intent = Intent(this, ChatActivity::class.java)

                //clears off the activity stack
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }


    }
}

