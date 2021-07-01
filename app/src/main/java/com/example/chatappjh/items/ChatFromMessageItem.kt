package com.example.chatappjh.items

import android.util.Log
import com.example.chatappjh.R
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.chat_from_row.view.*
import java.text.SimpleDateFormat
import java.util.*

class ChatFromMessageItem(val text: String, val name: String, val timestamp: Long): Item<GroupieViewHolder>(){
    companion object {
        val TAG = "ChatFromMessage"
    }


    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        Log.d(TAG, "text is $text")
        Log.d(TAG, "timestamp length is ${timestamp.toString().length}")

        viewHolder.itemView.textView_from_chatmessage.text = text
        viewHolder.itemView.textView_chat_from_name_and_time.text = if(timestamp.toString().length==10) "$name   ${getDateTimeKotlin(timestamp)}" else "$name   ${getDateTimeReact(timestamp)}"
    }

    override fun getLayout(): Int {
        return R.layout.chat_from_row
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