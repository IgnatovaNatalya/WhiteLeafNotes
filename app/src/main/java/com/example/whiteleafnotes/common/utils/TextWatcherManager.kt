package com.example.whiteleafnotes.common.utils

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

object TextWatcherManager {

    fun createTextWatcher(
        condition: () -> Boolean = { true },
        onAfterTextChanged: (String) -> Unit = {}
    ): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (condition()) {
                    onAfterTextChanged(s?.toString().orEmpty())
                }
            }
        }
    }

    fun setupEditText(
        editText: EditText,
        condition: () -> Boolean = { true },
        onAfterTextChanged: (String) -> Unit = {}
    ) {
        editText.addTextChangedListener(createTextWatcher(condition, onAfterTextChanged))
    }
}