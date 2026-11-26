package com.example.studyplan

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.studyplan.databinding.ActivityAddEditTaskBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Locale

class AddEditTaskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditTaskBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var existingTask: Task? = null

    private val priorities = arrayOf("Rendah", "Sedang", "Tinggi")
    private val statuses = arrayOf("Belum Dikerjakan", "Sedang Dikerjakan", "Selesai")
    private val reminders = mutableListOf(
        "Tanpa Pengingat",
        "1 Hari Sebelum (08:00)",
        "Hari H (08:00)",
        "Tes 10 Detik (Demo)",
        "Atur Waktu Sendiri..."
    )
    private lateinit var reminderAdapter: ArrayAdapter<String>
    private var isUserInteracting = false

    override fun onUserInteraction() {
        super.onUserInteraction()
        isUserInteracting = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        setupSpinners()
        setupDatePicker()

        @Suppress("DEPRECATION")
        existingTask = intent.getSerializableExtra("EXTRA_TASK") as? Task

        if (existingTask != null) {
            binding.tvPageTitle.text = getString(R.string.title_edit_task)
            binding.etTitle.setText(existingTask?.title)
            binding.etCourse.setText(existingTask?.course)
            binding.etDescription.setText(existingTask?.description)
            binding.etDeadline.setText(existingTask?.deadline)

            val priorityIndex = priorities.indexOf(existingTask?.priority)
            if (priorityIndex >= 0) binding.spinnerPriority.setSelection(priorityIndex)

            val statusIndex = statuses.indexOf(existingTask?.status)
            if (statusIndex >= 0) binding.spinnerStatus.setSelection(statusIndex)

            val savedReminder = existingTask?.reminder ?: "Tanpa Pengingat"
            if (savedReminder.startsWith("Custom: ")) {
                reminders[4] = savedReminder
                reminderAdapter.notifyDataSetChanged()
                binding.spinnerReminder.setSelection(4)
            } else {
                val reminderIndex = reminders.indexOf(savedReminder)
                if (reminderIndex >= 0) binding.spinnerReminder.setSelection(reminderIndex)
            }
        }

        binding.btnSaveTask.setOnClickListener {
            saveTask()
        }
    }

    private fun setupSpinners() {
        binding.spinnerPriority.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, priorities)
        binding.spinnerStatus.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, statuses)

        reminderAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, reminders)
        binding.spinnerReminder.adapter = reminderAdapter

        binding.spinnerReminder.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isUserInteracting) return
                if (position == 4) {
                    showCustomReminderPicker()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun showCustomReminderPicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                TimePickerDialog(
                    this,
                    { _, hour, minute ->
                        val formatted = String.format(
                            Locale.getDefault(),
                            "Custom: %02d-%02d-%04d %02d:%02d",
                            day, month + 1, year, hour, minute
                        )
                        reminders[4] = formatted
                        reminderAdapter.notifyDataSetChanged()
                        binding.spinnerReminder.setSelection(4)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun setupDatePicker() {
        binding.etDeadline.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = String.format(
                    Locale.getDefault(),
                    "%02d-%02d-%04d",
                    selectedDay,
                    selectedMonth + 1,
                    selectedYear
                )
                binding.etDeadline.setText(formattedDate)
            }, year, month, day).show()
        }
    }

    private fun saveTask() {
        val title = binding.etTitle.text.toString().trim()
        val course = binding.etCourse.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val deadline = binding.etDeadline.text.toString().trim()
        val priority = binding.spinnerPriority.selectedItem.toString()
        val status = binding.spinnerStatus.selectedItem.toString()
        val selectedReminder = binding.spinnerReminder.selectedItem.toString()
        val reminderOption = if (selectedReminder == "Atur Waktu Sendiri...") "Tanpa Pengingat" else selectedReminder

        binding.tilTitle.error = null
        binding.tilCourse.error = null
        binding.tilDeadline.error = null

        if (title.isEmpty()) {
            binding.tilTitle.error = "Judul tugas tidak boleh kosong"
            return
        }
        if (course.isEmpty()) {
            binding.tilCourse.error = "Mata kuliah tidak boleh kosong"
            return
        }
        if (deadline.isEmpty()) {
            binding.tilDeadline.error = "Deadline harus dipilih"
            return
        }

        val userId = auth.currentUser?.uid ?: return
        setLoading(true)

        if (existingTask == null) {
            val docRef = db.collection("tasks").document()
            val newTask = Task(
                id = docRef.id,
                userId = userId,
                title = title,
                course = course,
                description = description,
                deadline = deadline,
                priority = priority,
                status = status,
                reminder = reminderOption
            )
            docRef.set(newTask)
                .addOnSuccessListener {
                    setLoading(false)
                    NotificationHelper.scheduleReminder(this, newTask, reminderOption)
                    Toast.makeText(this, "Tugas berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    setLoading(false)
                    Toast.makeText(this, "Gagal menyimpan: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
        } else {
            val updatedTask = Task(
                id = existingTask!!.id,
                userId = userId,
                title = title,
                course = course,
                description = description,
                deadline = deadline,
                priority = priority,
                status = status,
                reminder = reminderOption
            )
            db.collection("tasks").document(existingTask!!.id).set(updatedTask)
                .addOnSuccessListener {
                    setLoading(false)
                    NotificationHelper.scheduleReminder(this, updatedTask, reminderOption)
                    Toast.makeText(this, "Tugas berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    setLoading(false)
                    Toast.makeText(this, "Gagal memperbarui: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSaveTask.isEnabled = !isLoading
    }
}