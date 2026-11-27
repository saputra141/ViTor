package com.saputra.vitor.gebang.engine

import android.net.Uri
import com.saputra.vitor.gebang.model.GebangProject
import kotlinx.coroutines.flow.Flow

/**
 * Contract for the native Android editing engine.
 *
 * Implementations must be backed by Android-compatible media components.
 * Do not route calls into the Windows Electron binary or its native modules.
 */
interface EditorEngine {
    val state: Flow<EngineState>

    suspend fun openProject(uri: Uri): Result<GebangProject>
    suspend fun saveProject(project: GebangProject, destination: Uri): Result<Unit>
    suspend fun createProject(name: String, width: Int, height: Int, frameRate: Double): Result<GebangProject>
    suspend fun apply(command: EditorCommand): Result<Unit>
    suspend fun export(request: ExportRequest): Flow<ExportProgress>
    suspend fun cancelExport()
}

sealed interface EditorCommand {
    data class AddMedia(val trackId: String, val mediaUri: Uri, val atUs: Long) : EditorCommand
    data class MoveClip(val clipId: String, val toUs: Long) : EditorCommand
    data class TrimClip(val clipId: String, val sourceInUs: Long, val sourceOutUs: Long) : EditorCommand
    data class SetClipEnabled(val clipId: String, val enabled: Boolean) : EditorCommand
    data class RemoveClip(val clipId: String) : EditorCommand
}

data class ExportRequest(
    val project: GebangProject,
    val destination: Uri,
    val videoMimeType: String = "video/mp4",
    val videoBitrate: Int? = null
)

data class ExportProgress(
    val fraction: Float,
    val message: String,
    val isComplete: Boolean = false
)

data class EngineState(
    val isProjectOpen: Boolean = false,
    val isExporting: Boolean = false,
    val lastError: String? = null
)
