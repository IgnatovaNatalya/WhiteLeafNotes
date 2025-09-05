package com.example.txtnotesapp.common.utils

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.MenuInflater
import android.view.View
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.content.ContextCompat
import com.example.txtnotesapp.R
import com.example.txtnotesapp.domain.model.ContextMenuItem

object ContextMenuHelper {

    fun showContextMenu(
        context: Context,
        anchorView: View,
        items: List<ContextMenuItem>,
        onItemSelected: (itemId: Int) -> Unit
    ) {
        val listPopupWindow = ListPopupWindow(context).apply {

            setAdapter(ContextMenuAdapter(context, items))

            this.anchorView = anchorView
            setDropDownGravity(Gravity.START)
            width = 140.dpToPx(context)
            setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.popup_background))
            verticalOffset = -anchorView.height
            horizontalOffset = 16.dpToPx(context)


            setOnItemClickListener { parent, view, position, id ->
                val item = items[position]
                if (item.isEnabled) onItemSelected(item.id)
                dismiss()
            }
        }
        listPopupWindow.show()
    }

    // Стандартные пункты меню для заметок
    fun getNoteContextMenuItems(context: Context): List<ContextMenuItem> {
        return listOf(
            createMenuItem(context, R.id.note_menu_rename),
            createMenuItem(context, R.id.note_menu_move),
            createMenuItem(context, R.id.note_menu_share),
            createMenuItem(context, R.id.note_menu_delete)
        )
    }

    @SuppressLint("RestrictedApi")
    private fun createMenuItem(context: Context, menuItemId: Int): ContextMenuItem {
        val menu = MenuBuilder(context)
        MenuInflater(context).inflate(R.menu.context_note_menu, menu)
        val item = menu.findItem(menuItemId)

        return ContextMenuItem(
            id = menuItemId,
            title = item.title.toString(),
            isEnabled = item.isEnabled
        )
    }

    fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

}