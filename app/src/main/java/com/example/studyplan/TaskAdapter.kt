package com.example.studyplan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.studyplan.databinding.ItemTaskBinding

class TaskAdapter(
    private var taskList: List<Task>,
    private val onItemClick: (Task) -> Unit,
    private val onEditClick: (Task) -> Unit,
    private val onDeleteClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]
        holder.binding.apply {
            tvTaskTitle.text = task.title
            tvTaskCourse.text = "Mata Kuliah: ${task.course}"
            tvTaskDeadline.text = "Deadline: ${task.deadline}"
            tvTaskPriority.text = "Prioritas: ${task.priority}"
            tvTaskStatus.text = "Status: ${task.status}"

            root.setOnClickListener { onItemClick(task) }
            btnEdit.setOnClickListener { onEditClick(task) }
            btnDelete.setOnClickListener { onDeleteClick(task) }
        }
    }

    override fun getItemCount(): Int = taskList.size

    fun updateData(newList: List<Task>) {
        taskList = newList
        notifyDataSetChanged()
    }
}