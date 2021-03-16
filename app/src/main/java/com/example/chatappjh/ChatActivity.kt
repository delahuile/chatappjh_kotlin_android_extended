package com.example.chatappjh
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.xwray.groupie.Item
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.chat_from_row.view.*
import kotlinx.android.synthetic.main.chat_from_row.view.textView_from_chatmessage
import kotlinx.android.synthetic.main.chat_to_row.view.*
import java.text.SimpleDateFormat
import java.util.*

private lateinit var linearLayoutManager: LinearLayoutManager

class ChatActivity : AppCompatActivity() {

    companion object {
        val TAG = "ChatActivity"
    }

    var username = ""

    val adapter = GroupAdapter<GroupieViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

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
                TODO("Not yet implemented")
            }
        })
    }

    class ChatMessage(val content: String, val name: String, val timestamp: Long, val uid: String) {
        constructor() : this("", "", -1, "")
    }

    class UsernameUID(val name: String, val uid: String) {
        constructor() : this("", "")
    }

    private fun sendMessage(){
        val text = edittext_chat.text.toString()

        //TODO implement getting username from database

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
                    Log.d(TAG, "chatmessage is $chatMessage")
                    adapter.add(ChatFromMessage(chatMessage.content, chatMessage.name, chatMessage.timestamp))
                }
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
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, LoginActivity::class.java)
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
}

class ChatFromMessage(val text: String, val name: String, val timestamp: Long): Item<GroupieViewHolder>(){
    companion object {
        val TAG = "ChatFromMessage"
    }


    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        Log.d(TAG, "text is $text")
        Log.d(ChatFromMessage.TAG, "timestamp length is ${timestamp.toString().length}")

        viewHolder.itemView.textView_from_chatmessage.text = text
        viewHolder.itemView.textView_chat_from_name_and_time.text = if(timestamp.toString().length==10) "$name   ${getDateTimeKotlin(timestamp)}" else "$name   ${getDateTimeReact(timestamp)}"
    }

    override fun getLayout(): Int {
        return R.layout.chat_from_row
    }

    private fun getDateTimeKotlin(timestamp: Long): String? {
        try {
            val sdf = SimpleDateFormat("MM/dd/yyyy  HH:mm:ss")
            val netDate = Date(timestamp*1000)
            return sdf.format(netDate)
        } catch (e: Exception) {
            return e.toString()
        }
    }

    private fun getDateTimeReact(timestamp: Long): String? {
        try {
            val sdf = SimpleDateFormat("MM/dd/yyyy  HH:mm:ss")
            val netDate = Date(timestamp)
            return sdf.format(netDate)
        } catch (e: Exception) {
            return e.toString()
        }
    }
}

class ChatToMessage(val text: String, val name: String, val timestamp: Long): Item<GroupieViewHolder>(){

    companion object {
        val TAG = "ChatToMessage"
    }

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        Log.d(TAG, "text is $text")

        Log.d(TAG, "timestamp length is ${timestamp.toString().length}")

        viewHolder.itemView.textView_to_chatmessage.text = text
        viewHolder.itemView.textView_chat_to_name_and_time.text = if(timestamp.toString().length==10) "$name   ${getDateTimeKotlin(timestamp)}" else "$name   ${getDateTimeReact(timestamp)}"
    }

    override fun getLayout(): Int {
        return R.layout.chat_to_row
    }

    private fun getDateTimeKotlin(timestamp: Long): String? {
        try {
            val sdf = SimpleDateFormat("MM/dd/yyyy  HH:mm:ss")
            val netDate = Date(timestamp*1000)
            return sdf.format(netDate)
        } catch (e: Exception) {
            return e.toString()
        }
    }

    private fun getDateTimeReact(timestamp: Long): String? {
        try {
            val sdf = SimpleDateFormat("MM/dd/yyyy  HH:mm:ss")
            val netDate = Date(timestamp)
            return sdf.format(netDate)
        } catch (e: Exception) {
            return e.toString()
        }
    }
}
