package com.example.chatappjh

import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.android.synthetic.main.activity_set_username_from_chat.*
import kotlinx.android.synthetic.main.activity_set_username_from_signup.*
import kotlinx.android.synthetic.main.activity_signup.*

class SetUsernameFromSignupActivity : AppCompatActivity() {

    private lateinit var constraintlayout: ConstraintLayout
    private lateinit var animationDrawable: AnimationDrawable

    companion object {
        val TAG = "SetUsernameFromSignup"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_username_from_signup)

        //Starts animation of the setusernamefromsignup layout background
        constraintlayout = findViewById((R.id.set_username_signup_layout))
        animationDrawable = constraintlayout.background as AnimationDrawable
        animationDrawable.setEnterFadeDuration(2000)
        animationDrawable.setExitFadeDuration(4000)
        animationDrawable.start()

        button_set_username_from_chat_signup.setOnClickListener {
            updateUsername()
        }
    }

    private fun updateUsername() {

        val newName = set_username_new_username_signup.text.toString()

        FirebaseFirestore.getInstance().collection("userID_Names")
            .get()
            .addOnCompleteListener(OnCompleteListener<QuerySnapshot?> { task ->
                if (task.isSuccessful) {
                    for (document in task.result!!) {
                        val user = document.toObject(UsernameUID::class.java)
                        if (user.uid == FirebaseAuth.getInstance().uid){
                            FirebaseFirestore.getInstance().collection("userID_Names").document(document.id)
                                .update("name", newName)
                                .addOnSuccessListener {

                                    // let's also update firebase auth DisplayName
                                    var currentUser = FirebaseAuth.getInstance().getCurrentUser();
                                    var profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(newName).build()
                                    currentUser.updateProfile(profileUpdates)

                                    val intent = Intent(this@SetUsernameFromSignupActivity, ChatActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    startActivity(intent)
                                }
                                .addOnFailureListener {
                                    Log.d(TAG, "Failed to change username")
                                }
                        }
                    }
                } else {
                    Log.w(TAG, "Error failed update username.", task.exception)
                }
            })
    }
}