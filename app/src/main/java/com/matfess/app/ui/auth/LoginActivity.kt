package com.matfess.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.matfess.app.databinding.ActivityLoginBinding
import com.matfess.app.ui.common.BaseActivity
import com.matfess.app.ui.main.MainActivity
import com.matfess.app.util.applyVerticalInsets
import com.matfess.app.util.hideKeyboard
import com.matfess.app.util.shake
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoginActivity : BaseActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.content.applyVerticalInsets(8, 24)

        binding.btnLogin.setOnClickListener { attemptLogin() }
        binding.btnDemo.setOnClickListener { demoLogin() }
        binding.btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        binding.etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin()
                true
            } else false
        }
        binding.etNim.setOnFocusChangeListener { _, _ -> binding.tvError.isVisible = false }
        binding.etPassword.setOnFocusChangeListener { _, _ -> binding.tvError.isVisible = false }
    }

    private fun attemptLogin() {
        binding.root.hideKeyboard()
        val nim = binding.etNim.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        if (nim.isBlank() || password.isBlank()) {
            showError("NIM dan password wajib diisi.")
            return
        }
        binding.btnLogin.isEnabled = false
        binding.btnLogin.text = "Memeriksa..."
        lifecycleScope.launch {
            delay(240)
            val user = repo.db.findUser(nim, password)
            binding.btnLogin.isEnabled = true
            binding.btnLogin.text = "Masuk"
            if (user == null) {
                showError("NIM atau password belum cocok. Coba lagi.")
            } else {
                prefs.saveSession(user.id, user.name, user.nim, user.major, user.semester)
                openMain()
            }
        }
    }

    private fun demoLogin() {
        binding.root.hideKeyboard()
        binding.btnDemo.isEnabled = false
        lifecycleScope.launch {
            delay(180)
            val demoNim = "2024001234"
            var user = repo.db.findUserByNim(demoNim)
            if (user == null) {
                val id = repo.db.registerUser(
                    name = "Alya Ramadhani",
                    nim = demoNim,
                    major = "Teknik Informatika",
                    semester = 4,
                    password = "matfess"
                )
                repo.seedDemoData()
                user = repo.db.getUser(id)
            } else if (repo.db.getCourses().isEmpty()) {
                repo.seedDemoData()
            }
            binding.btnDemo.isEnabled = true
            if (user == null) {
                showError("Gagal menyiapkan akun demo. Coba lagi.")
                return@launch
            }
            prefs.saveSession(user.id, user.name, user.nim, user.major, user.semester)
            openMain()
        }
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.isVisible = true
        binding.tvError.shake()
    }

    private fun openMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
