package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val originalText: String,
    val slideCount: Int,
    val language: String,
    val summary: String? = null,
    val grammarAnalysis: String? = null,
    val spellingAnalysis: String? = null,
    val aiPlagiarismScore: Int? = null,
    val aiAnalysis: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
