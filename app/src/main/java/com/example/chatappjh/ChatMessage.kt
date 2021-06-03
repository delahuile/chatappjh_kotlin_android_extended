package com.example.chatappjh

import com.google.firebase.Timestamp

class ChatMessage(val content: String, val name: String, val timestamp: Timestamp, val uid: String) {
    constructor() : this("", "", Timestamp(100L, 10), "")
    
}