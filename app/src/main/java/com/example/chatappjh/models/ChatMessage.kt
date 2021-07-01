package com.example.chatappjh.models

import com.google.firebase.Timestamp

class ChatMessage(val content: String,
                  override val name: String,
                  override val timestamp: Timestamp,
                  override val uid: String,
                  override val contentType: String)
    : Message  {
    constructor() : this("", "", Timestamp(100L, 10), "", "")
}