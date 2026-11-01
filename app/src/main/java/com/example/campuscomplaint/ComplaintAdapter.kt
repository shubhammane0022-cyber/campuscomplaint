package com.example.campuscomplaint

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.campuscomplaint.databinding.ItemComplaintBinding

class ComplaintAdapter(private val isAdmin: Boolean, private val actions: ComplaintActions)
    : RecyclerView.Adapter<ComplaintAdapter.VH>() {

    private var items = listOf<Complaint>()
    fun submit(list: List<Complaint>) { items = list; notifyDataSetChanged() }

    class VH(val b: ItemComplaintBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemComplaintBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val c = items[position]
        with(h.b) {
            tvTitle.text = c.title
            tvStudent.text = "By: ${c.studentName}"
            tvCategoryLocation.text = "${c.category} • ${c.location}"
            tvDescription.text = c.description
            tvDate.text = "${c.createdAt.toDateString()} • Urgency ${c.urgency}/5"
            chipStatus.text = c.status
            chipStatus.chipBackgroundColor = ColorStateList.valueOf(statusColor(c.status))
            chipStatus.setTextColor(Color.WHITE)

            btnView.text = if (isAdmin) "View / Update" else "View"
            val open = c.status == Status.PENDING || c.status == Status.IN_PROGRESS
            btnReject.visibility = if (isAdmin && open) View.VISIBLE else View.GONE
            btnView.setOnClickListener { actions.onView(c) }
            btnReject.setOnClickListener { actions.onReject(c) }

            root.setOnCreateContextMenuListener { menu, _, _ ->        // context menu (long-press)
                menu.setHeaderTitle(c.title)
                menu.add("Share").setOnMenuItemClickListener { actions.onShare(c); true }
                if (isAdmin || c.status == Status.PENDING)
                    menu.add("Delete").setOnMenuItemClickListener { actions.onDelete(c); true }
            }
        }
    }
}