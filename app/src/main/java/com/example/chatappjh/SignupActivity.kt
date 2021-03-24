package com.example.chatappjh

import android.content.Context
import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_signup.*


class SignupActivity : AppCompatActivity() {

    val RC_SIGN_IN: Int = 1
    lateinit var signInClient: GoogleSignInClient
    lateinit var signInOptions: GoogleSignInOptions
    private lateinit var auth: FirebaseAuth

    private lateinit var githubAuthProvider: OAuthProvider.Builder

    private lateinit var constraintlayout: ConstraintLayout
    private lateinit var animationDrawable: AnimationDrawable

    var userInDatabase = false

    companion object {
        val TAG = "SignupActivity"
        fun getLaunchIntent(from: Context) = Intent(from, SignupActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        //Starts animation of the signup layout background
        constraintlayout = findViewById((R.id.signup_layout))
        animationDrawable = constraintlayout.background as AnimationDrawable
        animationDrawable.setEnterFadeDuration(2000)
        animationDrawable.setExitFadeDuration(4000)
        animationDrawable.start()

        button_signup.setOnClickListener {
            val email = edit_signup_email.text.toString()
            val password = edit_signup_password.text.toString()

            Log.d("SignupActivity", "email is " + email)
            Log.d("SignupActivity", "password is $password")
            Log.d("SignupActivity", "Shows signup activity")

            if (email == "" || password == "" || edit_signup_username.text.toString() == ""){
                Toast.makeText(this, "Failed to register user, please fill out all required fields", Toast.LENGTH_SHORT).show()
            } else {
                registerUserToFirebase(email, password)
            }
        }

        button_google_signup.setOnClickListener {
            Log.d("SignupActivity", "redirect to googlesignup")
            val loginIntent: Intent = signInClient.signInIntent
            startActivityForResult(loginIntent, RC_SIGN_IN)
        }

        auth = FirebaseAuth.getInstance()
        setupGoogleLogin()

        githubAuthProvider = OAuthProvider.newBuilder("github.com")
        githubAuthProvider.addCustomParameter("login", "your-email@gmail.com")

        button_github_signup.setOnClickListener {
            Log.d("SignupActivity", "redirect to githubsignup")
            checkPendingGithubLogin()
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
                        Log.d(TAG, "usernameUID sent into the database successfully")
                        val intent = Intent(this, SetUsernameFromSignupActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
                    .addOnFailureListener {
                        Log.d(TAG, "failed to send usernameUID into the database")
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
                        Log.d(
                            TAG,
                            "User with uid ${FirebaseAuth.getInstance().uid} is already in database"
                        )
                    }
                }
                pushUserToUsernameUID()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d(ChatActivity.TAG, "Returning to login activity")
            }

        })
    }

    private fun checkPendingGithubLogin(){


        if (auth.getPendingAuthResult() == null) {
            // There's no pending result so you need to start the sign-in flow.
            // See below.
            startGithubSigninFlow()
        } else {
            val pendingResultTask: Task<AuthResult> = auth.getPendingAuthResult()
            // There's something already here! Finish the sign-in for your user.
            pendingResultTask
                .addOnSuccessListener {
                    // User is signed in.
                    // IdP data available in
                    Log.d(
                        TAG,
                        "it.getAdditionalUserInfo().getProfile() is ${
                            it.getAdditionalUserInfo().getProfile()
                        }"
                    )
                    // The OAuth access token can also be retrieved:
                    // authResult.getCredential().getAccessToken().
                    checkIfUserIsAlreadyInDatabase()
                }
                .addOnFailureListener {
                    Log.d(TAG, "Failed to complete pending github sign-up")
                    // Handle failure.
                }
        }
    }

    private fun startGithubSigninFlow(){
        auth
            .startActivityForSignInWithProvider( /* activity= */this, githubAuthProvider.build())
            .addOnSuccessListener(
                OnSuccessListener<AuthResult?> {
                    // User is signed in.
                    // IdP data available in
                    // authResult.getAdditionalUserInfo().getProfile().
                    // The OAuth access token can also be retrieved:
                    // authResult.getCredential().getAccessToken().
                    checkIfUserIsAlreadyInDatabase()
                })
            .addOnFailureListener(
                OnFailureListener {
                    // Handle failure.
                    Log.d(TAG, "Failed to complete github sign-up")
                })

    }

}

