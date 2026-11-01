package com.example.campuscomplaint

import android.os.Bundle
import com.example.campuscomplaint.databinding.ActivityHostBinding

class HostActivity : BaseActivity() {
    companion object { const val EXTRA_ID = "complaint_id" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val b = ActivityHostBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.root.applySystemBarPadding()
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, ComplaintDetailFragment.newInstance(intent.getLongExtra(EXTRA_ID, -1)))
                .commit()
        }
    }
}