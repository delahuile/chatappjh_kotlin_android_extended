package com.example.chatappjh.models

import com.google.firebase.Timestamp

object MessageType {
    const val TEXT = "TEXT"
    const val IMAGE = "IMAGE"
}

interface Message {
    val name: String
    val timestamp: Timestamp
    val uid: String
    val contentType: String
}