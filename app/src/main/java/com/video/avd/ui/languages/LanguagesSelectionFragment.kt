package com.video.avd.ui.languages

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.avd.util.AdBlockerHelper
import com.avd.util.AdBlockerHelper.isProVersion
import com.google.android.gms.ads.nativead.NativeAd
import com.video.avd.BuildConfig
import com.video.avd.R
import com.video.avd.constent.isSplash
import com.video.avd.constent.splashAdClick
import com.video.avd.databinding.FragmentLanguagesSelectionBinding
import com.video.avd.ads.AdsHelper
import com.video.avd.ads.AdsHelper.langNative1Enabled
import com.video.avd.ads.AdsHelper.langNativeHigh1Enabled
import com.video.avd.ads.AdsManagerKit
import com.video.avd.ads.AdsManagerKit.loadWithFallback
import com.video.avd.ui.MainActivity
import com.video.avd.utils.AppPreference
import com.video.avd.utils.AppUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class LanguagesSelectionFragment : Fragment(), LanguageSelectionAdapter.LanguageSelectionClickListener {

    private var binding: FragmentLanguagesSelectionBinding? = null
    private var mActivity: FragmentActivity? = null
    private var selectedLanguage = ""
    private var selectedPosition = 0 //English
    private var systemLanguage = ""
    private var languageAdapter: LanguageSelectionAdapter? = null

    var languagenativeAd2: NativeAd? = null
    var languagenativeAdhigh2: NativeAd? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        AppUtils.setLocate(requireActivity())
        binding = FragmentLanguagesSelectionBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AppUtils.fbEvents("view_language", "Language",mActivity)
        if(AdBlockerHelper.isProVersion.value == true){
            binding?.adViewLayout?.visibility = View.GONE
        }
       /* mActivity?.let {activity->
            if (AdBlockerHelper.isProVersion.value != true && NetworkUtils.isOnline(activity)) {
                if (GlobalValues.is24hourEnabled.value == false) {
                    showShimmer(true) // Show shimmer before loading

                }
            }
        }*/
        binding?.langLoading?.visibility = View.VISIBLE
        if (isProVersion.value!=true) {
            lifecycleScope.launch {
                delay(1500)
                binding?.bottomad?.visibility = View.GONE
                binding?.langLoading?.visibility = View.GONE
            }

        } else {
            binding?.langLoading?.visibility = View.GONE
            binding?.bottomad?.visibility = View.GONE
        }
        mActivity?.let { activity ->
            setupClickListners()
            setDataAndAdapter(activity)
            getinitialData()
            showShimmer(false)
          /*  if (AdBlockerHelper.isProVersion.value != true) {
                showShimmer(true)
                loadLanguageNativeAd()
                loadLanguageNativeAd2()
            }*/

        }
        if (!isSplash && mActivity is MainActivity) {
            AppUtils.getMain(mActivity).hidebottombar()
            Log.d("ActivityCheck", "isSplash: $isSplash")
        }

        setupHeader()

        // Set layout direction based on current locale
        view.layoutDirection = if (Locale.getDefault().language == "ar") {
            View.LAYOUT_DIRECTION_RTL
        } else {
            View.LAYOUT_DIRECTION_LTR
        }

        binding?.textView20?.setOnClickListener {
            AppUtils.firebaseUserAction("done_click_Language", "languagescreen")
            if (selectedLanguage.isNotEmpty()) {
                applyLanguage()
            }
        }
        binding?.textView21?.setOnClickListener {
            Toast.makeText(context,
                getString(R.string.please_select_a_language_first), Toast.LENGTH_SHORT).show()
        }

        setSelector()
    }

    private fun setupHeader() {
        binding?.textView19?.text = getString(R.string.change_language)
        binding?.textView19?.textDirection = when (Locale.getDefault().language) {
            "ar" -> View.TEXT_DIRECTION_RTL
            else -> View.TEXT_DIRECTION_LTR
        }
        binding?.imageView9?.visibility = View.VISIBLE
        binding?.imageView9?.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setDataAndAdapter(activity: FragmentActivity) {
        val list = arrayListOf<LanguageSelectionModel>()
        list.add(LanguageSelectionModel("en", "English", false, resources.getDrawable(R.drawable.ic_us)))
        list.add(LanguageSelectionModel("ar", "العربية", false, resources.getDrawable(R.drawable.ic_saudia)))
        list.add(LanguageSelectionModel("ko", "한국인", false,resources.getDrawable(R.drawable.ic_korea)))
        list.add(LanguageSelectionModel("ja", "日本語", false,resources.getDrawable(R.drawable.ic_japan)))
        list.add(LanguageSelectionModel("es", "Español", false,resources.getDrawable(R.drawable.ic_spain)))
        list.add(LanguageSelectionModel("in", "Indonesian", false,resources.getDrawable(R.drawable.ic_indonesia)))
        list.add(LanguageSelectionModel("pt", "Portuguese", false,resources.getDrawable(R.drawable.ic_portugal)))
        list.add(LanguageSelectionModel("fr", "Français", false,resources.getDrawable(R.drawable.ic_france)))
        list.add(LanguageSelectionModel("vi", "Tiếng Việt", false,resources.getDrawable(R.drawable.ic_vietnam)))
        list.add(LanguageSelectionModel("ru", "Русский", false,resources.getDrawable(R.drawable.ic_russia)))
        list.add(LanguageSelectionModel("tr", "Türkçe", false,resources.getDrawable(R.drawable.ic_turkey)))
        list.add(LanguageSelectionModel("ms", "Melayu", false,resources.getDrawable(R.drawable.ic_malaysia)))
        list.add(LanguageSelectionModel("th", "แบบไทย", false,resources.getDrawable(R.drawable.ic_thailand)))
        list.add(LanguageSelectionModel("pl", "Polski", false,resources.getDrawable(R.drawable.ic_poland)))
        for (item in list) {
            if (item.lang == getAutoLanguage(activity)) {
                item.name += " ( Auto )"
                break
            }
        }
        val appLanguage = Locale.getDefault().language
        var itemToInsert: LanguageSelectionModel? = null
        for (item in list) {
            if (item.lang == appLanguage) {
                item.isSelected = false
                itemToInsert = item
                break
            }
        }
        if (itemToInsert != null) {
            list.remove(itemToInsert)
            list.add(0, itemToInsert)
            itemToInsert = null
        }
        val currentLang = AppPreference.getLanguage(activity)
        if (currentLang != null) {
//            selectedLanguage = currentLang
        }
        val adapter = LanguageSelectionAdapter(list, this)
        languageAdapter = adapter
        binding?.rvLanguage?.layoutManager = LinearLayoutManager(activity)
        binding?.rvLanguage?.adapter = adapter
    }

    private fun getinitialData() {
        mActivity?.let { activity ->
            if (AppPreference.getLanguage(activity) != null) {
                selectedLanguage = AppPreference.getLanguage(activity).toString()
            }
            setSelector()
            systemLanguage = getAutoLanguage(activity)
        }
    }

    private fun showShimmer(show: Boolean) {
        binding?.shimmerViewContainer?.let { shimmer ->
            if (show) {
                shimmer.visibility = View.VISIBLE
                shimmer.startShimmer()
                binding?.adViewLayout?.visibility = View.GONE
                binding?.bottomad?.visibility = View.VISIBLE
            } else {
                shimmer.stopShimmer()
                shimmer.visibility = View.GONE
                binding?.adViewLayout?.visibility = View.VISIBLE
                binding?.bottomad?.visibility = View.VISIBLE
            }
        }
    }



    override fun onResume() {
        super.onResume()
        if (splashAdClick) {
            splashAdClick = false
            AppUtils.firebaseUserAction("SplashAdClickResumeApp", "MainActivity")
        }
    }

    @SuppressLint("ResourceType")
    private fun setupClickListners() {
        binding?.let { binding ->
            binding.imageView9.setOnClickListener {
                findNavController().popBackStack()
            }
            binding.textView20.setOnClickListener {
                AppUtils.firebaseUserAction("Langu_done_click", "languagescreen")
                if (selectedLanguage != ""){
                    mActivity?.let {activity->
                        navigateToNext()
                        mActivity?.let {
                                Log.d("CheckRemoteFetch","False value Saved for first launch pref")
                                AppPreference.setFirstLaunch(it, isFirstLaunch = false)
                            }

                    }
                }else {
                    Toast.makeText(mActivity, "Please Select language", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun navigateToNext(){
        try {
                mActivity?.let {
                    AppPreference.setFirstLaunch(it, isFirstLaunch = false)
                    AppPreference.saveLanguage(it, selectedLanguage)
                    val intent = Intent(it, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
        }catch (e:Exception){
            e.printStackTrace()
            mActivity?.let {
                try {
                    val intent = Intent(it, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } catch (e: Exception) {
                   e.printStackTrace()
                }
            }
        }

    }

    private fun setSelector() {
        selectedPosition = when (selectedLanguage) {
            "en" -> 0
            "ar" -> 1
            "ko" -> 2
            "ja" -> 3
            "es" -> 4
            "in" -> 5
            "pt" -> 6
            "fr" -> 7
            "vi" -> 8
            "ru" -> 9
            "tr" -> 10
            "ms" -> 11
            "th" -> 12
            "pl" -> 13
            else -> 3
        }
    }
    private fun applyLanguage() {
        if (selectedLanguage.isNotEmpty()) {
            AppUtils.firebaseUserAction("Langu_done_click", "languagescreen")
            mActivity?.let { activity ->
                navigateToNext()
            }
        } else {
            Toast.makeText(mActivity, "Please Select language", Toast.LENGTH_SHORT).show()
        }
    }

    var onetime=false

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onLanguageClick(language: LanguageSelectionModel?) {
        binding?.textView20?.visibility = View.VISIBLE
        binding?.textView21?.visibility = View.INVISIBLE
        binding?.anim?.visibility = View.VISIBLE
        languageAdapter?.stopStartupHighlight()
        if (!onetime){
            onetime=true
            if (AdBlockerHelper.isProVersion.value != true) {
                Log.d("AddProo", "onLanguageClick22: ${AdBlockerHelper.isProVersion.value}")
              /*  if (languagenativeAdhigh2 != null) {
                    showNativeAdTemplate(languagenativeAdhigh2, "native_lan_high1")
                } else {
                    showNativeAdTemplate(languagenativeAd2, "native_lan1")
                }*/
            }
        }
        language?.let {
            selectedLanguage = it.lang
        }
        setSelector()
    }
    private fun getAutoLanguage(activity: FragmentActivity): String {
        val systemLan = AppPreference.getSystemDefaultLanguage(activity)
        val supportedLangs =
            listOf("en","ar", "ko", "ja", "es", "in", "pt", "fr", "vi", "ru", "tr", "ms", "th", "pl")
        var lange = if (systemLan in supportedLangs) systemLan else "en"
        return lange ?: "en"
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()
    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }

/*    fun loadLanguageNativeAd() =
        loadWithFallback(
            activity = requireActivity(),
            highFloorAdId = BuildConfig.native_language_high,
            normalAdId = BuildConfig.native_language,
            showHighfloor = langNativeHigh1Enabled,
            showNormalfloor = langNative1Enabled,
            onAdLoadedHigh = {
                showNativeAdTemplate(it, "native_lan_high")
            },
            onAdLoadedNormal = { showNativeAdTemplate(it, "native_lan") },
            onAdFailed = {
                showShimmer(false)
            }
        )

    fun loadLanguageNativeAd2() = loadWithFallback(
        activity = requireActivity(),
        highFloorAdId = BuildConfig.native_language_alt_high,
        normalAdId = BuildConfig.native_language_alt,
        showHighfloor = AdsHelper.langNativeHigh2Enabled,
        showNormalfloor = AdsHelper.langNative2Enabled,
        onAdLoadedHigh = { languagenativeAdhigh2 = it },
        onAdLoadedNormal = { languagenativeAd2 = it },
        onAdFailed = {
            showShimmer(false)
        }
    )
    private fun showNativeAdTemplate(nativeAd: NativeAd?, eventName: String) {
        showShimmer(false)
        if (AdBlockerHelper.isProVersion.value != true) {
            try {
                if (nativeAd == null) return
                binding?.adViewLayout?.visibility = View.VISIBLE
                //  AppUtils.firebaseUserAction(eventName, "languageScreen")
                Log.d("Native_Gen", "✅OnBoarding $eventName Ad show successfully")
                binding?.primary?.text = nativeAd.headline
                binding?.adBody?.text = nativeAd.body
                binding?.AdImage?.mediaContent = nativeAd.mediaContent
                binding?.cta?.text = nativeAd.callToAction

                binding?.adViewLayout?.apply {
                    headlineView = binding?.primary
                    mediaView = binding?.AdImage
                    callToActionView = binding?.cta
                    setNativeAd(nativeAd)
                }

                binding?.bottomad?.visibility = View.VISIBLE

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }*/
}


