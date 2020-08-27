package com.evendai.loglibrary

import android.app.Application
import android.text.format.DateFormat
import android.util.Log
import androidx.preference.PreferenceManager
import java.util.*

/**
 * Log开关控制类，默认只输出error级别的Log，其他级别log的输出通过setLogSwitch(boolean)进行设置，输出当天有效，第二天恢复只输出error级别的log
 * 在使用此对象之前，必须先调用init(context)函数，建议在Application中进行调用
 */
object LogTree: Timber.DebugTree() {

    private const val KEY: String = "LoggerSwitch"
    /** 用于获取保存配置的对象（SharedPreferences） */
    private lateinit var context: Application
    /** 用于保存当天的日期 */
    private var mToday: CharSequence? = null
    /** 用于控制是否显示Log */
    private var showLog = false

    /**
     * 初始化LogTree, 在使用此对象之前，必须先调用init(context)函数，建议在Application中进行调用
     * @param context 用于获取保存配置的对象（SharedPreferences）
     */
    fun init(context: Application) {
        LogTree.context = context
    }

    /** 设置是否显示Log，并持久化该参数，且只有当天有效，第二天自动变成不显示Log */
    private fun setLogSwitch(isShowLog: Boolean) {
        showLog = isShowLog
        putString("$KEY${getToday()}", isShowLog.toString())
    }

    /** 获取Log开关，true为显示Log，false则不显示 */
    fun getLogSwitch(): Boolean {
        val today = getToday()
        if (today != mToday) {
            mToday = today
            showLog = getString("$KEY${getToday()}") == "true" // 如果一天已经过去了，则今天也会变，所以不能写死
        }
        return showLog
    }

    /** 切换Log的显示为相反状态 */
    fun logToggle() = setLogSwitch(!showLog)

    /** 根据log级别判断是否输出log, error级别的log总是要显示的, 其他级别的Log是否输出要取决于设置的log开关 */
    override fun isLoggable(tag: String?, priority: Int) = if (BuildConfig.DEBUG || priority == Log.ERROR) true else getLogSwitch()

    /** 获取今天 */
    private fun getToday() = DateFormat.format("yyyy-MM-dd", Date())

    /** 存一个String到配置文件中 */
    private fun putString(key: String, value: String) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(Aes.encrypt(key), Aes.encrypt(value)).apply()
    }

    /** 从配置文件中取出一个String */
    private fun getString(key: String): String? {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(Aes.encrypt(key), null)?.let { Aes.decrypt(it) }
    }

}