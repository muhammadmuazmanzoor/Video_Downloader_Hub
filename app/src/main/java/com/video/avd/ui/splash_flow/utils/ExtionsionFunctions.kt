package com.video.avd.ui.splash_flow.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.widget.ViewPager2
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieCompositionFactory
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.snackbar.Snackbar
import java.io.UnsupportedEncodingException
import java.net.URLEncoder


fun View.fadeIn(duration: Long = 200, delay: Long = 200, distance: Float = -60f) {
    this.apply {
        alpha = 0f
        translationY = distance // Move the view up by `distance` (default is -100 pixels)
        visibility = View.VISIBLE
        animate().alpha(1f) // Fade in
            .translationY(0f) // Move to the original position
            .setDuration(duration).setStartDelay(delay).setListener(null)
    }
}

fun View.fadeOut(duration: Long = 200, delay: Long = 200, distance: Float = -60f) {
    this.apply {
        animate().alpha(0f) // Fade out
            .translationY(distance) // Move the view up by `distance`
            .setDuration(duration).setStartDelay(delay)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    visibility = View.GONE
                    translationY = 0f // Reset the position
                }
            })
    }
}

fun View.fadesIn(duration: Long = 250) {
    this.apply {
        // Make sure the view is not interfering with layout changes
        visibility = View.INVISIBLE

        val fadeIn = AlphaAnimation(0f, 1f).apply {
            this.duration = duration
            fillAfter = true // Keeps the view at its final state
        }

        fadeIn.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
                visibility = View.VISIBLE // Ensures it's visible when animation starts
            }

            override fun onAnimationEnd(animation: Animation?) {
                visibility =
                    View.VISIBLE // Makes sure the visibility is set correctly after animation ends
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })

        startAnimation(fadeIn)
    }
}


// Extension function to fade out (hide) a view
fun View.fadesOut(duration: Long = 150) {
    this.apply {
        val fadeOut = AlphaAnimation(1f, 0f)
        fadeOut.duration = duration
        fadeOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                visibility = View.GONE
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })
        startAnimation(fadeOut)
    }
}

// Extension function to toggle visibility with fade in/out effect
fun View.toggleFade(duration: Long = 150) {
    if (visibility == View.VISIBLE) {
        fadesOut(duration)
    } else {
        fadesIn(duration)
    }
}


fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.GONE
}

fun EditText.changeImeActionLable() {
    setImeActionLabel("Done", KeyEvent.KEYCODE_ENTER)

}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun View.hideKeyboard() {
    val inputMethodManager =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
}

fun View.enable() {
    isEnabled = true
    isActivated = true
    isClickable = true
}


fun View.disable() {
    isEnabled = false
    isActivated = false
    isClickable = false
}

fun RadioButton.check() {
    isChecked = true
}

fun RadioButton.unCheck() {
    isChecked = false
}

fun View.activate() {
    isActivated = true
}

fun View.deactivate() {
    isActivated = false
}


fun View.makeClickAble() {
    isClickable = true
    isEnabled = true
}

fun View.makeUnclickable() {
    isClickable = false
    isEnabled = false
}

fun View.showSnack(
    message: String, action: String = "", actionListener: () -> Unit = {}
): Snackbar {
    var snackbar = Snackbar.make(this, message, Snackbar.LENGTH_SHORT)
    if (action != "") {
        snackbar.duration = Snackbar.LENGTH_INDEFINITE
        snackbar.setAction(action) {
            actionListener()
            snackbar.dismiss()
        }
    }
    snackbar.show()
    return snackbar
}

fun String.replaceWords(wordsToReplace: List<String>, replacement: (String) -> String): String {
    var modifiedText = this
    wordsToReplace.forEach { word ->
        modifiedText = modifiedText.replace(word, replacement(word), ignoreCase = true)
    }
    return modifiedText
}

fun <T : Any> Context.startActivity(clazz: Class<T>) {
    val intent = Intent(this, clazz)
    startActivity(intent)
}

fun FragmentActivity.showToastForAuth(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun FragmentActivity.showToastForAuth(message: Int) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}


fun ViewPager2.autoScroll(interval: Long) {
    val handler = Handler()
    var scrollPosition = 0

    val runnable = object : Runnable {
        override fun run() {
            val count = adapter?.itemCount ?: 0
            if (count > 0) {
                setCurrentItem(scrollPosition++ % count, true)
                handler.postDelayed(this, interval)
            }
        }
    }

    registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            scrollPosition = position + 1
        }
    })

    handler.post(runnable)
}

fun ImageView.loadWithGlide(image: Int) {
    Glide.with(this).load(image).into(this)
}

fun ImageView.loadWithGlide(image: String) {
    Glide.with(this).load(image).into(this)
}

fun FragmentActivity.showToast(message: String) {    try {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
}

fun FragmentActivity.showErrorToastCompulsory(message: String) {
    try {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun FragmentActivity.showFeedbackToast(message: String) {
    try {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun FragmentActivity.showToast(message: Int) {
//    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Activity.showToast(message: String) {
//    Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
}

fun EditText.dismissKeyboard() {
    val imm: InputMethodManager? =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
    imm?.hideSoftInputFromWindow(windowToken, 0)
}

fun loadAndPlayLottieAnimation(
    view: LottieAnimationView, rawResId: Int, context: Context
) {
    LottieCompositionFactory.fromRawRes(context, rawResId)?.addListener {
            view.setComposition(it)
            view.playAnimation()
        }
}

fun loadAndCancelLottieAnimation(
    view: LottieAnimationView, rawResId: Int, context: Context
) {
    LottieCompositionFactory.fromRawRes(context, rawResId)?.addListener {
            view.setComposition(it)
            view.cancelAnimation()
        }
}



fun ImageView.loadImage(url: String) {
    Glide.with(this).load(url).into(this)
}


fun ImageView.load(imageUrl: String) {
    Glide.with(this).load(imageUrl).thumbnail(0.1f).into(this)
}

fun ImageView.load(imageUrl: Drawable) {
    Glide.with(this).load(imageUrl).into(this)
}

fun ImageView.load(imageUrl: Uri) {
    Glide.with(this).load(imageUrl).into(this)
}

fun ImageView.loadBitmap(imageUrl: Bitmap) {
    try {
        Glide.with(this).load(imageUrl)
//            .dontTransform()
            .into(this);
    } catch (ex: Exception) {
        ex.printStackTrace()
        Log.d("ZZZ:::", "exception: $ex")
    }
}


fun ImageView.loadBitmapFromVariation(
    imageResource: Any, // Accept Any to handle both String and Int
    placeholderResId: Int, errorResId: Int, approve: Boolean
) {
    val requestOptions = RequestOptions()
        .placeholder(placeholderResId)
        .error(errorResId)

    if (!approve) {
        Glide.with(context).load(imageResource) // Load using imageResource directly
//            .apply(RequestOptions.bitmapTransform(BlurTransformation(25, 18)))
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .transition(DrawableTransitionOptions.withCrossFade()).into(this)
    } else {
        Glide.with(context).load(imageResource) // Load using imageResource directly
            .dontTransform().apply(requestOptions).diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .transition(DrawableTransitionOptions.withCrossFade()).into(this)
    }

    invalidate()
}


fun ImageView.loadingImageForYou(context: Context, url: String) {
    Glide.with(context).load(url).into(this)
}

fun String.cacheImageBeforeLoading(context: Context) {
    Glide.with(context).load(this).preload()
}

fun ImageView.loadWithYourPlaceholder(imageUrl: String, drawable: Int) {
    Glide.with(this).load(imageUrl).placeholder(drawable).into(this)
}

fun TextView.setTextColorRes(colorResId: Int) {
    val color = ContextCompat.getColor(context, colorResId)
    setTextColor(color)
}

fun ImageView.setTextColorRes(colorResId: Int) {
    val color = ContextCompat.getColor(context, colorResId)
    setColorFilter(color)
}

infix fun Activity.openLink(link: String) {
    try {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        startActivity(browserIntent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}


/**
 * Opens a given URL link in the device's default browser.
 *
 * This function creates an implicit intent with the `ACTION_VIEW` action
 * and the provided URL, then starts the browser activity to open the link.
 * It handles any exceptions that might occur during the intent creation or
 * activity start process, such as malformed URLs.
 *
 * Usage:
 * ```
 * activity openLink "https://www.example.com"
 * ```
 *
 * @param link The URL to open in the browser as a [String].
 * @throws Exception if there is an issue with starting the browser activity.
 */
infix fun FragmentActivity.openLink(link: String) {
    try {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        startActivity(browserIntent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}



fun String.encodeUrl(): String {
    return try {
        URLEncoder.encode(this, "UTF-8")
    } catch (e: UnsupportedEncodingException) {
        // Handle encoding exception
        ""
    }
}

fun ImageView.animateImageView(): ValueAnimator {
    val anim = ValueAnimator.ofFloat(1f, 1.07f) // Slight scale for heartbeat effect
    anim.duration = 500 // Duration for each pulse
    anim.repeatCount = ValueAnimator.INFINITE // Repeat indefinitely
    anim.repeatMode = ValueAnimator.REVERSE // Reverse to create the heartbeat effect

    anim.addUpdateListener { animation ->
        val scale = animation.animatedValue as Float
        scaleX = scale
        scaleY = scale
    }

// Start the animation
    anim.start()

    return anim
}


