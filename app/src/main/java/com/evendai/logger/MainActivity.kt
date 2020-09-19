package com.evendai.logger

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.evendai.loglibrary.Timber

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Timber.init(application, BuildConfig.DEBUG) // 建议在Application中进行调用
    }

    fun printLog(view: View) {
        if (Timber.getLogSwitch() || BuildConfig.DEBUG) { // 获取当前日志开关状态
            Timber.i("当前Log开关是开的，可以输出所有级别日志")
        } else {
            Timber.e("当前log开关是关的，只能输出error级别日志")
        }

        // 输出日志到控制台
        Timber.v("vvvvv")
        Timber.d("ddddd")
        Timber.i("iiiii")
        Timber.w("wwwww")
        Timber.e("eeeee")

        // 输出日志到控制台和文件
        Timber.fi("Hello")
        Timber.fe(Exception("惨了"))
        Timber.fe(Exception("又出异常了"), "不慌，没事")

        // Timber.setLogSwitch(true)   // 设置日志开关为开状态(当天有效，第二天自动变成关）
        Timber.logToggle() // 切换日志开关，开变成关，关变成开(当天有效，第二天自动变成关）
    }
}