package com.example.campuscomplaint

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.example.campuscomplaint.databinding.FragmentStatsBinding

class StatsFragment : Fragment(R.layout.fragment_stats) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentStatsBinding.bind(view)
        val user = Session(requireContext()).user()
        val isAdmin = user?.role == Role.ADMIN.value

        val cursor = requireContext().contentResolver.query(
            ComplaintProvider.CONTENT_URI, arrayOf("category"),
            if (isAdmin) null else "student_id=?",
            if (isAdmin) null else arrayOf((user?.id ?: -1L).toString()), null)

        val categories = mutableListOf<String>()
        cursor?.use { while (it.moveToNext()) categories += it.getString(0) }

        val summary = categories.groupingBy { it }.eachCount().entries
            .sortedByDescending { it.value }.map { "${it.key}  —  ${it.value}" }

        b.lvStats.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1,
            summary.ifEmpty { listOf("No complaints yet") })
    }
}