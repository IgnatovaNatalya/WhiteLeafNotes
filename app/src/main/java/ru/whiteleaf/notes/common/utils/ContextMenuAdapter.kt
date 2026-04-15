package ru.whiteleaf.notes.common.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import ru.whiteleaf.notes.R
import ru.whiteleaf.notes.domain.model.ContextMenuItem

class ContextMenuAdapter(
    private val context: Context,
    private val items: MutableList<ContextMenuItem>
) : BaseAdapter() {

    private val expandedItems = mutableSetOf<Int>()
    private var displayList = buildDisplayList()

    private fun buildDisplayList(): List<ContextMenuItem> {
        val result = mutableListOf<ContextMenuItem>()
        for (item in items) {
            result.add(item)
            // Если пункт раскрыт и имеет подпункты - добавляем их
            if (expandedItems.contains(item.id) && item.subItems != null) {
                result.addAll(item.subItems)
            }
        }
        return result
    }

    fun toggleExpand(item: ContextMenuItem) {
        if (item.subItems == null) return

        if (expandedItems.contains(item.id)) {
            expandedItems.remove(item.id)
        } else {
            expandedItems.add(item.id)
        }

        displayList = buildDisplayList()
        notifyDataSetChanged()
    }

    override fun getCount(): Int = displayList.size//items.size
    override fun getItem(position: Int): ContextMenuItem = displayList[position]//items[position]
    override fun getItemId(position: Int): Long = getItem(position).id.toLong()//position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_context_menu, parent, false)

        val item = getItem(position)

        val itemTitleTextView = view.findViewById<TextView>(R.id.item_title)
        val itemIconImageView = view.findViewById<ImageView>(R.id.item_icon)
        val itemGroupDivider = view.findViewById<View>(R.id.options_menu_divider)
        val itemIsCheckedImage = view.findViewById<ImageView>(R.id.item_checked)

        // Установка заголовка
        itemTitleTextView.text = item.title

        // Установка иконки (если есть)
        if (item.iconRes != null) {
            itemIconImageView.setImageDrawable(item.iconRes)
            itemIconImageView.visibility = View.VISIBLE
        } else {
            itemIconImageView.visibility = View.GONE
        }

        itemIsCheckedImage.visibility = View.GONE

        // Настройка состояния (включено/выключено)
        itemTitleTextView.isEnabled = item.isEnabled
        itemIconImageView.isEnabled = item.isEnabled
        view.isEnabled = item.isEnabled

        itemGroupDivider.visibility = View.GONE

        // Разделители
        if (item.isLastInGroup)
            if (!item.isExpanded)
                itemGroupDivider.visibility = View.VISIBLE
            else
                itemGroupDivider.visibility = View.GONE

        val isSubItem = items.any { it.subItems?.contains(item) == true }

        if (isSubItem) {
            //подпункты со сдвигом и отмечаем выбранный
            itemTitleTextView.text = "      " + item.title
            itemIsCheckedImage.isVisible = item.isChecked
        } else {
            // Для пункта "Вид" показываем стрелку
            if (item.subItems != null) {
                itemIconImageView.visibility = View.VISIBLE

                if (expandedItems.contains(item.id)) {
                    itemIconImageView.setImageResource(R.drawable.ic_options_group_expanded)
                    itemGroupDivider.visibility = View.GONE
                } else itemIconImageView.setImageResource(R.drawable.ic_options_group_expand)
            }
        }

        return view
    }
}