package com.example.campuscomplaint

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.campuscomplaint.databinding.ActivityComplaintsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class HomeActivity : BaseActivity(), ComplaintActions {
    private lateinit var b: ActivityComplaintsBinding
    private lateinit var db: DbHelper
    private lateinit var session: Session
    private lateinit var adapter: ComplaintAdapter
    private var user: User? = null
    private var currentList = listOf<Complaint>()
    private val isAdmin get() = user?.role == Role.ADMIN.value
    private val helpdesk = "8007891090"          // put your campus helpdesk number here

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            Log.d(tag, "POST_NOTIFICATIONS granted = $it")
        }

    private val statusReceiver = object : BroadcastReceiver() {         // anonymous object
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(tag, "Broadcast received: ${intent.action}")
            refresh()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityComplaintsBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.root.applySystemBarPadding()
        db = DbHelper(this); session = Session(this)

        user = session.user() ?: run { logout(); return }

        setSupportActionBar(b.toolbar)
        supportActionBar?.title = if (isAdmin) "All Complaints" else "My Complaints"

        b.fabAdd.visibility = if (isAdmin) View.GONE else View.VISIBLE
        b.fabAdd.setOnClickListener { startActivity(Intent(this, AddComplaintActivity::class.java)) }

        adapter = ComplaintAdapter(isAdmin, this)
        b.rvComplaints.layoutManager = LinearLayoutManager(this)
        b.rvComplaints.adapter = adapter

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(this, statusReceiver,
            IntentFilter(StatusService.ACTION_STATUS_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
    }
    override fun onStop() { unregisterReceiver(statusReceiver); super.onStop() }
    override fun onResume() { super.onResume(); refresh() }

    // ----- Options menu -----
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.home_menu, menu)
        menu.findItem(R.id.action_export).isVisible = isAdmin
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_recent -> { startActivity(Intent(this, PreviousComplaintsActivity::class.java)); true }  // explicit intent
        R.id.action_call -> { launch(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$helpdesk"))); true }         // implicit intent
        R.id.action_sms -> {
            launch(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$helpdesk"))
                .putExtra("sms_body", "Hello, I need help with my complaint.")); true
        }
        R.id.action_export -> { exportCsv(); true }
        R.id.action_logout -> { logout(); true }
        else -> super.onOptionsItemSelected(item)
    }

    private fun launch(i: Intent) {
        try { startActivity(i) }
        catch (e: ActivityNotFoundException) { Toast.makeText(this, "No app can handle this", Toast.LENGTH_SHORT).show() }
    }

    private fun logout() {
        session.clear()
        startActivity(Intent(this, LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }

    // ----- Data (coroutines + collections) -----
    private fun refresh() {
        val u = user ?: return
        lifecycleScope.launch {
            val list = withContext(Dispatchers.IO) { db.getComplaints(if (isAdmin) null else u.id) }
            currentList = list
            adapter.submit(list)
            b.tvTotal.text = list.size.toString()
            b.tvPending.text = list.count { it.status == Status.PENDING }.toString()
            b.tvResolved.text = list.count { it.status == Status.RESOLVED }.toString()
        }
    }

    // ----- ComplaintActions -----
    override fun onView(c: Complaint) {
        startActivity(Intent(this, HostActivity::class.java).putExtra(HostActivity.EXTRA_ID, c.id))
    }

    override fun onReject(c: Complaint) {                        // alert dialog
        MaterialAlertDialogBuilder(this).setTitle("Reject this complaint?").setMessage(c.title)
            .setPositiveButton("Reject") { _, _ ->
                db.updateStatus(c.id, Status.REJECTED)
                notifyStatusChange(c.title, Status.REJECTED)
            }.setNegativeButton("Cancel", null).show()
    }

    override fun onDelete(c: Complaint) {
        MaterialAlertDialogBuilder(this).setTitle("Delete complaint?").setMessage(c.title)
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch { withContext(Dispatchers.IO) { db.delete(c.id) }; refresh() }
            }.setNegativeButton("Cancel", null).show()
    }

    override fun onShare(c: Complaint) {
        val text = "Complaint: ${c.title}\nCategory: ${c.category}\nLocation: ${c.location}\nStatus: ${c.status}"
        launch(Intent.createChooser(
            Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Share via"))
    }

    // ----- External storage -----
    private fun exportCsv() {
        try {
            val dir = getExternalFilesDir(null) ?: filesDir
            val file = File(dir, "complaints_${System.currentTimeMillis()}.csv")
            file.printWriter().use { out ->
                out.println("id,student,category,title,location,status,urgency,logged")
                currentList.forEach {
                    out.println("${it.id},${it.studentName.csv()},${it.category.csv()},${it.title.csv()}," +
                            "${it.location.csv()},${it.status},${it.urgency},${it.createdAt.toDateString()}")
                }
            }
            Toast.makeText(this, "Saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            Log.e(tag, "export failed", e)
            Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show()
        }
    }
}