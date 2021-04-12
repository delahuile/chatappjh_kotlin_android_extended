package com.example.chatappjh

import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_set_username_from_chat.*
import kotlinx.android.synthetic.main.activity_set_username_from_signup.*

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

        val ref = FirebaseDatabase.getInstance().getReference("/userID_Names")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                val children = snapshot!!.children

                children.forEach { child ->
                    val user = child.getValue(UsernameUID::class.java)

                    if(user==null) return
                    if (user.uid == FirebaseAuth.getInstance().uid) {
                        var dir = ref.child(child.key.toString())
                        var newUsernameUID = UsernameUID(newName, user.uid)
                        dir.setValue(newUsernameUID).addOnSuccessListener {

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
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

}