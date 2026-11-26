package com.example.studyplan

import java.io.Serializable

data class Task(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val course: String = "",
    val description: String = "",
    val deadline: String = "",
    val priority: String = "Sedang",
    val status: String = "Belum Dikerjakan",
    val reminder: String = "Tanpa Pengingat"
) : Serializable