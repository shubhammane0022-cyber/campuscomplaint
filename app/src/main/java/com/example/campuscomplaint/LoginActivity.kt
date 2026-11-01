package com.example.campuscomplaint

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import com.example.campuscomplaint.databinding.ActivityLoginBinding
import com.example.campuscomplaint.databinding.DialogRegisterBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class LoginActivity : BaseActivity() {
    private lateinit var db: DbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val session = Session(this)
        if (session.user() != null) { goHome(); return }

        val b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.root.applySystemBarPadding()
        db = DbHelper(this)

        b.loginButton.setOnClickListener {
            val email = b.emailEditText.text.toString().trim()
            val pass = b.passwordEditText.text.toString()
            val role = if (b.adminRadioButton.isChecked) Role.ADMIN else Role.STUDENT
            if (email.isEmpty() || pass.isEmpty()) {
                toast("Enter email and password"); return@setOnClickListener
            }
            when (val r = authenticate(email, pass, role)) {
                is AuthResult.Success -> { session.save(r.user); goHome() }
                AuthResult.WrongCredentials -> toast("Invalid credentials for ${role.value}")
                is AuthResult.Error -> toast(r.message)
            }
        }
        b.registerTextView.setOnClickListener { showRegisterDialog() }
    }

    private fun authenticate(email: String, pass: String, role: Role): AuthResult = try {
        db.login(email, pass, role.value)?.let { AuthResult.Success(it) } ?: AuthResult.WrongCredentials
    } catch (e: Exception) {
        Log.e(tag, "login failed", e); AuthResult.Error(e.message ?: "Unknown error")
    }

    private fun showRegisterDialog() {
        val d = DialogRegisterBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(this).setTitle("Student registration").setView(d.root)
            .setPositiveButton("Register") { _, _ ->
                val name = d.etName.text.toString().trim()
                val email = d.etEmail.text.toString().trim()
                val pass = d.etPassword.text.toString()
                when {
                    name.isEmpty() || email.isEmpty() -> toast("Fill all fields")
                    !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> toast("Invalid email")
                    pass.length < 6 -> toast("Password must be at least 6 characters")
                    db.registerStudent(name, email, pass) -> toast("Registered! Please login.")
                    else -> toast("Email already registered")
                }
            }.setNegativeButton("Cancel", null).show()
    }

    private fun goHome() { startActivity(Intent(this, HomeActivity::class.java)); finish() }
    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
}