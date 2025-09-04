package com.example.txtnotesapp.common.utils

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.MenuInflater
import android.view.View
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.ListPopupWindow
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
            width = 500
            setBackgroundDrawable(R.drawable.popup_background)
            verticalOffset = -anchorView.height
            horizontalOffset = 24


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
//        return listOf(
//            ContextMenuItem(
//                id = R.id.note_menu_rename,
//                title = "Переименовать",
//                //iconRes = R.drawable.ic_edit
//            ),
//            ContextMenuItem(
//                id = R.id.note_menu_move,
//                title = "Переместить",
//                //iconRes = R.drawable.ic_move
//            ),
//            ContextMenuItem(
//                id = R.id.note_menu_share,
//                title = "Поделиться",
//                //iconRes = R.drawable.ic_share
//            ),
//            ContextMenuItem(
//                id = R.id.note_menu_delete,
//                title = "Удалить",
//                iconRes = R.drawable.ic_delete
//            ),
//        )
//    }

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
}