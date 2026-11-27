package com.saputra.vitor

import android.app.Application
import com.zaitxcode.opencut.core.store.ProjectStore

/** Owns the local project store used by the integrated editor. */
class ViTorApplication : Application() {
    lateinit var projectStore: ProjectStore
        private set

    override fun onCreate() {
        super.onCreate()
        projectStore = ProjectStore(this)
    }
}
