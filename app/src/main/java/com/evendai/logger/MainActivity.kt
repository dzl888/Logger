package com.evendai.logger

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.evendai.loglibrary.Timber

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Timber.plantDefaultTree(application)
    }

    fun printLog(view: View) {
        Timber.fe(Exception("新异常"), "开心哈")
    }
}