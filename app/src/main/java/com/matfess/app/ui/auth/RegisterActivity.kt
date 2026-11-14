package com.matfess.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.matfess.app.databinding.ActivityRegisterBinding
import com.matfess.app.ui.common.BaseActivity
import com.matfess.app.ui.main.MainActivity
import com.matfess.app.util.applyVerticalInsets
import com.matfess.app.util.hideKeyboard
import com.matfess.app.util.shake
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RegisterActivity : BaseActivity() {

    private lateinit var binding: ActivityRegisterBinding

    private val majors = listOf(
        "Teknik Informatika", "Sistem Informasi", "Ilmu Komputer",
        "Teknik Elektro", "Manajemen", "Akuntansi", "Psikologi",
        "Ilmu Komunikasi", "Kedokteran", "Hukum", "Pendidikan"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.content.applyVerticalInsets(6, 24)

        binding.etMajor.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, majors)
        )
        binding.etSemester.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                (1..14).map { "Semester $it" }
            )
        )
        binding.btnBack.setOnClickListener { finish() }
        binding.btnLogin.setOnClickListener { finish() }
        binding.btnRegister.setOnClickListener { register() }
        binding.etPasswordConfirm.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                register()
                true
            } else false
        }
        binding.root.setOnApplyWindowInsetsListener { v, insets ->
            v.dispatchApplyWindowInsets(insets)
        }
    }

    private fun register() {
        binding.root.hideKeyboard()
        val name = binding.etName.text?.toString()?.trim().orEmpty()
        val nim = binding.etNim.text?.toString()?.trim().orEmpty()
        val major = binding.etMajor.text?.toString()?.trim().orEmpty()
        val semesterText = binding.etSemester.text?.toString()?.trim().orEmpty()
        val semester = semesterText.filter { it.isDigit() }.toIntOrNull() ?: 0
        val password = binding.etPassword.text?.toString().orEmpty()
        val confirm = binding.etPasswordConfirm.text?.toString().orEmpty()

        when {
            name.length < 3 -> return showError("Nama minimal 3 karakter.")
            nim.isBlank() -> return showError("NIM wajib diisi.")
            major.isBlank() -> return showError("Jurusan wajib dipilih.")
            semester !in 1..14 -> return showError("Pilih semester antara 1–14.")
            password.length < 6 -> return showError("Password minimal 6 karakter.")
            password != confirm -> return showError("Konfirmasi password belum sama.")
            repo.db.findUserByNim(nim) != null -> return showError("NIM sudah terdaftar. Silakan masuk.")
        }

        binding.btnRegister.isEnabled = false
        binding.btnRegister.text = "Membuat akun..."
        lifecycleScope.launch {
            delay(220)
            val id = repo.db.registerUser(name, nim, major, semester, password)
            binding.btnRegister.isEnabled = true
            binding.btnRegister.text = "Buat akun"
            if (id <= 0L) {
                showError("Akun gagal dibuat. Coba NIM lain.")
                return@launch
            }
            prefs.saveSession(id, name, nim, major, semester)
            openMain()
        }
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.isVisible = true
        binding.tvError.shake()
    }

    private fun openMain() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
