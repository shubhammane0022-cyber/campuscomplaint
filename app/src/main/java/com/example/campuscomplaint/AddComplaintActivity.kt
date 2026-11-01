package com.example.campuscomplaint

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.example.campuscomplaint.databinding.ActivityAddComplaintBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

class AddComplaintActivity : BaseActivity() {
    private lateinit var b: ActivityAddComplaintBinding
    private lateinit var db: DbHelper
    private var photoPath: String? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            photoPath = copyToAppStorage(uri)                    // internal storage
            photoPath?.let { b.complaintImageView.setImageURI(Uri.fromFile(File(it))) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityAddComplaintBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.root.applySystemBarPadding()
        db = DbHelper(this)
        val user = Session(this).user() ?: run { finish(); return }

        b.addPhotoButton.setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        b.submitComplaintButton.setOnClickListener {
            val category = b.categoryEditText.text.toString().trim()
            val title = b.titleEditText.text.toString().trim()
            val desc = b.descriptionEditText.text.toString().trim()
            val loc = b.locationEditText.text.toString().trim()
            if (category.isEmpty() || title.isEmpty() || desc.isEmpty() || loc.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            askUrgency { urgency ->
                askIncidentTime { time ->
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            db.addComplaint(user.id, category, title, desc, loc, photoPath, urgency, time)
                        }
                        Toast.makeText(this@AddComplaintActivity, "Complaint submitted", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }
    }

    private fun askUrgency(onDone: (Int) -> Unit) {              // SeekBar dialog
        val label = TextView(this).apply { text = "Urgency: 3 / 5"; setPadding(48, 32, 48, 0) }
        val seek = SeekBar(this).apply { max = 4; progress = 2; setPadding(48, 32, 48, 32) }
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) { label.text = "Urgency: ${p + 1} / 5" }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; addView(label); addView(seek) }
        MaterialAlertDialogBuilder(this).setTitle("How urgent is it?").setView(box)
            .setPositiveButton("Next") { _, _ -> onDone(seek.progress + 1) }
            .setNegativeButton("Cancel", null).show()
    }

    private fun askIncidentTime(onDone: (Long) -> Unit) {        // DatePicker + TimePicker dialogs
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            TimePickerDialog(this, { _, h, min ->
                cal.set(y, m, d, h, min); onDone(cal.timeInMillis)
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun copyToAppStorage(uri: Uri): String? = try {
        val bmp = contentResolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = 2 })
        }
        val file = File(filesDir, "complaint_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { bmp?.compress(Bitmap.CompressFormat.JPEG, 80, it) }
        file.absolutePath
    } catch (e: Exception) { Log.e(tag, "photo copy failed", e); null }
}