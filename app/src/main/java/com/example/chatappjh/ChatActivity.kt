package com.example.chatappjh

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.synthetic.main.activity_chat.*
import kotlin.collections.ArrayList


private lateinit var linearLayoutManager: LinearLayoutManager

lateinit var signInClient: GoogleSignInClient
lateinit var signInOptions: GoogleSignInOptions
lateinit var username_uid_key: String
lateinit var chatMessageIDs: ArrayList<String>


class ChatActivity : AppCompatActivity() {

    companion object {
        val TAG = "ChatActivity"
    }

    var username = ""

    val adapter = GroupAdapter<GroupieViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        chatMessageIDs = ArrayList<String>()

        // Initiates google sign in in case user is logged in with google authorization
        setupGoogleLogin()

        // AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        // If user is not logged in, LoginActivity is launched
        if (FirebaseAuth.getInstance().currentUser==null) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        recyclerview_chat.setAdapter(adapter)

        linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.stackFromEnd = true
        linearLayoutManager.reverseLayout = false

        recyclerview_chat.layoutManager = linearLayoutManager

        listenMessages()

        //retrieves username from firebase database
        val docRef = FirebaseFirestore.getInstance().collection("userID_Names")
        docRef.addSnapshotListener { snapshot, e ->

            if (e != null) {
                Log.w(TAG, "Listen failed.", e)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                snapshot!!.forEach {
                    Log.d(TAG, "logged users uid is ${FirebaseAuth.getInstance().uid}")
                    val user = it.toObject(UsernameUID::class.java)
                    Log.d(TAG, "it.getvalue is ${user.uid}")

                    if (user.uid == FirebaseAuth.getInstance().uid) {
                        username_uid_key = it.id
                        username = user.name
                        Log.d(TAG, "username set to $username")
                    }
                    Log.d(TAG, it.id + " => " + it.data)
                }
                // Scrolls recyclerview to the position of the last message
                recyclerview_chat.smoothScrollToPosition(adapter.itemCount)
            } else {
                Log.d(TAG, "Current data: null")
            }
        }

        button_send_chat.setOnClickListener {
            Log.d(TAG, "Attemting to send a chatmessage to the database")
            sendMessage()
        }

    }

    private fun sendMessage(){
        val text = edittext_chat.text.toString()

        val fromID = FirebaseAuth.getInstance().uid

        if (fromID == null) return

        val message = ChatMessage(text, username, Timestamp.now(), fromID)
        FirebaseFirestore.getInstance().collection("chats").add(message)
            .addOnCompleteListener {
                Log.d(TAG, "message send into the database successfully")
                edittext_chat.getText().clear()
                recyclerview_chat.smoothScrollToPosition(adapter.itemCount)
            }
            .addOnFailureListener {
                Log.d(TAG, "failed to send message into the database")
            }
    }

    private fun listenMessages(){

        FirebaseFirestore.getInstance().collection("chats")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.w(TAG, "listen:error", e)
                return@addSnapshotListener
            }

            for (dc in snapshots!!.documentChanges) {
                when (dc.type) {
                    DocumentChange.Type.ADDED -> {
                        if (dc.document is Number) {
                            Log.d (TAG,"Chatmessage document if of type number")
                        }
                        val chatMessage= dc.document.toObject(ChatMessage::class.java)
                        if (chatMessage.uid == FirebaseAuth.getInstance().uid) {
                            Log.d(TAG, "chatmessage is $chatMessage")
                            adapter.add(ChatToMessage(chatMessage.content, chatMessage.name, chatMessage.timestamp.seconds))
                        }

                        if (chatMessage.uid != FirebaseAuth.getInstance().uid) {
                            if (chatMessage.uid == "9999"){
                                adapter.add(ChatUsernameChange(chatMessage.content, chatMessage.timestamp.seconds))
                            } else {
                                Log.d(TAG, "chatmessage is $chatMessage")
                                adapter.add(ChatFromMessage(chatMessage.content, chatMessage.name, chatMessage.timestamp.seconds))
                            }
                        }
                    }
                    DocumentChange.Type.MODIFIED -> Log.d(TAG, "Modified city: ${dc.document.data}")
                    DocumentChange.Type.REMOVED -> Log.d(TAG, "Removed city: ${dc.document.data}")
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item?.itemId){
            R.id.chat_signout -> {
                Log.d(TAG, "signInOptions.account != null is ${signInOptions.account != null}")
                if (signInOptions.account != null) {
                    signInClient.signOut()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                } else {
                    signInClient.signOut()
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
            }
            R.id.chat_change_username -> {
                val intent = Intent(this, SetUsernameFromChatActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.chat_navigation_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun setupGoogleLogin() {
        signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        signInClient = GoogleSignIn.getClient(this, signInOptions)

    }
}

