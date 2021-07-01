package com.example.chatappjh.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class User(val name: String, val uid: String): Parcelable {
    constructor() : this("", "")
}