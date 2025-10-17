package ru.whiteleaf.notes.common.classes

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.widget.NestedScrollView

class CustomNestedScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : NestedScrollView(context, attrs, defStyleAttr) {

    override fun requestChildFocus(child: View?, focused: View?) {

        //чтобы при фокусе на дочернем элементе скролл не перескакивал
        if (focused == null)  super.requestChildFocus(child, focused)
    }
}