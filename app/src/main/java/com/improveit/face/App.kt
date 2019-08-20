package com.improveit.face

import android.app.Application


class App : Application() {

    companion object {
        lateinit var instance: App
        var currentPhotoPath: String = ""

    }

    override fun onCreate() {
        super.onCreate()
        instance = this


    }


}

