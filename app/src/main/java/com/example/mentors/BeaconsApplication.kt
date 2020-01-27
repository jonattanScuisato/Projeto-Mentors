package com.example.mentors

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class BeaconsApplication : Application() {

    companion object{
        const val CHANNEL_ID = "channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    fun createNotificationChannel(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel(CHANNEL_ID,"channel",NotificationManager.IMPORTANCE_HIGH)
            channel.description = "Bem Vindo!"
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

    }
}