package com.video.avd.ui.exitadactivity

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.avd.ui.main.home.browser.homeTab.adapter.IconVideosAdapter
import com.avd.ui.main.home.browser.homeTab.enginedialogue.model.IconItem
import com.avd.util.AdBlockerHelper
import com.avd.util.AdBlockerHelper.exitTimer
import com.avd.util.AdBlockerHelper.isPro
import com.avd.util.AdBlockerHelper.nativeAdNow
import com.avd.util.AdBlockerHelper.populateNativeAdViewInBackground
import com.avd.util.AdBlockerHelper.trackAdjustAdRevenue
import com.avd.util.AppConstant
import com.video.avd.MyApplication
import com.video.avd.R
import com.video.avd.ads.AdsManager.exit_native
import com.video.avd.databinding.ActivityExitBinding
import com.video.avd.utils.AppUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ExitActivity : AppCompatActivity() {

    lateinit var binding: ActivityExitBinding

    private val moviesWebList = listOf(
        IconItem(com.avd.R.drawable.filmzie_icon, "Filmzie"),
        IconItem(com.avd.R.drawable.plex_movie_icon, "Plex"),
        IconItem(com.avd.R.drawable.movie_123, "123Movies"),
        IconItem(com.avd.R.drawable.go_movie_icon, "GoMovies"),
    )

    private val dramasWebList = listOf(
        IconItem(com.avd.R.drawable.net_short_icon, "NetShorts"),
        IconItem(com.avd.R.drawable.reel_short_icon, "ReelShorts"),
        IconItem(com.avd.R.drawable.good_short_icon, "GoodShorts"),
        IconItem(com.avd.R.drawable.stardust_icon, "Stardust"),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppUtils.setLocate(this)
        binding = ActivityExitBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        moviesItems()
        dramaItems()
        setupButton()

        AdBlockerHelper.isProVersion.observe(this, Observer { it ->
            if (it == true) {
                binding.flAdplace.visibility = View.GONE
            } else {
                showShimmer()
            }
        })

        loadNativeAd(binding.flAdplace)

    }

    private fun setupButton() {
        binding.btnCancel.setOnClickListener {
            finish()
        }

        binding.btnExit.apply {
            startExitCountdown()
            setOnClickListener {
                if (isEnabled) {
                    finishAffinity() // or exitProcess(0)
                }
            }
        }


    }

    private fun AppCompatButton.startExitCountdown() {
        isEnabled = false
        lifecycleScope.launch {
            try {
                if(exitTimer<1){
                    // Final state update
                    text = "Exit"
                    isEnabled = true
                    setBackgroundResource(R.drawable.bg_btn_exit)
                    setTextColor(ContextCompat.getColor(context, R.color.bottom_nav_selected))
                }
                else{
                    for (i in exitTimer downTo 1) {
                        text = i.toString()
                        delay(1000)
                    }
                    // Final state update
                    text = "Exit"
                    isEnabled = true
                    setBackgroundResource(R.drawable.bg_btn_exit)
                    setTextColor(ContextCompat.getColor(context, R.color.bottom_nav_selected))
                }
            } catch (e: Exception) {
                text = "Exit"
                isEnabled = true
                setBackgroundResource(R.drawable.bg_btn_exit)
                setTextColor(ContextCompat.getColor(context, R.color.bottom_nav_selected))
                e.printStackTrace()
            }
        }
    }



    private fun moviesItems() {
        val recyclerView = binding.recyclerViewMovies
        recyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = IconVideosAdapter(moviesWebList) { clickedItem ->
            finish()
        }
    }

    private fun dramaItems() {
        val recyclerView = binding.recyclerViewDrama
        recyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = IconVideosAdapter(dramasWebList) { clickedItem ->
            finish()
        }
    }

    fun loadNativeAd(container: FrameLayout? = null) {
        if (AdBlockerHelper.isProVersion.value==true) {
            binding.flAdplace?.visibility= View.GONE
            return
        }  // Skip ads for Pro users
        if (!exit_native) {
            binding.flAdplace?.visibility= View.GONE
            return
            }

        // Destroy old ad before loading new one
        nativeAdNow?.destroy()
        nativeAdNow = null

        showShimmer()

        val adUnitId = getString(R.string.Native_Exit)

        val adLoader = AdLoader.Builder(this, adUnitId)
            .forNativeAd { nativeAd ->
                nativeAdNow?.destroy()
                nativeAdNow = nativeAd
                Log.d("NativeHome", "Ad Loaded")

                if (container == null) return@forNativeAd

                val adView = LayoutInflater.from(this)
                    .inflate(
                        com.avd.R.layout.native_ad_download,
                        null
                    ) as NativeAdView

                CoroutineScope(Dispatchers.Main).launch {
                    populateNativeAdViewInBackground(nativeAd, adView)
                }

                container.removeAllViews()
                container.addView(adView)
                container.visibility = View.VISIBLE
                nativeAd.setOnPaidEventListener { adValue ->
                    trackAdjustAdRevenue(
                        adUnitId = adUnitId,
                        revenue = adValue.valueMicros / 1_000_000.0,
                        currency = adValue.currencyCode,
                        token = AppConstant.AD_IMPRESSION_TOKEN,
                        appContext = MyApplication.context
                    )
                }

                hideShimmer()
            }
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setVideoOptions(VideoOptions.Builder().build())
                    .build()
            )
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e("NativeHome", "Failed to load: ${error.message}")
                    showShimmer()
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }


    private fun showShimmer() {
        binding.shimmerViewContainer.apply {
            visibility = View.VISIBLE
            startShimmer()
        }
    }

    private fun hideShimmer() {
        binding.shimmerViewContainer.apply {
            stopShimmer()
            visibility = View.GONE
        }
    }

}