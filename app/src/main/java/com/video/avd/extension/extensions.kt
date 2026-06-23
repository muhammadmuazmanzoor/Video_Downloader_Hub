package com.video.avd.extension

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavDirections
import androidx.navigation.findNavController
import com.video.avd.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


// Extension to keep track of the coroutine job for navigation
private var navigationJob: Job? = null

fun View.shake() {
    try {
        val shake = ObjectAnimator.ofFloat(
            this, "translationX",
            0f, -20f, 20f, -16f, 16f, -10f, 10f, -6f, 6f, 0f
        )
        shake.duration = 500
        shake.interpolator = DecelerateInterpolator()
        shake.start()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
fun FragmentActivity.nextNavigateTo(navDirections: NavDirections) {
    navigationJob?.cancel() // Cancel any ongoing navigation coroutine
    navigationJob = CoroutineScope(Dispatchers.Main).launch {
        try {
            delay(500) // Debounce time window
            findNavController(R.id.nav_host).navigate(navDirections)
        } catch (e: Exception) {
            Log.e("TAG", "Error during navigation to directions: ${e.message}", e)
        }
    }
}

fun Context.getResourceName(resId: Int): String {
    return resources.getResourceEntryName(resId)
}

fun Context.getResourceId(resName: String, resType: String = "drawable"): Int {
    return resources.getIdentifier(resName, resType, packageName)
}

fun FragmentActivity.nextNavigateWithId(id: Int, bundle: Bundle) {
    navigationJob?.cancel() // Cancel any ongoing navigation coroutine
    navigationJob = CoroutineScope(Dispatchers.Main).launch {
        try {
            delay(500) // Debounce time window
            findNavController(R.id.nav_host).navigate(id, bundle)
        } catch (e: Exception) {
           e.printStackTrace()
        }
    }
}


fun FragmentActivity.backNavigateTo() {
    navigationJob?.cancel() // Cancel any ongoing navigation coroutine
    navigationJob = CoroutineScope(Dispatchers.Main).launch {
        try {
            delay(500) // Debounce time window
            val navController = findNavController(R.id.nav_host)
            navController.popBackStack()
        } catch (e: Exception) {
            Log.e("TAG", "Error during back navigation: ${e.message}", e)
            // Additional error handling logic here, if necessary
        }
    }
}


infix fun Activity.openLink(link: String) {
    try {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        startActivity(browserIntent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun View.fadeVisibility(visibility: Int) {
    if (this.visibility != visibility) {
        this.visibility = visibility
    }
}

fun View.fadeVisibilityArrow(visibility: Int) {
    if (this.visibility != visibility) {
        this.visibility = visibility
    }
}

//fun View.fadeVisibility(visibility: Int) {
//    if (this.visibility != visibility) {
//        if (visibility == View.VISIBLE) {
//            this.alpha = 0f
//            this.visibility = View.VISIBLE
//            this.animate()
//                .alpha(1f)
//                .setDuration(300)
//                .setListener(null)
//        } else {
//            this.animate()
//                .alpha(0f)
//                .setDuration(300)
//                .withEndAction {
//                    this.visibility = visibility
//                }
//        }.start()
//    }
//}
//
//fun View.fadeVisibilityArrow(visibility: Int) {
//    if (this.visibility != visibility) {
//        if (visibility == View.VISIBLE) {
//            this.alpha = 0f
//            this.visibility = View.VISIBLE
//            this.animate()
//                .alpha(1f)
//                .setDuration(300)
//                .setListener(null)
//        } else {
//            this.animate()
//                .alpha(0f)
//                .setDuration(300)
//                .withEndAction {
//                    this.visibility = visibility
//                }
//        }.start()
//    }
//}



