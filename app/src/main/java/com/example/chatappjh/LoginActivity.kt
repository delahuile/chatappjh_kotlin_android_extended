package com.example.chatappjh

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    val RC_SIGN_IN: Int = 1
    lateinit var signInClient: GoogleSignInClient
    lateinit var signInOptions: GoogleSignInOptions
    private lateinit var auth: FirebaseAuth

    var userInDatabase = false


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
            Log.d("SignupActivity", "redirect to googlesignup")
            val loginIntent: Intent = signInClient.signInIntent
            startActivityForResult(loginIntent, RC_SIGN_IN)
        }

        auth = FirebaseAuth.getInstance()
        setupGoogleLogin()

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    googleFirebaseAuth(account)
                }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign in failed:(", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun googleFirebaseAuth(acct: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful) {
                checkIfUserIsAlreadyInDatabase()
            } else {
                Toast.makeText(this, "Google sign in failed:(", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupGoogleLogin() {
        signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        signInClient = GoogleSignIn.getClient(this, signInOptions)
    }

    private fun pushUserToUsernameUID() {
        val name = ""
        val fromID = FirebaseAuth.getInstance().uid

        if (!userInDatabase){
            if (fromID == null) return

            val reference = FirebaseDatabase.getInstance().getReference("/userID_Names").push()
            val message = UsernameUID(name, fromID)

            reference.setValue(message)
                    .addOnSuccessListener {
                        Log.d(SignupActivity.TAG, "usernameUID sent into the database successfully")
                        val intent = Intent(this, SetUsernameFromSignup::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
                    .addOnFailureListener {
                        Log.d(SignupActivity.TAG, "failed to send usernameUID into the database")
                    }

        } else {
            val intent = Intent(this, ChatActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    private fun checkIfUserIsAlreadyInDatabase(){

        val ref = FirebaseDatabase.getInstance().getReference("/userID_Names")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                Log.d(ChatActivity.TAG, "logged users uid is ${FirebaseAuth.getInstance().uid}")
                val children = snapshot!!.children

                children.forEach {
                    val user = it.getValue(UsernameUID::class.java)
                    if (user?.uid == FirebaseAuth.getInstance().uid) {
                        userInDatabase = true
                        Log.d(SignupActivity.TAG, "User with uid ${FirebaseAuth.getInstance().uid} is already in database")
                    }
                }
                pushUserToUsernameUID()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d(ChatActivity.TAG, "Returning to login activity")
            }

        })
    }
}