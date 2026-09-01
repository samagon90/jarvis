package com.jarvis.master

import android.app.Application
import com.jarvis.master.data.RepairRepository
import com.jarvis.master.data.db.AppDatabase

class JarvisApp : Application() {

    lateinit var repository: RepairRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = RepairRepository.getInstance(AppDatabase.getInstance(this))
    }
}
