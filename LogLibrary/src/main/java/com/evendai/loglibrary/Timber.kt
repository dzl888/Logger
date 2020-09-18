@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.evendai.loglibrary

import android.annotation.SuppressLint
import android.app.Application
import android.os.Build
import android.os.Environment
import android.text.format.DateFormat
import android.util.Base64
import android.util.Log
import androidx.preference.PreferenceManager
import org.jetbrains.annotations.NonNls
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*
import java.util.regex.Pattern
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/** 懒人的日志工具  */
@Suppress("MemberVisibilityCanBePrivate")
class Timber private constructor() {

    init {
        throw AssertionError("No instances.") // 不允许实例化
    }

    /** 用于处理日志调用的外观。通过Timber.plant()方法安装实例  */
    abstract class Tree {

        /** 显式的tag，用于保存用户手动指定的tag */
        val explicitTag = ThreadLocal<String>()

        /** 取出用户手动设置的tag，使用完立即丢弃，即用户手动设置的tag只能作用于一次log输出 */
        open fun getTag(): String? {
            val tag = explicitTag.get()
            if (tag != null) {
                explicitTag.remove()
            }
            return tag
        }

        /** 记录一个使用可选格式args的verbose消息。  */
        open fun v(message: String, vararg args: Any) {
            prepareLog(Log.VERBOSE, null, message, *args)
        }

        /** 记录一个verbose的异常和使用可选格式args的消息。  */
        open fun v(t: Throwable, message: String, vararg args: Any) {
            prepareLog(Log.VERBOSE, t, message, *args)
        }

        /** 记录一个verbose异常。  */
        open fun v(t: Throwable) {
            prepareLog(Log.VERBOSE, t, null)
        }

        /** 记录一个使用可选格式args的debug消息。  */
        open fun d(message: String, vararg args: Any) {
            prepareLog(Log.DEBUG, null, message, *args)
        }

        /** 记录一个debug异常和使用可选格式args的消息。  */
        open fun d(t: Throwable, message: String, vararg args: Any) {
            prepareLog(Log.DEBUG, t, message, *args)
        }

        /** 记录一个debug异常。  */
        open fun d(t: Throwable) {
            prepareLog(Log.DEBUG, t, null)
        }

        /** 记录一个使用可选格式args的info消息。  */
        open fun i(message: String, vararg args: Any) {
            prepareLog(Log.INFO, null, message, *args)
        }

        /** 记录一个info异常和使用可选格式args的消息。 */
        open fun i(t: Throwable, message: String, vararg args: Any) {
            prepareLog(Log.INFO, t, message, *args)
        }

        /** 记录一个info异常。  */
        open fun i(t: Throwable) {
            prepareLog(Log.INFO, t, null)
        }

        /** 记录一个使用可选格式args的warning消息。  */
        open fun w(message: String, vararg args: Any) {
            prepareLog(Log.WARN, null, message, *args)
        }

        /** 记录一个warning异常和使用可选格式args的消息。  */
        open fun w(t: Throwable, message: String, vararg args: Any) {
            prepareLog(Log.WARN, t, message, *args)
        }

        /** 记录一个warning异常。 */
        open fun w(t: Throwable) {
            prepareLog(Log.WARN, t, null)
        }

        /** 记录一个使用可选格式args的error消息。 */
        open fun e(message: String, vararg args: Any) {
            prepareLog(Log.ERROR, null, message, *args)
        }

        /** 记录一个error异常和使用可选格式args的消息。  */
        open fun e(t: Throwable, message: String, vararg args: Any) {
            prepareLog(Log.ERROR, t, message, *args)
        }

        /** 记录一个error异常。  */
        open fun e(t: Throwable) {
            prepareLog(Log.ERROR, t, null)
        }

        /** 记录一个使用可选格式args的assert消息 */
        open fun wtf(message: String, vararg args: Any) {
            prepareLog(Log.ASSERT, null, message, *args)
        }

        /** 记录一个assert异常和使用可选格式args的消息。  */
        open fun wtf(t: Throwable, message: String, vararg args: Any) {
            prepareLog(Log.ASSERT, t, message, *args)
        }

        /** 记录一个assert异常。 */
        open fun wtf(t: Throwable) {
            prepareLog(Log.ASSERT, t, null)
        }

        /** 记录一个使用可先格式args和指定priority的消息。 */
        open fun log(priority: Int, message: String, vararg args: Any) {
            prepareLog(priority, null, message, *args)
        }

        /** 记录一个异常和使用可先格式args和指定priority的消息。  */
        open fun log(priority: Int, t: Throwable, message: String, vararg args: Any) {
            prepareLog(priority, t, message, *args)
        }

        /** 记录一个指定priority的消息  */
        open fun log(priority: Int, t: Throwable) {
            prepareLog(priority, t, null)
        }

        /** 返回指定的tag或priority的消息是否应该被记录  */
        protected open fun isLoggable(tag: String, priority: Int): Boolean {
            return true
        }

        private fun prepareLog(priority: Int, t: Throwable?, message: String?, vararg args: Any) {
            var msg = message
            val tag = getTag() // 即消息不可记录也要消费tag事件，以便下一个消息的tag是正确的
            val needWriteToFile = msg?.startsWith(WRITE_FILE_FLAG) ?: false
            if (tag == null || (!needWriteToFile && !isLoggable(tag, priority))) {
                // 判断needWriteToFile的原因是：如果需要写文件的话，即使isLoggable方法返回false，我们也要记录日记
                return
            }

            if (msg.isNullOrBlank()) {
                if (t == null) {
                    return  // 如果消息是空的而且也没有异常对象，则忽略此消息
                }
                msg = getStackTraceString(t) // 获取异常的堆栈信息用作为消息
            } else {
                if (args.isNotEmpty()) {
                    // 如果消息使用了格式化，则先把消息进行格式化
                    msg = formatMessage(msg, args)
                }
                if (t != null) {
                    // 如果有异常对象，则把异常的堆栈信息拼接到消息的后面（换行显示）
                    msg += "\n${getStackTraceString(t)}"
                }
            }
            log(priority, tag, msg, t)
        }

        /** 使用可选参数格式化消息 */
        protected fun formatMessage(message: String, vararg args: Any): String {
            return String.format(message, *args)
        }

        /** 获取异常的堆栈信息 */
        private fun getStackTraceString(t: Throwable): String {
            // 不要用Log.getStackTraceString（）代替它，它隐藏了UnknownHostException，这不是我们想要的。
            val sw = StringWriter(256)
            val pw = PrintWriter(sw, false)
            t.printStackTrace(pw)
            pw.flush()
            return sw.toString()
        }

        /**
         * 将日志消息写入它的目的地。默认情况下，调用所有指定priority的方法。
         * @param priority 日志级别。有关常量，请参见[Log]。
         * @param tag 显式或推断的tag。
         * @param message 格式化的日志消息，可能为null，如果消息为null则Throwable就不能为null
         * @param t 伴随的异常，可能为null，如果异常为null，则message就不能为null
         */
        protected abstract fun log(priority: Int, tag: String, message: String?, t: Throwable?)
    }

    /** 用于调试的[Tree]。自动从调用类中推断出tag。  */
    open class DebugTree : Tree() {
        /**
         * 从element中提取用于消息的tag。 默认情况下，将使用没有任何匿名类后缀(如Foo$1变成Foo)的类名。
         * 注意：如果手动设置了一个[Timber.tag(String)][Timber.tag]，则此方法不会被调用
         */
        protected fun createStackElementTag(element: StackTraceElement): String? {
            var tag = element.className
            val m = ANONYMOUS_CLASS.matcher(tag)
            if (m.find()) {
                tag = m.replaceAll("") // 如果tag中包含匿名类，则把匿名类替换为空字符串
            }
            tag = tag.substring(tag.lastIndexOf('.') + 1) // 取现类名（不要包名）
            // tag长度限制已在API 24中删除。
            return if (tag.length <= MAX_TAG_LENGTH || Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                tag
            } else tag.substring(0, MAX_TAG_LENGTH) // 最大tag长度为23个字符
        }

        override fun getTag(): String? {
            val tag = super.getTag() // 取出用户手动设置的tag
            if (tag != null) {
                return tag // 优先使用用户手动设置的tag
            }

            // 请不要把这个切换为Thread.getCurrentThread().getStackTrace()，Robolectric在JVM上运行它们测试通过，但是在Android元素上是不一样的。
            val stackTrace = Throwable().stackTrace
            check(stackTrace.size > CALL_STACK_INDEX) { "合成的堆栈跟踪没有足够的元素：您是否正在使用代码混淆？" }
            return createStackElementTag(stackTrace[CALL_STACK_INDEX]) // 从堆栈信息的第5层信息中取出类名作为tag
        }

        /**
         * 将“消息”分解为最大长度的块（如果需要）并发送给[Log.println()][Log.println]或[Log.wtf()][Log.wtf]
         * {@inheritDoc}
         */
        override fun log(priority: Int, tag: String, message: String?, t: Throwable?) {
            var msg = message!!  // 注意：message中已经拼接上异常对象的堆栈信息了，所以不可能为空的
            val needWriteToFile = msg.startsWith(WRITE_FILE_FLAG)
            if (needWriteToFile) {
                msg = msg.replace(WRITE_FILE_FLAG, "")
                val msgForLogback = "$tag：$msg" // 构建一个用于logback的消息
                when (priority) {
                    Log.VERBOSE -> logback.trace(msgForLogback)
                    Log.DEBUG -> logback.debug(msgForLogback)
                    Log.INFO -> logback.info(msgForLogback)
                    Log.WARN -> logback.warn(msgForLogback)
                    Log.ERROR -> {
                        if (t != null) {
                            logback.warn("发生了异常： ${t.javaClass.name}(${t.message})")
                            logback.error(msgForLogback) // 有异常对象时，error级别的日志会单独保存在一个Bug文件中。
                            //logger.error(msgForLogback, t); // Timber对象已经把异常数据都封装到message对象中了，所以不需要用这个方法
                        } else {
                            logback.warn(msgForLogback)
                        }
                    }
                }
            }
            if (msg.length < MAX_LOG_LENGTH) {
                if (priority == Log.ASSERT) {
                    Log.wtf(tag, msg)
                } else {
                    Log.println(priority, tag, msg)
                }
                return
            }

            // 按行拆分，然后确保每行都可以容纳Log的最大长度。
            var i = 0
            val length = msg.length
            while (i < length) {
                var newline = msg.indexOf('\n', i)
                newline = if (newline != -1) newline else length
                do {
                    val end = newline.coerceAtMost(i + MAX_LOG_LENGTH)
                    val part = msg.substring(i, end)
                    if (priority == Log.ASSERT) {
                        Log.wtf(tag, part)
                    } else {
                        Log.println(priority, tag, part)
                    }
                    i = end
                } while (i < newline)
                i++
            }
        }

        companion object {
            private const val MAX_LOG_LENGTH = 4000
            private const val MAX_TAG_LENGTH = 23
            private const val CALL_STACK_INDEX = 5
            private val ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$")
        }
    }

    /**
     * Log开关控制类，默认只输出error级别的Log，其他级别log的输出通过setLogSwitch(boolean)进行设置，
     * 输出当天有效，第二天恢复只输出error级别的log。
     * 在使用此对象之前，必须先调用init(context)函数，建议在Application中进行调用
     */
    class DefaultTree: Timber.DebugTree() {

        private val key: String = "LoggerSwitch"
        /** 用于获取保存配置的对象（SharedPreferences） */
        private lateinit var context: Application
        /** 用于保存当天的日期 */
        private var mToday: CharSequence? = null
        /** 用于控制是否显示Log */
        private var showLog = false
        private var debuggable = false

        /** 使用AES进行加密，加密后的数据使用Base64编码为String */
        fun encrypt(rawData: String): String = Base64.encodeToString(getCipher(Cipher.ENCRYPT_MODE).doFinal(rawData.toByteArray()), Base64.NO_WRAP)

        /** 把AES加密并通过Base64编码的String进行解密，还原为原始的String */
        fun decrypt(base64Data: String): String = String(getCipher(Cipher.DECRYPT_MODE).doFinal(Base64.decode(base64Data, Base64.NO_WRAP)))

        @SuppressLint("GetInstance")
        private fun getCipher(mode: Int) = Cipher.getInstance("AES/ECB/PKCS5Padding").apply { init(mode, SecretKeySpec("abc3efgabcdef119".toByteArray(), "AES")) }

        /**
         * 初始化LogTree, 在使用此对象之前，必须先调用init(context)函数，建议在Application中进行调用
         * @param context 用于获取保存配置的对象（SharedPreferences）
         * @param debuggable 是否是可调试的，建议传BuildConfig.DEBUG
         */
        fun init(context: Application, debuggable: Boolean) {
            this.context = context
            this.debuggable = debuggable
        }

        /** 设置是否显示Log，并持久化该参数，且只有当天有效，第二天自动变成不显示Log */
        fun setLogSwitch(isShowLog: Boolean) {
            showLog = isShowLog
            putString("$key${getToday()}", isShowLog.toString())
        }

        /** 获取Log开关，true为显示Log，false则不显示 */
        fun getLogSwitch(): Boolean {
            val today = getToday()
            if (today != mToday) {
                mToday = today
                showLog = getString("$key${getToday()}") == "true" // 如果一天已经过去了，则今天也会变，所以不能写死
            }
            return showLog
        }

        /** 切换Log的显示为相反状态 */
        fun logToggle() = setLogSwitch(!showLog)

        /** 根据log级别判断是否输出log, error级别的log总是要显示的, 其他级别的Log是否输出要取决于设置的log开关 */
        override fun isLoggable(tag: String, priority: Int) : Boolean {
            return if (debuggable || priority == Log.ERROR) true else getLogSwitch()
        }

        /** 获取今天 */
        private fun getToday() = DateFormat.format("yyyy-MM-dd", Date())

        /** 存一个String到配置文件中 */
        private fun putString(key: String, value: String) {
            PreferenceManager.getDefaultSharedPreferences(context).edit().putString(encrypt(key), encrypt(value)).apply()
        }

        /** 从配置文件中取出一个String */
        private fun getString(key: String): String? {
            return PreferenceManager.getDefaultSharedPreferences(context).getString(encrypt(key), null)?.let { decrypt(it) }
        }

    }

    companion object {
        /** 写文件的标志，用于logback */
        private const val WRITE_FILE_FLAG = "--file--"

        /** logback，用于把日志写到文件中 */
        private lateinit var logback: Logger

        /** 记录一个使用可选格式args的verbose消息。  */
        fun v(@NonNls message: String, vararg args: Any) {
            TREE_OF_SOULS.v(message, *args)
        }

        /** 记录一个使用可选格式args的verbose消息，并写入日志文件。  */
        fun fv(@NonNls message: String, vararg args: Any) {
            TREE_OF_SOULS.v(WRITE_FILE_FLAG + message, *args)
        }

        /** 记录一个verbose的异常和使用可选格式args的消息。  */
        fun v(t: Throwable, @NonNls message: String, vararg args: Any) {
            TREE_OF_SOULS.v(t, message, *args)
        }

        /** 记录一个verbose的异常和使用可选格式args的消息，并写入日志文件。 */
        fun fv(t: Throwable, @NonNls message: String, vararg args: Any) {
            TREE_OF_SOULS.v(t, WRITE_FILE_FLAG + message, *args)
        }

        /** 记录一个verbose异常。  */
        fun v(t: Throwable) {
            TREE_OF_SOULS.v(t)
        }

        /** 记录一个verbose异常，并写入日志文件。  */
        fun fv(t: Throwable) {
            TREE_OF_SOULS.v(t, WRITE_FILE_FLAG)
        }

        /** 记录一个使用可选格式args的debug消息。  */
        fun d(@NonNls message: String, vararg args: Any) {
            TREE_OF_SOULS.d(message, *args)
        }

        /** 记录一个使用可选格式args的debug消息，并写入日志文件。  */
        fun fd(@NonNls message: String, vararg args: Any) {
            TREE_OF_SOULS.d(WRITE_FILE_FLAG + message, *args)
        }

        /** 记录一个debug异常和使用可选格式args的消息。  */
        fun d(t: Throwable, @NonNls message: String, vararg args: Any) {
            TREE_OF_SOULS.d(t, message, *args)
        }

        /** 记录一个debug异常和使用可选格式args的消息，并写入日志文件。  */
        fun fd(t: Throwable, @NonNls message: String, vararg args: Any) {
            TREE_OF_SOULS.d(t, WRITE_FILE_FLAG + message, *args)
        }

        /** 记录一个debug异常 */
        fun d(t: Throwable) {
            TREE_OF_SOULS.d(t)
        }

        /** 记录一个debug异常，并写入日志文件。 */
        fun fd(t: Throwable) {
            TREE_OF_SOULS.d(t, WRITE_FILE_FLAG)
        }

        /** 记录一个使用可选格式args的info消息。  */
        fun i(@NonNls message: String, vararg args: Any) {
            TREE_OF_SOULS.i(message, *args)
        }

        /** 记录一个使用可选格式args的info消息，并写入日志文件。  */
        fun fi(@NonNls message: String, vararg args: Any) {
            TREE_OF_SOULS.i(WRITE_FILE_FLAG + message, *args)
        }

        /** 记录一个info异常和使用可选格式args的消息。 */
        fun i(t: Throwable, @NonNls message: String, vararg args: Any) {
            TREE_OF_SOULS.i(t, message, *args)
        }

        /** 记录一个info异常和使用可选格式args的消息，并写入日志文件。 */
        fun fi(t: Throwable, @NonNls message: String, vararg args: Any) {
            TREE_OF_SOULS.i(t, WRITE_FILE_FLAG + message, *args)
        }

        /** 记录一个info异常。  */
        fun i(t: Throwable) {
            TREE_OF_SOULS.i(t)
        }

        /** 记录一个info异常，并写入日志文件。  */
        fun fi(t: Throwable) {
            TREE_OF_SOULS.i(t, WRITE_FILE_FLAG)
        }

        /** 记录一个使用可选格式args的warning消息。   */
        fun w(@NonNls message: String, vararg args: Any) {
            TREE_OF_SOULS.w(message, *args)
        }

        /** 记录一个使用可选格式args的warning消息，并写入日志文件。 */
        fun fw(@NonNls message: String, vararg args: Any) {
            TREE_OF_SOULS.w(WRITE_FILE_FLAG + message, *args)
        }

        /** 记录一个warning异常和使用可选格式args的消息。  */
        fun w(t: Throwable, @NonNls message: String, vararg args: Any) {
            TREE_OF_SOULS.w(t, message, *args)
        }

        /** 记录一个warning异常和使用可选格式args的消息，并写入日志文件。  */
        fun fw(t: Throwable, @NonNls message: String, vararg args: Any) {
            TREE_OF_SOULS.w(t, WRITE_FILE_FLAG + message, *args)
        }

        /** 记录一个warning异常。 */
        fun w(t: Throwable) {
            TREE_OF_SOULS.w(t)
        }

        /** 记录一个warning异常，并写入日志文件。 */
        fun fw(t: Throwable) {
            TREE_OF_SOULS.w(t, WRITE_FILE_FLAG)
        }

        /** 记录一个使用可选格式args的error消息。 */
        fun e(@NonNls message: String, vararg args: Any) {
            TREE_OF_SOULS.e(message, *args)
        }

        /** 记录一个使用可选格式args的error消息，并写入日志文件。 */
        fun fe(@NonNls message: String, vararg args: Any) {
            TREE_OF_SOULS.e(WRITE_FILE_FLAG + message, *args)
        }

        /** 记录一个error异常和使用可选格式args的消息。  */
        fun e(t: Throwable, @NonNls message: String, vararg args: Any) {
            TREE_OF_SOULS.e(t, message, *args)
        }

        /** 记录一个error异常和使用可选格式args的消息，并写入日志文件。 */
        fun fe(t: Throwable, @NonNls message: String, vararg args: Any) {
            TREE_OF_SOULS.e(t, WRITE_FILE_FLAG + message, *args)
        }

        /** 记录一个error异常。*/
        fun e(t: Throwable) {
            TREE_OF_SOULS.e(t)
        }

        /** 记录一个error异常，并写入日志文件。*/
        fun fe(t: Throwable) {
            TREE_OF_SOULS.e(t, WRITE_FILE_FLAG)
        }

        /** 记录一个使用可选格式args的assert消息 */
        fun wtf(@NonNls message: String, vararg args: Any) {
            TREE_OF_SOULS.wtf(message, *args)
        }

        /** 记录一个使用可选格式args的assert消息，并写入日志文件。 */
        fun fwtf(@NonNls message: String, vararg args: Any) {
            TREE_OF_SOULS.wtf(WRITE_FILE_FLAG + message, *args)
        }

        /** 记录一个assert异常和使用可选格式args的消息。  */
        fun wtf(t: Throwable, @NonNls message: String, vararg args: Any) {
            TREE_OF_SOULS.wtf(t, message, *args)
        }

        /** 记录一个assert异常和使用可选格式args的消息，并写入日志文件。 */
        fun fwtf(t: Throwable, @NonNls message: String, vararg args: Any) {
            TREE_OF_SOULS.wtf(t, WRITE_FILE_FLAG + message, *args)
        }

        /** 记录一个assert异常。 */
        fun wtf(t: Throwable) {
            TREE_OF_SOULS.wtf(t)
        }

        /** 记录一个assert异常，并写入日志文件。 */
        fun fwtf(t: Throwable) {
            TREE_OF_SOULS.wtf(t, WRITE_FILE_FLAG)
        }

        /** 记录一个使用可先格式args和指定priority的消息。 */
        fun log(priority: Int, @NonNls message: String, vararg args: Any) {
            TREE_OF_SOULS.log(priority, message, *args)
        }

        /** 记录一个异常和使用可先格式args和指定priority的消息。  */
        fun log(priority: Int, t: Throwable, @NonNls message: String, vararg args: Any) {
            TREE_OF_SOULS.log(priority, t, message, *args)
        }

        /** 记录一个指定priority的消息  */
        fun log(priority: Int, t: Throwable) {
            TREE_OF_SOULS.log(priority, t)
        }

        /**
         * A view into Timber's planted trees as a tree itself. This can be used for injecting a logger
         * instance rather than using static methods or to facilitate testing.
         * 视图进入木材真实植树作为树本身。 这可以用于注入一个记录器实例，而不是使用静态方法或方便测试。
         */
        fun asTree(): Tree {
            return TREE_OF_SOULS
        }

        /** 设置一个一次性tag，用于下一个日志记录调用。  */
        fun tag(tag: String): Tree {
            for (tree in forestAsArray) {
                tree.explicitTag.set(tag)
            }
            return TREE_OF_SOULS
        }

        private var defaultTree: DefaultTree? = null

        /** 设置是否显示Log，并持久化该参数，且只有当天有效，第二天自动变成不显示Log。注：只对DefaultTree生效 */
        fun setLogSwitch(isShowLog: Boolean) {
            defaultTree?.setLogSwitch(isShowLog)
        }

        /** 获取Log开关，true为显示Log，false则不显示。注：只对DefaultTree生效  */
        fun getLogSwitch(): Boolean {
            return defaultTree?.getLogSwitch() ?: false
        }

        /** 切换Log的显示为相反状态。注：只对DefaultTree生效 */
        fun logToggle() = setLogSwitch(!(defaultTree?.getLogSwitch() ?: false))

        /**
         * 植入一棵默认的Tree。在使用Timer记录日志之前，必须先植入一棵树。也可调用[Timber.plant]来植入其它的树
         * @param context 用于获取保存配置的对象（SharedPreferences）
         * @param debuggable 是否是可调试的，建议传BuildConfig.DEBUG，这样在Debug模式会输入log，打包后不输出log
         */
        fun init(context: Application, debuggable: Boolean) {
            // Environment.DIRECTORY_DOCUMENTS此变量在API19才有
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            logback = LoggerFactory.getLogger(Timber::class.java)
            defaultTree = DefaultTree()
            defaultTree!!.init(context, debuggable)
            plant(defaultTree!!)
        }

        /** 添加一个新的日志树。  */
        // Validating public API contract.（验证公共API合同。）
        fun plant(tree: Tree) {
            require(!(tree === TREE_OF_SOULS)) { "不能将Timber植入它自身。" }
            synchronized(FOREST) {
                FOREST.add(tree) // 把树放入集合中
                forestAsArray = FOREST.toTypedArray() // 把集合转换为对应的数组
            }
        }

        /** 添加新的日志记录树。  */
        // Validating public API contract.（验证公共API合同。）
        fun plant(vararg trees: Tree) {
            for (tree in trees) {
                require(!(tree === TREE_OF_SOULS)) { "不能将Timber植入它自身。" }
            }
            synchronized(FOREST) {
                Collections.addAll(FOREST, *trees)
                forestAsArray = FOREST.toTypedArray()
            }
        }

        /** 拔掉一棵已经种下的树。*/
        fun uproot(tree: Tree) {
            synchronized(FOREST) {
                require(FOREST.remove(tree)) { "无法拔掉一棵没种下的树: $tree" }
                forestAsArray = FOREST.toTypedArray()
            }
        }

        /** 拔掉所有已经种下的树 */
        fun uprootAll() {
            synchronized(FOREST) {
                FOREST.clear()
                forestAsArray = TREE_ARRAY_EMPTY
            }
        }

        /** 返回所有已种植的[tree][Tree]的副本。 */
        fun forest(): List<Tree> {
            synchronized(FOREST) {
                return Collections.unmodifiableList(ArrayList(FOREST))
            }
        }

        /** 返回所有已种下的树的数量 */
        fun treeCount(): Int {
            synchronized(FOREST) { return FOREST.size }
        }

        /* 一个空的Tree数组 */
        private val TREE_ARRAY_EMPTY = arrayOf<Tree>()

        // 这两个字段都由“FOREST”守卫。
        private val FOREST: MutableList<Tree> = ArrayList()
        @Volatile var forestAsArray = TREE_ARRAY_EMPTY

        /** 灵魂之树，此Tree对象将会把工作委派给种植在[Timber.FOREST][Timber.FOREST]中的所有的[Tree] */
        private val TREE_OF_SOULS: Tree = object : Tree() {
            override fun v(message: String, vararg args: Any) {
                forestAsArray.forEach { it.v(message, *args) }
            }

            override fun v(t: Throwable, message: String, vararg args: Any) {
                forestAsArray.forEach { it.v(t, message, *args) }
            }

            override fun v(t: Throwable) {
                forestAsArray.forEach { it.v(t) }
            }

            override fun d(message: String, vararg args: Any) {
                forestAsArray.forEach { it.d(message, *args) }
            }

            override fun d(t: Throwable, message: String, vararg args: Any) {
                forestAsArray.forEach { it.d(t, message, *args) }
            }

            override fun d(t: Throwable) {
                forestAsArray.forEach { it.d(t) }
            }

            override fun i(message: String, vararg args: Any) {
                forestAsArray.forEach { it.i(message, *args) }
            }

            override fun i(t: Throwable, message: String, vararg args: Any) {
                forestAsArray.forEach { it.i(t, message, *args) }
            }

            override fun i(t: Throwable) {
                forestAsArray.forEach { it.i(t) }
            }

            override fun w(message: String, vararg args: Any) {
                forestAsArray.forEach { it.w(message, *args) }
            }

            override fun w(t: Throwable, message: String, vararg args: Any) {
                forestAsArray.forEach { it.w(t, message, *args) }
            }

            override fun w(t: Throwable) {
                forestAsArray.forEach { it.w(t) }
            }

            override fun e(message: String, vararg args: Any) {
                forestAsArray.forEach { it.e(message, *args) }
            }

            override fun e(t: Throwable, message: String, vararg args: Any) {
                forestAsArray.forEach { it.e(t, message, *args) }
            }

            override fun e(t: Throwable) {
                forestAsArray.forEach { it.e(t) }
            }

            override fun wtf(message: String, vararg args: Any) {
                forestAsArray.forEach { it.wtf(message, *args) }
            }

            override fun wtf(t: Throwable, message: String, vararg args: Any) {
                forestAsArray.forEach { it.wtf(t, message, *args) }
            }

            override fun wtf(t: Throwable) {
                forestAsArray.forEach { it.wtf(t) }
            }

            override fun log(priority: Int, message: String, vararg args: Any) {
                forestAsArray.forEach { it.log(priority, message, *args) }
            }

            override fun log(priority: Int, t: Throwable, message: String, vararg args: Any) {
                forestAsArray.forEach { it.log(priority, t, message, *args) }
            }

            override fun log(priority: Int, t: Throwable) {
                forestAsArray.forEach { it.log(priority, t) }
            }

            override fun log(priority: Int, tag: String, message: String?, t: Throwable?) {
                throw AssertionError("没有覆盖此方法，不应该调用此方法")
            }
        }
    }
}