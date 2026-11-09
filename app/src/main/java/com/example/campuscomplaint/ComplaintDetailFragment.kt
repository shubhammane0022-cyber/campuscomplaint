package com.example.campuscomplaint

import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.campuscomplaint.databinding.FragmentDetailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class ComplaintDetailFragment : Fragment(R.layout.fragment_detail) {
    companion object {
        fun newInstance(id: Long) = ComplaintDetailFragment().apply {
            arguments = Bundle().apply { putLong("id", id) }
        }
    }
    private var _b: FragmentDetailBinding? = null
    private val b get() = _b!!
    private lateinit var db: DbHelper

    // Admin picks the "after fix" photo
    private val pickFixed = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null || _b == null) return@registerForActivityResult
        val c = db.getById(requireArguments().getLong("id")) ?: return@registerForActivityResult
        val path = requireContext().savePhoto(uri, "fixed")
        if (path == null) {
            Toast.makeText(requireContext(), "Could not save photo", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        db.saveFixedPhoto(c.id, path)
        show()
        if (c.status != Status.RESOLVED) askMarkResolved(c)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentDetailBinding.bind(view)
        db = DbHelper(requireContext())
        Log.d("DetailFragment", "onViewCreated")
        b.btnCategoryStats.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, StatsFragment())
                .addToBackStack("stats")
                .commit()
        }
    }

    override fun onResume() { super.onResume(); show() }
    override fun onDestroyView() { super.onDestroyView(); _b = null }

    private fun show() {
        val c = db.getById(requireArguments().getLong("id")) ?: run { requireActivity().finish(); return }
        val isAdmin = Session(requireContext()).user()?.role == Role.ADMIN.value

        b.tvDetailTitle.text = c.title
        b.tvDetailDesc.text = c.description
        b.detailTable.removeAllViews()
        listOf("Student" to if (isAdmin) "Anonymous" else c.studentName, "Category" to c.category, "Location" to c.location,
            "Status" to c.status, "Urgency" to "${c.urgency} / 5",
            "Incident" to c.incidentTime.toDateTimeString(), "Logged" to c.createdAt.toDateString())
            .forEach { (k, v) -> b.detailTable.addView(row(k, v)) }

        // Problem photo (from the student)
        c.photoPath?.let { p ->
            val f = File(p)
            if (f.exists()) { b.ivDetailPhoto.setImageURI(Uri.fromFile(f)); b.ivDetailPhoto.visibility = View.VISIBLE }
        }

        // Fixed photo (from the admin), visible to student and admin
        val fixed = c.fixedPhotoPath?.let { File(it) }?.takeIf { it.exists() }
        b.tvFixedLabel.visibility = if (fixed != null) View.VISIBLE else View.GONE
        b.ivFixedPhoto.visibility = if (fixed != null) View.VISIBLE else View.GONE
        fixed?.let { b.ivFixedPhoto.setImageURI(Uri.fromFile(it)) }

        b.btnUploadFixed.visibility = if (isAdmin) View.VISIBLE else View.GONE
        b.btnUploadFixed.text = if (fixed != null) "Replace fixed photo" else "Upload fixed photo"
        b.btnUploadFixed.setOnClickListener {
            pickFixed.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        b.btnUpdateStatus.visibility = if (isAdmin) View.VISIBLE else View.GONE
        b.btnUpdateStatus.setOnClickListener { pickStatus(c) }

        // Rating (student only, once resolved)
        val resolved = c.status == Status.RESOLVED
        val canRate = resolved && !isAdmin
        b.tvRateLabel.visibility = if (resolved) View.VISIBLE else View.GONE
        b.ratingBar.visibility = if (resolved) View.VISIBLE else View.GONE
        b.ratingBar.setOnRatingBarChangeListener(null)
        b.ratingBar.rating = c.rating.toFloat()
        b.ratingBar.setIsIndicator(!canRate)
        b.ratingBar.setOnRatingBarChangeListener { _, r, fromUser ->
            if (fromUser && canRate) {
                db.saveRating(c.id, r.toInt())
                Toast.makeText(requireContext(), "Thanks for rating!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun askMarkResolved(c: Complaint) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Photo saved")
            .setMessage("Mark this complaint as Resolved?")
            .setPositiveButton("Mark Resolved") { _, _ ->
                db.updateStatus(c.id, Status.RESOLVED)
                requireContext().notifyStatusChange(c.title, Status.RESOLVED)
                show()
            }
            .setNegativeButton("Not yet", null)
            .show()
    }

    private fun row(label: String, value: String) = TableRow(requireContext()).apply {
        addView(TextView(context).apply { text = label; setTypeface(null, Typeface.BOLD); setPadding(0, 8, 24, 8) })
        addView(TextView(context).apply { text = value; setPadding(0, 8, 0, 8) })
    }

    private fun pickStatus(c: Complaint) {
        val options = arrayOf(Status.PENDING, Status.IN_PROGRESS, Status.RESOLVED, Status.REJECTED)
        MaterialAlertDialogBuilder(requireContext()).setTitle("Update status")
            .setSingleChoiceItems(options, options.indexOf(c.status).coerceAtLeast(0)) { d, which ->
                d.dismiss()
                if (!db.updateStatus(c.id, options[which])) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Fixed photo required")
                        .setMessage("Upload a photo of the fixed problem before marking this complaint as Resolved.")
                        .setPositiveButton("Upload photo") { _, _ ->
                            pickFixed.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                    return@setSingleChoiceItems
                }
                requireContext().notifyStatusChange(c.title, options[which])
                show()
            }.show()
    }
}