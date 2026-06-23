package com.video.avd.ui.exitadactivity

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.SystemClock
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.video.avd.R
import com.video.avd.utils.AppUtils.convertMillisecondsToMinutes
import com.video.avd.utils.AppUtils.hideNavigationBar
import com.video.avd.utils.AppUtils.startTimeMillis
import com.video.avd.utils.AppUtils.totalForegroundTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ExitAdActivity : AppCompatActivity() {
    private var job: Job? = null

    companion object {
        var startcounter = MutableLiveData<Boolean>(false)
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exit_ad)
        this.hideNavigationBar()
        val cross=findViewById<ImageView>(R.id.cross)
        cross.setOnClickListener {
            finishAffinity()
        }
    }

    @SuppressLint("WrongViewCast")
    override fun onResume() {
        super.onResume()
        startcounter.observe(this){
            if (it){
                job = CoroutineScope(Dispatchers.Main).launch {
                    try {
                        // Your coroutine code here
                        // For example, a repetitive task:
                        while (isActive) {
                            val endTimeMillis = SystemClock.elapsedRealtime()
                            totalForegroundTimeMillis += endTimeMillis - startTimeMillis
                            val cross=findViewById<TextView>(R.id.third)
                            val total=convertMillisecondsToMinutes(totalForegroundTimeMillis)
                            cross.text="You use the app for a total of $total minutes this time"
                            // Execute some repeating work on the main thread
                            delay(9000) // Delay for 1 second
                            // Update UI or do something else
                            if (isActive){
                                finishAffinity()
                            }
                        }
                    }catch (e:Exception){
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        job?.cancel()
    }

    override fun onBackPressed() {
        finishAffinity()
    }

    override fun onStop() {
        super.onStop()
        if (isFinishing) {
            // User is exiting the app, finish the activity
            finishAffinity()
        }
    }

}