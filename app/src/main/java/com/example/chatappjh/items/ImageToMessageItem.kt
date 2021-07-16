package com.example.chatappjh.items

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.core.graphics.scale
import com.example.chatappjh.R
import com.example.chatappjh.displayWidth
import com.example.chatappjh.models.ImageMessage
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.activity_chat.view.*
import kotlinx.android.synthetic.main.image_from_row.view.*
import kotlinx.android.synthetic.main.image_to_row.view.*
import java.text.SimpleDateFormat
import java.util.*


class ImageToMessageItem(val message: ImageMessage
): Item<GroupieViewHolder>() {

    lateinit var bitMap: Bitmap
    lateinit var expandedImageView: ImageView

    companion object {
        val TAG = "ImageToMessage"
    }

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        val thumb1View: ImageView = viewHolder.itemView.imageView_to_chatmessage

        expandedImageView = viewHolder.itemView.expanded_image_to

        val targetThumb  = object : com.squareup.picasso.Target {
            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                TODO("not implemented")
            }

            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                bitMap = bitmap!!
                thumb1View.setImageBitmap(bitMap)
            }

            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
            }
        }



        val thumbRef = Firebase.storage.reference.child(message.thumbUrl)
        val expandedRef = Firebase.storage.reference.child(message.expandedUrl)

        expandedRef.downloadUrl.addOnSuccessListener { uri ->

            val expandedURL = uri.toString()

            thumbRef.downloadUrl.addOnSuccessListener { Uri->

                val thumbURL = Uri.toString()

                Picasso.get()
                    .load(thumbURL)
                    .into(targetThumb)

                thumb1View.setTag(targetThumb)

                thumb1View.setOnClickListener{

                    val targetExpanded = object : com.squareup.picasso.Target {
                        override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                            TODO("not implemented")
                        }

                        override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                            bitMap = bitmap!!
                            expandedImageView.setImageBitmap(bitmap)
                        }

                        override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                        }
                    }

                    Picasso.get()
                        .load(expandedURL)
                        .into(targetExpanded)

                    expandedImageView.setTag(targetExpanded)

                    zoomImageFromThumb(thumb1View, expandedImageView, viewHolder)
                }
            }

        }


        viewHolder.itemView.textView_to_imageMessage.text = if(message.timestamp.seconds.toString().length==10) "${message.name}   ${getDateTimeKotlin(message.timestamp.seconds)}" else "${message.name}   ${getDateTimeReact(message.timestamp.seconds)}"

        Log.d(TAG, "FirebaseStorage thumbnail imagepath was " + message.thumbUrl)
    }

    override fun getLayout(): Int {
        return R.layout.image_to_row
    }

    fun getImageMessage(): ImageMessage {
        return message
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

    private fun zoomImageFromThumb(thumbView: View, expandedImageView: ImageView, viewHolder: GroupieViewHolder) {

        // Hold a reference to the current animator,
        // so that it can be canceled mid-way.
        var currentAnimator: Animator? = null

        // The system "short" animation time duration, in milliseconds. This
        // duration is ideal for subtle animations or animations that occur
        // very frequently.

        var resources: Resources? = null
        var shortAnimationDuration = 200

        // If there's an animation in progress, cancel it
        // immediately and proceed with this one.
        currentAnimator?.cancel()

        // Load the high-resolution "zoomed-in" image.
        expandedImageView.setImageBitmap(bitMap)

        // Calculate the starting and ending bounds for the zoomed-in image.
        // This step involves lots of math. Yay, math.
        val startBoundsInt = Rect()
        val finalBoundsInt = Rect()
        val globalOffset = Point()

        // The start bounds are the global visible rectangle of the thumbnail,
        // and the final bounds are the global visible rectangle of the container
        // view. Also set the container view's offset as the origin for the
        // bounds, since that's the origin for the positioning animation
        // properties (X, Y).
        thumbView.getGlobalVisibleRect(startBoundsInt)
        viewHolder.itemView.containerTo.getGlobalVisibleRect(finalBoundsInt, globalOffset)
        startBoundsInt.offset(-globalOffset.x, -globalOffset.y)
        finalBoundsInt.offset(-globalOffset.x, -globalOffset.y)

        val startBounds = RectF(startBoundsInt)
        val finalBounds = RectF(finalBoundsInt)

        // Adjust the start bounds to be the same aspect ratio as the final
        // bounds using the "center crop" technique. This prevents undesirable
        // stretching during the animation. Also calculate the start scaling
        // factor (the end scaling factor is always 1.0).
        val startScale: Float
        if ((finalBounds.width() / finalBounds.height() > startBounds.width() / startBounds.height())) {
            // Extend start bounds horizontally
            startScale = startBounds.height() / finalBounds.height()
            val startWidth: Float = startScale * finalBounds.width()
            val deltaWidth: Float = (startWidth - startBounds.width()) / 2
            startBounds.left -= deltaWidth.toInt()
            startBounds.right += deltaWidth.toInt()
        } else {
            // Extend start bounds vertically
            startScale = startBounds.width() / finalBounds.width()
            val startHeight: Float = startScale * finalBounds.height()
            val deltaHeight: Float = (startHeight - startBounds.height()) / 2f
            startBounds.top -= deltaHeight.toInt()
            startBounds.bottom += deltaHeight.toInt()
        }

        // Hide the thumbnail and show the zoomed-in view. When the animation
        // begins, it will position the zoomed-in view in the place of the
        // thumbnail.
        thumbView.alpha = 0f
        expandedImageView.visibility = View.VISIBLE

        // Set the pivot point for SCALE_X and SCALE_Y transformations
        // to the top-left corner of the zoomed-in view (the default
        // is the center of the view).
        expandedImageView.pivotX = 0f
        expandedImageView.pivotY = 0f

        // Construct and run the parallel animation of the four translation and
        // scale properties (X, Y, SCALE_X, and SCALE_Y).
        currentAnimator = AnimatorSet().apply {
            play(
                ObjectAnimator.ofFloat(
                    expandedImageView,
                    View.X,
                    startBounds.left,
                    finalBounds.left)
            ).apply {
                with(ObjectAnimator.ofFloat(expandedImageView, View.Y, startBounds.top, finalBounds.top))
                with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X, startScale, 1f))
                with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_Y, startScale, 1f))
            }
            duration = shortAnimationDuration.toLong()
            interpolator = DecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {

                override fun onAnimationEnd(animation: Animator) {
                    currentAnimator = null
                }

                override fun onAnimationCancel(animation: Animator) {
                    currentAnimator = null
                }
            })
            start()
        }

        // Upon clicking the zoomed-in image, it should zoom back down
        // to the original bounds and show the thumbnail instead of
        // the expanded image.
        expandedImageView.setOnClickListener {
            currentAnimator?.cancel()

            // Animate the four positioning/sizing properties in parallel,
            // back to their original values.
            currentAnimator = AnimatorSet().apply {
                play(ObjectAnimator.ofFloat(expandedImageView, View.X, startBounds.left)).apply {
                    with(ObjectAnimator.ofFloat(expandedImageView, View.Y, startBounds.top))
                    with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X, startScale))
                    with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_Y, startScale))
                }
                duration = shortAnimationDuration.toLong()
                interpolator = DecelerateInterpolator()
                addListener(object : AnimatorListenerAdapter() {

                    override fun onAnimationEnd(animation: Animator) {
                        thumbView.alpha = 1f
                        expandedImageView.visibility = View.GONE
                        currentAnimator = null
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        thumbView.alpha = 1f
                        expandedImageView.visibility = View.GONE
                        currentAnimator = null
                    }
                })
                start()
            }
        }
    }

}