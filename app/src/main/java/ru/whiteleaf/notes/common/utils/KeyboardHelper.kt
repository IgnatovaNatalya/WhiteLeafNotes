package ru.whiteleaf.notes.common.utils

import android.content.Context.INPUT_METHOD_SERVICE
import android.view.inputmethod.InputMethodManager
import android.widget.EditText


fun checkKeyboard(editText: EditText): Boolean {
    val imm =
        editText.context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
    return imm.isAcceptingText
}

fun showKeyboard(editText: EditText) {
    editText.let {
        val imm = editText.context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }
}

fun hideKeyboard(editText: EditText) {
    editText.let {
        val imm = editText.context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(it.windowToken, 0)
    }
}