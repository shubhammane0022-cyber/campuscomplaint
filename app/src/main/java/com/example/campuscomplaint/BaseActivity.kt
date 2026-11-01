package com.example.campuscomplaint

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {
    protected val tag: String get() = this::class.java.simpleName
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); Log.d(tag, "onCreate") }
    override fun onStart() { super.onStart(); Log.d(tag, "onStart") }
    override fun onResume() { super.onResume(); Log.d(tag, "onResume") }
    override fun onPause() { Log.d(tag, "onPause"); super.onPause() }
    override fun onStop() { Log.d(tag, "onStop"); super.onStop() }
    override fun onDestroy() { Log.d(tag, "onDestroy"); super.onDestroy() }
}