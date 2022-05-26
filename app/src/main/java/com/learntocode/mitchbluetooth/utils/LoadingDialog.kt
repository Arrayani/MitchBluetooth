package com.learntocode.mitchbluetooth.utils

import com.learntocode.mitchbluetooth.R
import android.app.Activity
import android.app.AlertDialog
import android.os.Handler
import com.learntocode.mitchbluetooth.MainActivity

class LoadingDialog(val mActivity: MainActivity) {
    private lateinit var isdialog :AlertDialog
    fun startLoading(){
     /**set view*/
        val infalter= mActivity.layoutInflater
        val dialogView = infalter.inflate(R.layout.dialog_progress,null)
    /** set Dialog */
        val bulider=AlertDialog.Builder(mActivity)
        bulider.setView(dialogView)
        bulider.setCancelable(false)
        isdialog = bulider.create()
        isdialog.show()
    }

    fun isDismiss(){
        isdialog.dismiss()
    }

}