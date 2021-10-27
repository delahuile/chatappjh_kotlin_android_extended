package com.example.chatappjh

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.Glide.with
import com.example.chatappjh.items.*
import com.example.chatappjh.models.*
import com.example.chatappjh.utils.BitmapResolver
import com.example.chatappjh.utils.ImageUtil
import com.example.chatappjh.utils.ImageUtil.scaleBitmap
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Picasso
import com.stfalcon.imageviewer.StfalconImageViewer
import com.xwray.groupie.*
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.image_from_row.view.*
import kotlinx.android.synthetic.main.image_to_row.view.*
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.StringUtils.chop


private lateinit var linearLayoutManager: LinearLayoutManager

lateinit var signInClient: GoogleSignInClient
lateinit var signInOptions: GoogleSignInOptions
lateinit var username_uid_key: String
lateinit var chatMessageIDs: ArrayList<String>

private const val RC_SELECT_IMAGE = 5

public var displayWidth = 1
public var displayHeight = 1

lateinit var imageMessages: ArrayList<ImageMessage>
lateinit var imageMessagePositionMap: HashMap<Int, Int>

private lateinit var viewer: StfalconImageViewer<ImageMessage>


class ChatActivity : AppCompatActivity() {

    companion object {
        val TAG = "ChatActivity"
    }


    var username = ""

    val adapter = GroupAdapter<GroupieViewHolder>()

    private val firestoreInstance: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val display = resources.displayMetrics
        displayWidth = display.widthPixels
        displayHeight = display.heightPixels

        chatMessageIDs = ArrayList<String>()
        imageMessages = ArrayList<ImageMessage>()
        imageMessagePositionMap = HashMap<Int, Int>()

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
                // Scrolls recyclerview to the position of the last message.
                // Total number of messages is messages under textSection plus messages under imageSection.
                recyclerview_chat.smoothScrollToPosition(adapter.itemCount)
            } else {
                Log.d(TAG, "Current data: null")
            }
        }

        button_send_chat.setOnClickListener {
            Log.d(TAG, "Attemting to send a chatmessage to the database")
            sendTextMessage()
        }

        imageButton_chat.setOnClickListener {
            val intent = Intent().apply{
                type = "image/*"
                action = Intent.ACTION_GET_CONTENT
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png"))
            }
            startActivityForResult(Intent.createChooser(intent, "Select image"), RC_SELECT_IMAGE)
        }

    }

    override fun onStart() {
        super.onStart()

    }

    private fun sendTextMessage(){
        val text = edittext_chat.text.toString()

        val fromID = FirebaseAuth.getInstance().uid

        if (fromID == null) return

        val message = ChatMessage(text, username, Timestamp.now(), fromID, MessageType.TEXT)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == RC_SELECT_IMAGE && resultCode == Activity.RESULT_OK && data != null && data.data != null){
            val imagePath = data.data
            val bitmapResolver = BitmapResolver()

            val imageBitmap = bitmapResolver.getBitmap(contentResolver, imagePath)
            val thumbImageBmp: Bitmap
            val expandedImageBmp: Bitmap

            if (imageBitmap!!.height > 500 || imageBitmap!!.height > 500) {
                thumbImageBmp = scaleBitmap(imageBitmap, 500, 500)
            } else {
                thumbImageBmp = imageBitmap
            }

            if (imageBitmap!!.height > 1200 || imageBitmap!!.height > 1200){
                expandedImageBmp = scaleBitmap(imageBitmap, 1200, 1200)
            } else {
                expandedImageBmp = imageBitmap
            }

            val thumbOutputStream = ByteArrayOutputStream()
            val expandedOutputStream = ByteArrayOutputStream()

            thumbImageBmp!!.compress(Bitmap.CompressFormat.JPEG, 85, thumbOutputStream)
            expandedImageBmp!!.compress(Bitmap.CompressFormat.JPEG, 85, expandedOutputStream)

            val thumbImageBytes = thumbOutputStream.toByteArray()
            val expandedImageBytes = expandedOutputStream.toByteArray()

            ImageUtil.uploadMessageImage(thumbImageBytes, true) { imagePathThumb ->

                ImageUtil.uploadMessageImage(expandedImageBytes, false) { imagePathExpanded ->

                    val messageToSend =
                        ImageMessage(imagePathThumb, imagePathExpanded, username, Timestamp.now(), FirebaseAuth.getInstance().currentUser!!.uid, MessageType.IMAGE)
                    firestoreInstance.collection("chats/").add(messageToSend)
                        .addOnCompleteListener{
                            Log.d(TAG, "Image send into the Firebase storage successfully")
                        }
                }
            }
        }
    }

    //TODO: separate message listening into its own thread and start a new thread from this thread for every
    // imagemessage that performs image processing and returns processed image to the listening thread
    // and listening thread returns imageMessage to UI main thread that adds imagemessage to adapter
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
                        if(dc.document.getString("contentType").equals("TEXT")){
                            val chatMessage= dc.document.toObject(ChatMessage::class.java)
                            if (chatMessage.uid == FirebaseAuth.getInstance().uid) {
                                Log.d(TAG, "chatmessage is $chatMessage")
                                adapter.add(ChatToMessageItem(chatMessage.content, chatMessage.name, chatMessage.timestamp.seconds))
                                recyclerview_chat.smoothScrollToPosition(adapter.itemCount)
                            }

                            if (chatMessage.uid != FirebaseAuth.getInstance().uid) {
                                if (chatMessage.uid == "9999"){
                                    adapter.add(ChatUsernameChangeMessageItem(chatMessage.content, chatMessage.timestamp.seconds))
                                    recyclerview_chat.smoothScrollToPosition(adapter.itemCount)
                                } else {
                                    Log.d(TAG, "chatmessage is $chatMessage")
                                    adapter.add(ChatFromMessageItem(chatMessage.content, chatMessage.name, chatMessage.timestamp.seconds))
                                    recyclerview_chat.smoothScrollToPosition(adapter.itemCount)
                                }
                            }
                        } else {
                            val imageMessage= dc.document.toObject(ImageMessage::class.java)
                            imageMessages.add(imageMessage)
                            if (imageMessage.uid == FirebaseAuth.getInstance().uid) {
                                adapter.add(ImageToMessageItem(imageMessage))
                                recyclerview_chat.smoothScrollToPosition(adapter.itemCount)
                                Log.d(TAG, "imageToMessage added to adapter")
                            } else {
                                adapter.add(ImageFromMessageItem(imageMessage))
                                recyclerview_chat.smoothScrollToPosition(adapter.itemCount)
                                Log.d(TAG, "imageFromMessage added to adapter")
                            }
                            imageMessagePositionMap[adapter.itemCount-1] = imageMessages.size-1
                        }
                    }
                    DocumentChange.Type.MODIFIED -> TODO()
                    DocumentChange.Type.REMOVED -> TODO()
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

    inner class ImageFromMessageItem (val message: ImageMessage
    ): Item<GroupieViewHolder>() {

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {

            val thumbref = Firebase.storage.reference.child(message.thumbUrl)
            thumbref.downloadUrl.addOnSuccessListener {Uri->

                val thumbURL = Uri.toString()



                Picasso.get().load(thumbURL)
                    .into(viewHolder.itemView.imageView_from_chatmessage)

                viewHolder.itemView.imageView_from_chatmessage.setOnClickListener {
                    Log.d(TAG, "clicked imageView_from_chatmessage.setOnClickListener, imageMessage thumburl is ${message.thumbUrl}")

                    StfalconImageViewer.Builder<ImageMessage>(
                        this@ChatActivity, imageMessages
                    ) { imageView, image ->
                        Log.d(TAG, "image.expandedUrl inside Builder is ${image.expandedUrl}")
                        Log.d(TAG, "imageView inside Builder is ${imageView}")
                        Log.d(TAG, "imageMessageMap value is ${imageMessagePositionMap[position]}")

                        val gsReference = FirebaseStorage.getInstance().getReferenceFromUrl("${chop(FirebaseStorage.getInstance().getReference().toString())}${image.expandedUrl}")
                        gsReference.downloadUrl.addOnSuccessListener {uri->

                            Picasso.get()
                                .load(uri)
                                .into(imageView)
                        }
                    }
                        .withStartPosition(imageMessagePositionMap[position]!!)
                        .withHiddenStatusBar(false)
                        .withBackgroundColor(Color.parseColor("#000000"))
                        .show(true)
                }

            }

            viewHolder.itemView.textView_from_imageMessage.text = if(message.timestamp.seconds.toString().length==10) "${message.name}   ${getDateTimeKotlin(message.timestamp.seconds)}" else "${message.name}   ${getDateTimeReact(message.timestamp.seconds)}"
        }

        override fun getLayout(): Int {
            return R.layout.image_from_row
        }


        private fun getDateTimeKotlin(timestamp: Long): String? {
            try {
                val sdf = SimpleDateFormat("dd/MM/yyyy  HH:mm:ss")
                val netDate = Date(timestamp*1000)
                return sdf.format(netDate)
            } catch (e: Exception) {
                return e.toString()
            }
        }

        private fun getDateTimeReact(timestamp: Long): String? {
            try {
                val sdf = SimpleDateFormat("dd/MM/yyyy  HH:mm:ss")
                val netDate = Date(timestamp)
                return sdf.format(netDate)
            } catch (e: Exception) {
                return e.toString()
            }
        }
    }

    inner class ImageToMessageItem (val message: ImageMessage
    ): Item<GroupieViewHolder>() {

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {

            val thumbref = Firebase.storage.reference.child(message.thumbUrl)
            thumbref.downloadUrl.addOnSuccessListener {Uri->

                val thumbURL = Uri.toString()



                Picasso.get().load(thumbURL)
                    .into(viewHolder.itemView.imageView_to_chatmessage)

                viewHolder.itemView.imageView_to_chatmessage.setOnClickListener {

                    Log.d(TAG, "clicked imageView_to_chatmessage.setOnClickListener, imageMessage thumburl is ${message.thumbUrl}")

                    StfalconImageViewer.Builder<ImageMessage>(
                        this@ChatActivity, imageMessages
                    ) { imageView, image ->
                        Log.d(TAG, "image.expandedUrl inside Builder is ${image.expandedUrl}")
                        Log.d(TAG, "imageView inside Builder is ${imageView}")
                        Log.d(TAG, "imageMessageMap value is ${imageMessagePositionMap[position]}")

                        val gsReference = FirebaseStorage.getInstance().getReferenceFromUrl("${chop(FirebaseStorage.getInstance().getReference().toString())}${image.expandedUrl}")
                        gsReference.downloadUrl.addOnSuccessListener {uri->

                            Picasso.get()
                                .load(uri)
                                .into(imageView)
                        }
                    }
                        .withStartPosition(imageMessagePositionMap[position]!!)
                        .withHiddenStatusBar(false)
                        .withBackgroundColor(Color.parseColor("#000000"))
                        .show(true)
                }

            }

            viewHolder.itemView.textView_to_imageMessage.text = if(message.timestamp.seconds.toString().length==10) "${message.name}   ${getDateTimeKotlin(message.timestamp.seconds)}" else "${message.name}   ${getDateTimeReact(message.timestamp.seconds)}"
        }

        override fun getLayout(): Int {
            return R.layout.image_to_row
        }


        private fun getDateTimeKotlin(timestamp: Long): String? {
            try {
                val sdf = SimpleDateFormat("dd/MM/yyyy  HH:mm:ss")
                val netDate = Date(timestamp*1000)
                return sdf.format(netDate)
            } catch (e: Exception) {
                return e.toString()
            }
        }

        private fun getDateTimeReact(timestamp: Long): String? {
            try {
                val sdf = SimpleDateFormat("dd/MM/yyyy  HH:mm:ss")
                val netDate = Date(timestamp)
                return sdf.format(netDate)
            } catch (e: Exception) {
                return e.toString()
            }
        }
    }
}

