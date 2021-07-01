package com.example.chatappjh

import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.chatappjh.models.ChatMessage
import com.example.chatappjh.models.MessageType
import com.example.chatappjh.models.UsernameUID
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.activity_set_username_from_chat.*
import kotlinx.android.synthetic.main.activity_set_username_from_chat.set_username_new_username
import kotlinx.android.synthetic.main.activity_set_username_from_signup.*


class SetUsernameFromChatActivity : AppCompatActivity() {

    private lateinit var constraintlayout: ConstraintLayout
    private lateinit var animationDrawable: AnimationDrawable

    companion object {
        val TAG = "SetUsernameFromChat"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_username_from_chat)

        val actionbar = supportActionBar
        actionbar!!.setDisplayHomeAsUpEnabled(true)

        //Starts animation of the setusernamefromchat layout background
        constraintlayout = findViewById((R.id.set_username_chat_layout))
        animationDrawable = constraintlayout.background as AnimationDrawable
        animationDrawable.setEnterFadeDuration(2000)
        animationDrawable.setExitFadeDuration(4000)
        animationDrawable.start()

        button_set_username_from_chat.setOnClickListener {
            updateUsername()
        }
    }

    override fun onBackPressed() {
        val intent = Intent(this@SetUsernameFromChatActivity, ChatActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun updateUsername() {

        val newName = set_username_new_username.text.toString()

        FirebaseFirestore.getInstance().collection("userID_Names")
            .get()
            .addOnCompleteListener(OnCompleteListener<QuerySnapshot?> { task ->
                if (task.isSuccessful) {
                    for (document in task.result!!) {
                        val user = document.toObject(UsernameUID::class.java)
                        Log.d(TAG, "user UID is" + user.uid)
                        Log.d(TAG, "FirebaseAuth user uid is" + FirebaseAuth.getInstance().uid)
                        if (user.uid == FirebaseAuth.getInstance().uid){
                            Log.d(TAG,"document id is " + FirebaseFirestore.getInstance().collection("userID_Names").document(user.uid))
                            FirebaseFirestore.getInstance().collection("userID_Names").document(document.id)
                                .update("name", newName)
                                .addOnSuccessListener {

                                    var previousName = user.name

                                    nameChangeMessageToChat(previousName, newName)

                                    // let's also update firebase auth DisplayName
                                    var currentUser = FirebaseAuth.getInstance().getCurrentUser();
                                    var profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(newName).build()
                                    currentUser.updateProfile(profileUpdates)

                                    val intent = Intent(this@SetUsernameFromChatActivity, ChatActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    startActivity(intent)
                                }
                                .addOnFailureListener {
                                    Log.d(TAG, "Failed to change username")
                                }
                        } else {
                            Log.d(TAG, "user not found")
                        }
                    }
                } else {
                    Log.w(TAG, "Error failed update username.", task.exception)
                }
            })
    }

    private fun nameChangeMessageToChat(previusName: String, newName: String){

        val content = "User $previusName changed name to $newName"
        val message = ChatMessage(content, "", Timestamp.now(), "9999", MessageType.TEXT)

        FirebaseFirestore.getInstance().collection("chats").add(message)
            .addOnCompleteListener {
                Log.d(TAG, "message send into the database successfully")
            }
            .addOnFailureListener {
                Log.d(TAG, "failed to send message into the database")
            }
    }
}