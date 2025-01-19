package com.example.drawit.utils.viewutils

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.example.drawit.R

fun showRationaleDialog(context: Context, message: String, title: String) {
    val builder: AlertDialog.Builder = AlertDialog.Builder(context)
    builder.setTitle(title)
    builder.setMessage(message)
    builder.setPositiveButton(context.getString(R.string.ok_caps)) { dialog, _ ->
        dialog.dismiss()
    }
    builder.create().show()
}