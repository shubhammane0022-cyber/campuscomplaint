package com.example.campuscomplaint

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.example.campuscomplaint.databinding.ActivityPreviousComplaintsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PreviousComplaintsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val b = ActivityPreviousComplaintsBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.root.applySystemBarPadding()
        val user = Session(this).user() ?: run { finish(); return }

        lifecycleScope.launch {
            val list = withContext(Dispatchers.IO) {
                DbHelper(this@PreviousComplaintsActivity)
                    .getComplaints(if (user.role == Role.ADMIN.value) null else user.id)
            }.take(3)
            fill(list.getOrNull(0), b.complaintOneTitleTextView, b.complaintOneCategoryTextView,
                b.complaintOneDateTextView, b.complaintOneStatusTextView)
            fill(list.getOrNull(1), b.complaintTwoTitleTextView, b.complaintTwoCategoryTextView,
                b.complaintTwoDateTextView, b.complaintTwoStatusTextView)
            fill(list.getOrNull(2), b.complaintThreeTitleTextView, b.complaintThreeCategoryTextView,
                b.complaintThreeDateTextView, b.complaintThreeStatusTextView)
        }
    }

    private fun fill(c: Complaint?, title: TextView, cat: TextView, date: TextView, status: TextView) {
        if (c == null) { (title.parent as View).visibility = View.GONE; return }
        title.text = c.title
        cat.text = "Category: ${c.category}"
        date.text = "Date: ${c.createdAt.toDateString()}"
        status.text = "Status: ${c.status}"
    }
}