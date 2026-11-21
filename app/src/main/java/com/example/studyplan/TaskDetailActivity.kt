package com.example.studyplan

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.studyplan.databinding.ActivityTaskDetailBinding
import com.google.firebase.firestore.FirebaseFirestore

class TaskDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskDetailBinding
    private lateinit var db: FirebaseFirestore
    private var currentTask: Task? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()

        @Suppress("DEPRECATION")
        currentTask = intent.getSerializableExtra("EXTRA_TASK") as? Task

        if (currentTask == null) {
            Toast.makeText(this, "Data tugas tidak ditemukan", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        displayTask(currentTask!!)

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnDetailEdit.setOnClickListener {
            val intent = Intent(this, AddEditTaskActivity::class.java)
            intent.putExtra("EXTRA_TASK", currentTask)
            startActivity(intent)
            finish()
        }

        binding.btnDetailDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Hapus Tugas")
                .setMessage("Yakin ingin menghapus tugas \"${currentTask!!.title}\"?")
                .setPositiveButton("Hapus") { _, _ ->
                    db.collection("tasks").document(currentTask!!.id).delete()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Tugas berhasil dihapus", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Gagal menghapus: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    private fun displayTask(task: Task) {
        binding.tvDetailTitle.text = task.title
        binding.tvDetailCourse.text = "Mata Kuliah: ${task.course}"
        binding.tvDetailDeadline.text = "Deadline: ${task.deadline}"
        binding.tvDetailPriority.text = "Prioritas: ${task.priority}"
        binding.tvDetailStatus.text = "Status: ${task.status}"
        binding.tvDetailDescription.text =
            if (task.description.isNotEmpty()) task.description else "Tidak ada deskripsi."
    }
}