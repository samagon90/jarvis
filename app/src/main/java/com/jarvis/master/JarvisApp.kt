package com.jarvis.master

import android.app.Application
import com.jarvis.master.data.RepairRepository

class JarvisApp : Application() {

    lateinit var repository: RepairRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = RepairRepository.getInstance(this)
    }
}
