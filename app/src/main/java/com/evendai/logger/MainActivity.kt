package com.evendai.logger

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.evendai.loglibrary.LogTree
import com.evendai.loglibrary.Timber

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        LogTree.init(application)
        Timber.plant(LogTree)
    }

    fun printLog(view: View) {
        Timber.fe(Exception("完了，出Bug了"), "出了Bug怎么办？")
    }
}