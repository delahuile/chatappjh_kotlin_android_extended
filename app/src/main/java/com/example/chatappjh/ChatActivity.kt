package com.example.chatappjh
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.xwray.groupie.Item
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.chat_from_row.view.textView_from_chatmessage
import kotlinx.android.synthetic.main.chat_to_row.view.*

private lateinit var linearLayoutManager: LinearLayoutManager

class ChatActivity : AppCompatActivity() {

    companion object {
        val TAG = "ChatActivity"
    }

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
    }

    class ChatMessage(val content: String, val name: String, val timestamp: Long, val uid: String) {
        constructor() : this("", "", -1, "")
    }

    private fun sendMessage(){
        val text = edittext_chat.text.toString()

        //TODO implement getting username from database
        val name = ""
        val fromID = FirebaseAuth.getInstance().uid

        if (fromID == null) return

        val reference = FirebaseDatabase.getInstance().getReference("/chats").push()

        val message = ChatMessage(text, name, System.currentTimeMillis()/1000, fromID)

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
                    adapter.add(ChatToMessage(chatMessage.content))
                }

                if (chatMessage.uid != FirebaseAuth.getInstance().uid) {
                    Log.d(TAG, "chatmessage is $chatMessage")
                    adapter.add(ChatFromMessage(chatMessage.content))
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
}

class ChatFromMessage(val text: String): Item<GroupieViewHolder>(){
    companion object {
        val TAG = "ChatFromMessage"
    }


    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        Log.d(TAG, "text is $text")

        viewHolder.itemView.textView_from_chatmessage.text = text

    }

    override fun getLayout(): Int {
        return R.layout.chat_from_row
    }
}

class ChatToMessage(val text: String): Item<GroupieViewHolder>(){

    companion object {
        val TAG = "ChatToMessage"
    }

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        Log.d(TAG, "text is $text")
        viewHolder.itemView.textView_to_chatmessage.text = text
    }

    override fun getLayout(): Int {
        return R.layout.chat_to_row
    }
}