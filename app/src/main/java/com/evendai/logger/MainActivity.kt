package com.evendai.logger

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.evendai.loglibrary.Timber

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Timber.init(application)
    }

    fun printLog(view: View) {
        Timber.fe(Exception("哎，又想看直播了"), "为什么会这样呢？")
    }
}