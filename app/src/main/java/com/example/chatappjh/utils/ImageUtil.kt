package com.example.chatappjh.utils

import android.R.attr.maxHeight
import android.R.attr.maxWidth
import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.*


object ImageUtil {
    private val storageInstance: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

    private val currentUserReference: StorageReference
    get() = storageInstance.reference.child((FirebaseAuth.getInstance().currentUser?.uid ?: throw NullPointerException("UID is null")))

    fun uploadMessageImage(ImageBytes: ByteArray, thumbImage: Boolean, onSuccess: (imagePath: String) -> Unit){

            val ref = if (thumbImage) currentUserReference.child(("messages/${UUID.nameUUIDFromBytes(ImageBytes)}_thumb")) else currentUserReference.child(("messages/${UUID.nameUUIDFromBytes(ImageBytes)}_expanded"))
            ref.putBytes(ImageBytes)
                .addOnSuccessListener {
                    onSuccess(ref.path)
                }
    }

    fun pathToReference(path: String) = storageInstance.getReference(path)

    fun scaleBitmap(bm: Bitmap, maxHeight: Int, maxWidth: Int): Bitmap {
        var bm = bm

        var width = bm.width
        var height = bm.height
        Log.v("Pictures", "Width and height are $width--$height")
        if (width > height) {
            // landscape
            val ratio = width.toFloat() / maxWidth
            width = maxWidth
            height = (height / ratio).toInt()
        } else if (height > width) {
            // portrait
            val ratio = height.toFloat() / maxHeight
            height = maxHeight
            width = (width / ratio).toInt()
        } else {
            // square
            height = maxHeight
            width = maxWidth
        }
        Log.v("Pictures", "after scaling Width and height are $width--$height")
        bm = Bitmap.createScaledBitmap(bm, width, height, true)
        return bm
    }

}