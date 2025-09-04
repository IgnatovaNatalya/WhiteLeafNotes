package com.example.txtnotesapp.common.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.txtnotesapp.R
import com.example.txtnotesapp.domain.model.ContextMenuItem

class ContextMenuAdapter(
    private val context: Context,
    private val items: List<ContextMenuItem>
) : BaseAdapter() {

    override fun getCount(): Int = items.size
    override fun getItem(position: Int): ContextMenuItem = items[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_context_menu, parent, false)

        val item = getItem(position)

        val itemTitleTextView = view.findViewById<TextView>(R.id.context_item_title)
        val itemIconImageView = view.findViewById<ImageView>(R.id.context_item_icon)

        // Установка заголовка
        itemTitleTextView.text = item.title

        // Установка иконки (если есть)
        if (item.iconRes != null) {
            itemIconImageView.setImageResource(item.iconRes)
            itemIconImageView.visibility = View.VISIBLE
        } else {
            itemIconImageView.visibility = View.GONE
        }

        // Настройка состояния (включено/выключено)
        itemTitleTextView.isEnabled = item.isEnabled
        itemIconImageView.isEnabled = item.isEnabled
        view.isEnabled = item.isEnabled

        return view
    }
}