package ru.whiteleaf.notes.domain.model

import android.graphics.drawable.Drawable

data class ContextMenuItem(
    val id: Int,
    val title: String,
    val iconRes: Drawable? = null,
    val isEnabled: Boolean = true,
    val isChecked: Boolean = false,
    val subItems: List<ContextMenuItem>? = null,
    val isExpanded: Boolean = false,
    val isLastInGroup: Boolean = false
)