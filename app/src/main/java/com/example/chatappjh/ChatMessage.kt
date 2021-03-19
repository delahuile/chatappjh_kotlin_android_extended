package com.example.chatappjh

class ChatMessage(val content: String, val name: String, val timestamp: Long, val uid: String) {
    constructor() : this("", "", -1, "")
}