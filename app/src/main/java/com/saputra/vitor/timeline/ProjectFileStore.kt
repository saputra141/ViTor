package com.saputra.vitor.timeline

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.io.IOException
import java.util.UUID

/** File-backed project storage. Writes are staged and renamed so an interrupted save won't corrupt the last project. */
class ProjectFileStore(
    private val directory: File,
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
) {
    data class ProjectDocument(
        val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
        val projectId: String,
        val savedAtEpochMs: Long,
        val project: TimelineProject
    )

    data class ProjectSummary(
        val projectId: String,
        val name: String,
        val savedAtEpochMs: Long,
        val file: File
    )

    fun save(project: TimelineProject, projectId: String = UUID.randomUUID().toString()): ProjectSummary {
        validateProject(project)
        require(projectId.matches(Regex("[A-Za-z0-9_-]{1,80}"))) { "Invalid project ID" }
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Cannot create project directory: $directory")
        }
        val destination = projectFile(projectId)
        val temporary = File(directory, ".$projectId.tmp")
        try {
            val document = ProjectDocument(
                projectId = projectId,
                savedAtEpochMs = System.currentTimeMillis(),
                project = project
            )
            temporary.writeText(gson.toJson(document), Charsets.UTF_8)
            if (!temporary.renameTo(destination)) {
                if (destination.exists() && !destination.delete()) {
                    throw IOException("Cannot replace previous project")
                }
                if (!temporary.renameTo(destination)) {
                    throw IOException("Cannot commit project file")
                }
            }
        } finally {
            if (temporary.exists()) temporary.delete()
        }
        return ProjectSummary(projectId, project.name, destination.lastModified(), destination)
    }

    fun load(projectId: String): TimelineProject {
        val file = projectFile(projectId)
        if (!file.isFile) throw IOException("Project not found: $projectId")
        val document = try {
            gson.fromJson(file.readText(Charsets.UTF_8), ProjectDocument::class.java)
        } catch (error: Exception) {
            throw IOException("Project file is invalid: $projectId", error)
        } ?: throw IOException("Project file is empty: $projectId")
        if (document.schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw IOException("Unsupported project schema: ${document.schemaVersion}")
        }
        if (document.projectId != projectId) throw IOException("Project ID does not match its file")
        validateProject(document.project)
        return document.project
    }

    fun list(): List<ProjectSummary> {
        if (!directory.isDirectory) return emptyList()
        return directory.listFiles { file ->
            file.isFile && file.name.endsWith(FILE_SUFFIX) && file.name.removeSuffix(FILE_SUFFIX)
                .matches(Regex("[A-Za-z0-9_-]{1,80}"))
        }.orEmpty().mapNotNull { file ->
            runCatching {
                val id = file.name.removeSuffix(FILE_SUFFIX)
                val document = gson.fromJson(file.readText(Charsets.UTF_8), ProjectDocument::class.java)
                if (document.schemaVersion != CURRENT_SCHEMA_VERSION || document.projectId != id) return@runCatching null
                validateProject(document.project)
                ProjectSummary(id, document.project.name, document.savedAtEpochMs, file)
            }.getOrNull()
        }.sortedByDescending { it.savedAtEpochMs }
    }

    fun delete(projectId: String): Boolean {
        val file = projectFile(projectId)
        return !file.exists() || file.delete()
    }

    private fun projectFile(projectId: String): File {
        require(projectId.matches(Regex("[A-Za-z0-9_-]{1,80}"))) { "Invalid project ID" }
        return File(directory, "$projectId$FILE_SUFFIX")
    }

    private fun validateProject(project: TimelineProject) {
        require(project.name.isNotBlank())
        require(project.videoClips.map { it.id }.distinct().size == project.videoClips.size)
        require(project.audioLayers.map { it.id }.distinct().size == project.audioLayers.size)
        require(project.textLayers.map { it.id }.distinct().size == project.textLayers.size)
        require(project.subtitleCues.map { it.id }.distinct().size == project.subtitleCues.size)
        project.videoClips.forEach {
            require(it.trimStartMs >= 0L && it.trimEndMs > it.trimStartMs && it.trimEndMs <= it.sourceDurationMs)
            require(it.volume in 0f..1f)
        }
        project.audioLayers.forEach {
            require(it.startMs >= 0L && it.sourceStartMs >= 0L && it.sourceEndMs > it.sourceStartMs)
            require(it.volume in 0f..1f)
        }
        project.textLayers.forEach { require(it.startMs >= 0L && it.endMs > it.startMs && it.text.isNotBlank()) }
        project.subtitleCues.forEach { require(it.startMs >= 0L && it.endMs > it.startMs && it.text.isNotBlank()) }
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
        private const val FILE_SUFFIX = ".vitor.json"
    }
}
