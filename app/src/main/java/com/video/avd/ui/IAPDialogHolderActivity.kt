package com.video.avd.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.video.avd.R
import com.video.avd.databinding.ActivityDialogHolderIapBinding
import com.video.avd.utils.AppUtils.hideNavigationBar
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class IAPDialogHolderActivity : AppCompatActivity() {
    var binding: ActivityDialogHolderIapBinding? = null
    private var navController: NavController? = null
    private var navHostFragment: NavHostFragment? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDialogHolderIapBinding.inflate(layoutInflater)
        setContentView(binding?.root)
       // this.hideSystemUI()
        hideNavigationBar()
        navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_iap) as NavHostFragment
        navController = navHostFragment?.navController
    }

    override fun onBackPressed() {
        finish()
    }

}