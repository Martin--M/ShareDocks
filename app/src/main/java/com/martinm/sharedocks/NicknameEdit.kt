package com.martinm.sharedocks

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment

class NicknameEdit(
    private val stationId: String,
    private val viewGroup: ViewGroup,
    private val textView: TextView
) :
    DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return requireActivity().let { fragmentActivity ->
            val builder = AlertDialog.Builder(fragmentActivity)
            val dialogView = layoutInflater.inflate(R.layout.nickname_dialog, viewGroup, false)
            val inputMethodManager =
                fragmentActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            val dialogText = dialogView.findViewById<EditText>(R.id.nickname_edit_text)

            dialogText.setText(textView.text)

            builder.setView(dialogView)
                .setPositiveButton(fragmentActivity.getString(R.string.dialog_nickname_button_ok)) { _, _ ->
                    val nickname = dialogText.text.toString()
                    ConfigurationHandler.storeNickname(stationId, nickname)
                    textView.text = nickname
                    inputMethodManager.hideSoftInputFromWindow(dialogView.windowToken, 0)
                    textView.visibility = View.VISIBLE
                    viewGroup.findViewById<TextView>(R.id.dock_name).visibility = View.GONE
                }
                .setNegativeButton(fragmentActivity.getString(R.string.dialog_nickname_button_cancel)) { _, _ ->
                    inputMethodManager.hideSoftInputFromWindow(dialogView.windowToken, 0)
                }
                .setNeutralButton(fragmentActivity.getString(R.string.dialog_nickname_button_remove)) { _, _ ->
                    ConfigurationHandler.storeNickname(stationId, "")
                    textView.text = ""
                    textView.visibility = View.GONE
                    viewGroup.findViewById<TextView>(R.id.dock_name).visibility = View.VISIBLE
                    inputMethodManager.hideSoftInputFromWindow(dialogView.windowToken, 0)
                }

            builder.create()
        }
    }
}