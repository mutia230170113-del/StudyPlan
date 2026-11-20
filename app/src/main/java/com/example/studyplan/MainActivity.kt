package com.example.studyplan

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.studyplan.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        // Jika tidak ada sesi login, kembalikan ke halaman Login
        if (currentUser == null) {
            goToLogin()
            return
        }

        val displayName = currentUser.displayName ?: "Mahasiswa"
        binding.tvWelcome.text = getString(R.string.welcome_user, displayName)
        binding.tvEmail.text = currentUser.email

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            goToLogin()
        }
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}