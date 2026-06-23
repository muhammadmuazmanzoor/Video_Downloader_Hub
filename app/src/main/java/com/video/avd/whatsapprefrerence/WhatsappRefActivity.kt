package com.video.avd.whatsapprefrerence

//import com.myAllVideoBrowser.ui.main.downloder_queue.ui.main.DownloadsFragment
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.avd.util.Memory.changeStatusBarColor
import com.video.avd.R
import com.video.avd.databinding.ActivityWhatsappRefBinding
import com.video.avd.ui.inapp.DialogFragments
import com.video.avd.ui.languages.LanguagesSelectionFragment
import com.video.avd.ui.status_saver.statusnew.StatusHomeFragment
import com.video.avd.ui.download_guidance.DownloadGuidanceHolderFragment
import com.video.avd.utils.AppUtils
import com.video.avd.utils.AppUtils.hideNavigationBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WhatsappRefActivity : AppCompatActivity() {
    var binding: ActivityWhatsappRefBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityWhatsappRefBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        try {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // hideSystemUI()
        changeStatusBarColor(com.avd.R.color.black, this@WhatsappRefActivity, true)
        hideNavigationBar()
        val intent=intent
        val where = intent.getStringExtra("where")
        val fragment : Fragment = if (where == "download"){
                DownloadGuidanceHolderFragment()
        }else if (where == "propanel"){
            DialogFragments()
        } else if (where == "language"){
            LanguagesSelectionFragment()
        }
        else{
            StatusHomeFragment()
        }
        Log.e("checkLanguage","Not Korean :${AppUtils.localeLanguage}")
        supportFragmentManager.beginTransaction()
            .replace(R.id.whats_container_view, fragment)
            .commit()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

}