package ru.whiteleaf.notes.common.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.ImageView
import android.widget.TextView
import ru.whiteleaf.notes.R

class NotebookDropdownAdapter(
    context: Context,
    private val allItems: List<DropdownNotebookItem>
) : ArrayAdapter<DropdownNotebookItem>(context, R.layout.item_dropdown, allItems) {

    private var filteredItems: List<DropdownNotebookItem> = allItems

    override fun getCount(): Int = filteredItems.size

    override fun getItem(position: Int): DropdownNotebookItem = filteredItems[position]

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_dropdown, parent, false)
        val item = getItem(position)
        val icon = view.findViewById<ImageView>(R.id.dropdown_icon)
        val text = view.findViewById<TextView>(R.id.dropdown_text)

        text.text = item.toDisplayString()

        when (item) {
            is DropdownNotebookItem.RootItem -> {
                icon.visibility = View.GONE // или иконка домика
                //text.setTypeface(text.typeface, Typeface.ITALIC)
            }

            is DropdownNotebookItem.NotebookItem -> {
                icon.visibility = View.VISIBLE
                if (item.notebook.isEncrypted) icon.setImageResource(R.drawable.ic_folder_protected)
                else icon.setImageResource(R.drawable.ic_folder)
                //text.setTypeface(text.typeface, Typeface.NORMAL)
            }
        }
        return view
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint?.toString()?.trim()?.lowercase() ?: ""
                val filtered = allItems.filter {
                    when (it) {
                        is DropdownNotebookItem.RootItem -> true // всегда в списке
                        is DropdownNotebookItem.NotebookItem -> {
                            if (query.isEmpty()) true
                            else it.notebook.path.lowercase().contains(query)
                        }
                    }
                }
                return FilterResults().apply {
                    values = filtered
                    count = filtered.size
                }
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredItems = results?.values as? List<DropdownNotebookItem> ?: emptyList()
                notifyDataSetChanged()
            }
        }
    }
}