package ru.whiteleaf.notes.common.utils

import android.widget.EditText
import android.widget.ImageButton
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import ru.whiteleaf.notes.common.utils.TextWatcherManager.setupEditText

object TextWatcherScrollManager {

        fun setupScrollDetection(
            editText: EditText,
            scrollView: NestedScrollView,
            button: ImageButton,
            onVisibilityChanged: ((Boolean) -> Unit)? = null
        ) {
            button.hide()

            val scrollCheck = {
                val canScrollDown = scrollView.canScrollVertically(1)
                if (canScrollDown) {
                    button.show()
                    onVisibilityChanged?.invoke(true)
                } else {
                    button.hide()
                    onVisibilityChanged?.invoke(false)
                }
            }

            setupEditText(editText) {
                editText.postDelayed({ scrollCheck() }, 50)
            }

            scrollView.viewTreeObserver.addOnScrollChangedListener {
                scrollCheck()
            }

            button.setOnClickListener {
                scrollView.smoothScrollTo(0, scrollView.getChildAt(0)?.height ?: 0)
            }
        }
    }

    private fun ImageButton.hide() {
        //this.hide()
        this.isVisible = false
    }
    private fun ImageButton.show() {
        //this.show()
        this.isVisible = true
    }