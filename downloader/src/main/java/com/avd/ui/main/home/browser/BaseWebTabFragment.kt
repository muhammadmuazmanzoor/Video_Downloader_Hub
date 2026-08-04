package com.avd.ui.main.home.browser

import android.content.Context
import android.content.Intent
import android.view.Gravity
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import androidx.databinding.Observable
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import com.avd.ui.main.home.browser.social.SocialPlatform
import com.avd.ui.main.home.browser.social.SocialPlatformDownloadFragment
import com.avd.R
import com.avd.browserkit.api.BrowserKit
import com.avd.ui.main.base.BaseFragment
import com.avd.ui.main.history.HistoryFragment
import com.avd.ui.main.proxies.ProxiesFragment
import com.avd.ui.main.settings.SettingFragmentNew
import com.avd.ui.main.settings.SettingsViewModel
import com.avd.util.AdBlockerHelper.interHome
import com.avd.util.AdBlockerHelper.inter_browser
import com.avd.util.AdBlockerHelper.inter_home_high
import com.avd.util.AdBlockerHelper.inter_home_normal
import com.avd.util.AdBlockerHelper.loadFallbackInterstitialAd
import com.avd.util.AdBlockerHelper.showInterstitial
import com.avd.util.AppLogger
import com.avd.util.CommunicateWithActivity
import com.avd.util.DownloaderModuleNavigator
import kotlinx.coroutines.launch

abstract class BaseWebTabFragment : BaseFragment() {

    var settingsViewModel: SettingsViewModel? = null
    private var host: CommunicateWithActivity? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        settingsViewModel= DownloaderModuleNavigator.settingsViewModel
        try {
            host = context as? CommunicateWithActivity ?: error("Activity must implement HostActions")
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    private var popupMenu: PopupMenu? = null

    abstract fun shareWebLink()

    /**
     * Screens that reach BrowserKit's browser (rather than the in-app download history)
     * override this to expose the browsing-history entry in the overflow menu.
     */
    open val showsBrowserHistoryMenuItem: Boolean = false

    fun buildWebTabMenu(browserMenu: View, isShareItemVisible: Boolean) {
        val isdesk= settingsViewModel?.isDesktopMode?.get()
        // Rebuild the popup for the current anchor view to avoid stale references after view recreation.
        popupMenu = buildPopupMenu(browserMenu)
            val shareMenuItem = popupMenu!!.menu.findItem(R.id.share_link)
            val desktopMenuItem = popupMenu!!.menu.findItem(R.id.desktop_mode)
            val browserHistoryMenuItem = popupMenu!!.menu.findItem(R.id.browser_history_menu_item)
//            val isAdblockMenuItem = popupMenu!!.menu.getItem(4)
            val isAdBlocker = settingsViewModel?.isAdBlocker
        if (isdesk != null) {
            desktopMenuItem?.isChecked =isdesk
        }
//            isAdblockMenuItem.isChecked = isAdBlocker.get() == true
            popupMenu!!.setForceShowIcon(true)
            isAdBlocker?.addOnPropertyChangedCallback(object :
                Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
//                    lifecycleScope.launch(Dispatchers.Main) {
//                        isAdblockMenuItem.isChecked = isAdBlocker.get() == true
//                    }
                }
            })

            shareMenuItem?.isVisible = isShareItemVisible
            browserHistoryMenuItem?.isVisible = showsBrowserHistoryMenuItem
    }

    fun showPopupMenu() {

        popupMenu?.show()
       // mainActivity.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
    }

    open fun setIsDesktop(isDesktop: Boolean) {
       settingsViewModel?.setIsDesktopMode(isDesktop)
    }

    private fun buildPopupMenu(view: View): PopupMenu {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val popupMenu = PopupMenu(ContextThemeWrapper(activity,R.style.PopupMenu), view)

        popupMenu.gravity = Gravity.END
        popupMenu.menuInflater.inflate(R.menu.menu_browser, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {

                R.id.share_link -> {
                    shareWebLink()
                    true
                }

                R.id.browser_history_menu_item -> {
                    BrowserKit.launchHistory(requireContext())
                    true
                }

             /*   R.id.history_screen_menu_item -> {
                    try {
                        if (host != null){
                            host?.hideBottomBar()
                        }
                    }catch (e:Exception){
                        e.printStackTrace()
                    }
                    navigateToHistory()
                    true
                }*/

                R.id.desktop_mode -> {
                    menuItem.isChecked = !menuItem.isChecked
                    setIsDesktop(menuItem.isChecked)
                    false
                }

              /*  R.id.settings -> {
                    navigateToSettings()
                    true
                }

                R.id.help -> {
                    navigateToHelp()
                    true
                }*/

//                R.id.proxies -> {
////                    navigateToProxies()
//                    true
//                }

//                R.id.ad_blocker -> {
//                    val isAdBlockerOn = !menuItem.isChecked
//                    menuItem.isChecked = isAdBlockerOn
//                    mainActivity.settingsViewModel.setIsAdBlockerOn(isAdBlockerOn)
//                    if (isAdBlockerOn) {
//                        initializeAdBlocker()
//                    }
//                    true
//                }

//                R.id.is_dark -> {
//                    mainActivity.settingsViewModel.setIsDarkMode(!mainActivity.settingsViewModel.isDarkMode.get())
//                    true
//                }

                else -> false
            }
        }

        return popupMenu
    }

    private fun initializeAdBlocker() {
//        AdsInitializerHelper.initializeAdBlocker(
//            mainActivity.adBlockHostsRepository,
//            mainActivity.sharedPrefHelper,
//            lifecycle.coroutineScope
//        )
    }

    private fun navigateToHistory() {
        if(interHome!=null) {
            interHome?.let {
                showInterstitial(true, it, requireActivity(), {
                    try {
                        val currentFragment = this
                        val activityFragmentContainer =
                            currentFragment.activity?.findViewById<FragmentContainerView>(R.id.fragment_container_view)
                        activityFragmentContainer?.let {
                            val transaction =
                                currentFragment.requireActivity().supportFragmentManager.beginTransaction()
                            transaction.replace(it.id, HistoryFragment.newInstance())
                            transaction.addToBackStack("history")
                            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                            transaction.commit()
                        }
                    } catch (e: ClassCastException) {
                        AppLogger.d("Can't get the fragment manager with this")
                    }
                },inter_browser)
            }

        }
        else{
            loadFallbackInterstitialAd(requireActivity(), requireActivity().resources.getString(R.string.Interstitial_Home_ID_High), requireActivity().resources.getString(R.string.Interstitial_Home_ID),inter_home_high,inter_home_normal,{
                interHome=it
            },{
                interHome=it
            })
            try {
                val currentFragment = this
                val activityFragmentContainer =
                    currentFragment.activity?.findViewById<FragmentContainerView>(R.id.fragment_container_view)
                activityFragmentContainer?.let {
                    val transaction =
                        currentFragment.requireActivity().supportFragmentManager.beginTransaction()
                    transaction.replace(it.id, HistoryFragment.newInstance())
                    transaction.addToBackStack("history")
                    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    transaction.commit()
                }
            } catch (e: ClassCastException) {
                AppLogger.d("Can't get the fragment manager with this")
            }
        }

    }

    fun shareLink(url: String?) {
//        ShareCompat.IntentBuilder(mainActivity).setType("text/plain").setChooserTitle("Share Link")
//            .setText(url).startChooser()
    }

     fun navigateToSettings() {
         if(interHome!=null) {
             interHome?.let {
                 showInterstitial(true, it, requireActivity(), {
                     try {
                         val currentFragment = this
                         val activityFragmentContainer = currentFragment.activity?.findViewById<FragmentContainerView>(R.id.fragment_container_view)
                         activityFragmentContainer?.let {
                             val transaction = currentFragment.requireActivity().supportFragmentManager.beginTransaction()
                             transaction.replace(it.id, SettingFragmentNew.newInstance())
                             transaction.addToBackStack("settings")
                             transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                             transaction.commit()
                         }
                     } catch (e: ClassCastException) {
                         AppLogger.d("Can't get the fragment manager with this")
                     }
                 },inter_browser)
             }

         }
         else{
             try {
                 val currentFragment = this
                 val activityFragmentContainer = currentFragment.activity?.findViewById<FragmentContainerView>(R.id.fragment_container_view)
                 activityFragmentContainer?.let {
                     val transaction = currentFragment.requireActivity().supportFragmentManager.beginTransaction()
                     transaction.replace(it.id, SettingFragmentNew.newInstance())
                     transaction.addToBackStack("settings")
                     transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                     transaction.commit()
                 }
             } catch (e: ClassCastException) {
                 AppLogger.d("Can't get the fragment manager with this")
             }
         }

    }

    private fun navigateToProxies() {
        try {
            val currentFragment = this
            val activityFragmentContainer =
                currentFragment.activity?.findViewById<FragmentContainerView>(R.id.fragment_container_view)
            activityFragmentContainer?.let {
                val transaction =
                    currentFragment.requireActivity().supportFragmentManager.beginTransaction()
                transaction.add(it.id, ProxiesFragment.newInstance())
                transaction.addToBackStack("proxies")
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                transaction.commit()
            }
        } catch (e: ClassCastException) {
            AppLogger.d("Can't get the fragment manager with this")
        }
    }


    fun isSocialPlatformDownloadFragmentVisible(): Boolean {
        val container = activity?.findViewById<FragmentContainerView>(R.id.fragment_container_view)
            ?: return false
        val current = requireActivity().supportFragmentManager.findFragmentById(container.id)
        return current is SocialPlatformDownloadFragment && current.isAdded && current.isVisible
    }

     fun navigateToSocialPlatformDownload(initialUrl: String) {
         navigateToSocialPlatformDownloadInternal(
             SocialPlatformDownloadFragment.newInstance(initialUrl)
         )
     }

     fun navigateToSocialPlatformDownloadFromIcon(platform: SocialPlatform) {
         navigateToSocialPlatformDownloadInternal(
             SocialPlatformDownloadFragment.newInstanceFromIcon(platform)
         )
     }

     private fun navigateToSocialPlatformDownloadInternal(fragment: SocialPlatformDownloadFragment) {
         try {
             host?.hideBottomBar()
         } catch (e: Exception) {
             e.printStackTrace()
         }
         navigateInMainContainer(fragment, SocialPlatformDownloadFragment.TAG)
     }

     protected fun navigateInMainContainer(fragment: androidx.fragment.app.Fragment, backStackTag: String) {
         try {
             val hostActivity = activity ?: return
             val activityFragmentContainer =
                 hostActivity.findViewById<FragmentContainerView>(R.id.fragment_container_view) ?: return
             val fragmentManager = hostActivity.supportFragmentManager

             viewLifecycleOwner.lifecycleScope.launch {
                 if (!isAdded || hostActivity.isFinishing || hostActivity.isDestroyed) return@launch
                 val transaction = fragmentManager.beginTransaction()
                     .replace(activityFragmentContainer.id, fragment, backStackTag)
                     .addToBackStack(backStackTag)
                     .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)

                 commitTransactionSafely(fragmentManager, transaction)
             }
         } catch (e: Exception) {
             AppLogger.d("Can't navigate to social download screen: $e")
         }
     }

     private fun commitTransactionSafely(
         fragmentManager: FragmentManager,
         transaction: FragmentTransaction
     ) {
         if (fragmentManager.isStateSaved) {
             transaction.commitAllowingStateLoss()
         } else {
             transaction.commit()
         }
         fragmentManager.executePendingTransactions()
     }

     fun navigateToHelp() {
        try {
            val activityClass = Class.forName("com.video.avd.whatsapprefrerence.WhatsappRefActivity")
            val intent = Intent(requireContext(), activityClass)
            intent.putExtra("where", "download")
            startActivity(intent)
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }catch (e : Exception){
            e.printStackTrace()
        }
    }


}
