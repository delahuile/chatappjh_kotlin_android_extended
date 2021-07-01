package com.example.chatappjh.items

import android.util.Log
import com.example.chatappjh.R
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.chat_message_username_changed.view.*
import java.text.SimpleDateFormat
import java.util.*

class ChatUsernameChangeMessageItem (val text: String, val timestamp: Long): Item<GroupieViewHolder>() {
    companion object {
        val TAG = "ChatUsernameChange"
    }


    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        Log.d(TAG, "text is $text")
        Log.d(ChatFromMessageItem.TAG, "timestamp length is ${timestamp.toString().length}")

        val time = if(timestamp.toString().length==10) "$${getDateTimeKotlin(timestamp)}" else "${getDateTimeReact(timestamp)}"

        viewHolder.itemView.message_username_changed.text = "$text on $time".replace('$', ' ', false)

    }

    override fun getLayout(): Int {
        return R.layout.chat_message_username_changed
    }

    private fun getDateTimeKotlin(timestamp: Long): String? {
        try {
            val sdf = SimpleDateFormat("dd/MM/yyyy 'at' HH:mm:ss")
            val netDate = Date(timestamp*1000)
            return sdf.format(netDate)
        } catch (e: Exception) {
            return e.toString()
        }
    }

    private fun getDateTimeReact(timestamp: Long): String? {
        try {
            val sdf = SimpleDateFormat("dd/MM/yyyy 'at' HH:mm:ss")
            val netDate = Date(timestamp)
            return sdf.format(netDate)
        } catch (e: Exception) {
            return e.toString()
        }
    }
}