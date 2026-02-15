package com.nomixcloner.app

import android.app.Application
import android.content.Context
import android.util.Log

class AppApplication: Application(){
    override fun onCreate() {
        super.onCreate()

        Log.d("test", "tewt")
        test(this)
        print("testtesttest")
    }
}

fun test(context: Context){}