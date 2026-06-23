package com.video.avd.ui.apppurchase

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.video.avd.R

class AppPurchaseDialogue : DialogFragment() {


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val inflater = requireActivity().layoutInflater
        val dialogView = inflater.inflate(R.layout.app_purchase_dialogue, null)



        val alertDialog = builder.setView(dialogView).create()



        return alertDialog

    }
}