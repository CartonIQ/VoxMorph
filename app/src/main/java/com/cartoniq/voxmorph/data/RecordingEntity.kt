package com.cartoniq.voxmorph.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Metadata only. Audio lives in filesDir/recordings/<fileName>. */
@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val voiceId: String,
    val voiceDisplayName: String,
    val category: String,
    val sourceType: String,
    val fileName: String,
    val durationMs: Long,
    val createdAtEpochMs: Long,
    val sampleRate: Int,
    val channels: Int,
    val format: String,
    val fileSizeBytes: Long
)
