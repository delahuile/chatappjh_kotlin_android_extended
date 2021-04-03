package com.example.chatappjh
import android.app.UiModeManager
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.synthetic.main.activity_chat.*

private lateinit var linearLayoutManager: LinearLayoutManager

lateinit var signInClient: GoogleSignInClient
lateinit var signInOptions: GoogleSignInOptions

class ChatActivity : AppCompatActivity() {

    companion object {
        val TAG = "ChatActivity"
    }

    var username = ""

    val adapter = GroupAdapter<GroupieViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

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

        button_send_chat.setOnClickListener {
            Log.d(TAG, "Attemting to send a chatmessage to the database")
            sendMessage()
        }
        listenMessages()

        //retrieves username from firebase database
        val ref = FirebaseDatabase.getInstance().getReference("/userID_Names")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                Log.d(TAG, "logged users uid is ${FirebaseAuth.getInstance().uid}")
                val children = snapshot!!.children

                children.forEach {
                    Log.d(TAG, "logged users uid is ${FirebaseAuth.getInstance().uid}")
                    val user = it.getValue(UsernameUID::class.java)
                    Log.d(TAG, "it.getvalue is ${it.getValue(UsernameUID::class.java)?.uid}")
                    if(user==null) return
                    if (user.uid == FirebaseAuth.getInstance().uid) {
                        username = user.name
                        Log.d(TAG, "username set to $username")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d(TAG, "Returning to login activity")
            }
        })
    }

    private fun sendMessage(){
        val text = edittext_chat.text.toString()

        val fromID = FirebaseAuth.getInstance().uid

        if (fromID == null) return

        val reference = FirebaseDatabase.getInstance().getReference("/chats").push()
        val message = ChatMessage(text, username, System.currentTimeMillis()/1000, fromID)

        reference.setValue(message)
            .addOnSuccessListener {
                Log.d(TAG, "message send into the database successfully")
                edittext_chat.getText().clear()
                recyclerview_chat.smoothScrollToPosition(adapter.itemCount)
            }
            .addOnFailureListener {
                Log.d(TAG, "failed to send message into the database")
            }
    }

    private fun listenMessages(){
        val ref = FirebaseDatabase.getInstance().getReference("/chats")

        ref.addChildEventListener(object: ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage = snapshot.getValue(ChatMessage::class.java)
                Log.d(TAG, "content is ${chatMessage?.content}")

                if (chatMessage==null) return

                if (chatMessage.uid == FirebaseAuth.getInstance().uid) {
                    Log.d(TAG, "chatmessage is $chatMessage")
                    adapter.add(ChatToMessage(chatMessage.content, chatMessage.name, chatMessage.timestamp))
                }

                if (chatMessage.uid != FirebaseAuth.getInstance().uid) {
                    if (chatMessage.uid == "9999"){
                        adapter.add(ChatUsernameChange(chatMessage.content, chatMessage.timestamp))

                    } else {
                        Log.d(TAG, "chatmessage is $chatMessage")
                        adapter.add(ChatFromMessage(chatMessage.content, chatMessage.name, chatMessage.timestamp))
                    }

                }
                // Scrolls recyclerview to the position of the last message
                recyclerview_chat.smoothScrollToPosition(adapter.itemCount)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {

            }

            override fun onChildRemoved(snapshot: DataSnapshot) {

            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
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

