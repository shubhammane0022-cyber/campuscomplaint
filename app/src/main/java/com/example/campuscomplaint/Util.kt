package com.example.campuscomplaint

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.File
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.SimpleDateFormat
import java.util.*

fun View.applySystemBarPadding() {           // keeps your layouts clear of the status bar
    val l = paddingLeft; val t = paddingTop; val r = paddingRight; val b = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val s = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.setPadding(l + s.left, t + s.top, r + s.right, b + s.bottom)
        insets
    }
}

fun Long.toDateString(): String = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(this))
fun Long.toDateTimeString(): String =
    if (this == 0L) "—" else SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(this))

fun String.csv() = "\"" + replace("\"", "\"\"") + "\""

fun statusColor(status: String): Int = Color.parseColor(
    when (status) {
        Status.PENDING -> "#F57C00"
        Status.IN_PROGRESS -> "#1976D2"
        Status.RESOLVED -> "#2E7D32"
        else -> "#C62828"
    }
)

fun Context.notifyStatusChange(title: String, status: String) {
    startService(Intent(this, StatusService::class.java)
        .putExtra(StatusService.EXTRA_TITLE, title).putExtra(StatusService.EXTRA_STATUS, status))
}

fun Context.savePhoto(uri: Uri, prefix: String): String? = try {
    val bmp = contentResolver.openInputStream(uri).use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = 2 })
    }
    val file = File(filesDir, "${prefix}_${System.currentTimeMillis()}.jpg")
    file.outputStream().use { bmp?.compress(Bitmap.CompressFormat.JPEG, 80, it) }
    file.absolutePath
} catch (e: Exception) { Log.e("savePhoto", "failed", e); null }