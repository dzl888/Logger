[TOC]



# 一、日志库描述

项目地址：https://github.com/dzl888/Logger.git

结合了开源框架[Timber](https://github.com/JakeWharton/timber)与[Logback](https://github.com/tony19/logback-android)各自的优点。

1. 使用Timber进行logcat控制台的日志输出，示例如下：

    ```kotlin
    Timber.i("开始登录")
    Timber.w("登录失败")
    Timber.e(Exception("参数错误"))
    Timber.e(Exception(), "写文件时出现异常")
    ```
    
    使用方式与系统的Log类一样，优点是不需要tag参数，默认会以当前类名作为TAG，也可以自定义tag，如下：
    
    ```kotlin
    Timber.tag("MyTag").i("Hello")
    ```
    
2. 使用Logback进行日志文件保存，并且使用Timber对Logback进行了封装，统一了使用方式，加个 f 即可，示例如下：

    ```kotlin
    Timber.fi("开始登录")
    Timber.fw("登录失败")
    Timber.fe("无可用网络")
    Timber.fe(Exception(), "内存不足")
    ```

    带 f 的日志会同时输出到控制台和文件, 输出到文件的内容示例如下:

    ```kotlin
08:14:09 [main] INFO  MainActivity：开始登录
    10:35:54 [main] WARN  MainActivity：登录失败
    ```
    
    输出格式为: 日志发生的时间、所在线程、日志级别、TAG、日志内容
    
    
    
    fe级别的日志会保存在一个bug.txt文件中, 其他 f 级别的会保存在log.txt文件中,而且会按日期保存每天的文件, 保留30天, 文件总大小限制为600M.
    
    保存位置如下:
    
    	- 普通日志：/sdcard/Android/data/应用包名/files/Doucuments/logs/
    	- BUG日志：/sdcard/Android/data/应用包名/files/Doucuments/bugs/
    
    保存位置优点: 兼容所有设备,不需要读写权限, 缺点:应用卸载时文件就没有了
    
3. 调试模式时输出控制台日志, 正式打包后不输出控制台日志(error级别的日志不受此控制,始终输出), 写文件的日志时,控制台不输出日志,但是日志内容还是会写到文件上.
   
4. 在高版本的Android系统中, 虽然在Locat控制台中看不到release版本的应用的进程(模拟器上高版本也能看到进程, 可能是有root权限),但是log还是可以正常输出的, 我们可以直接使用TAG进行过滤日志 , release版本时,日志开关默认是关的, 可通过如下方法打开

    ```kotlin
    Timber.setLogSwitch(true)   // 设置日志开关为开状态（当天有效，第二天自动变成关）
    Timber.getLogSwitch()       // 获取当前日志开关状态
    Timber.logToggle()          // 切换日志开关，开变成关，关变成开（当天有效，第二天自动变成关）
    ```
    
    release模式也可以看到进程, 配置如下:
    
    ```groovy
    android {
        
        buildTypes {
            release {
                debuggable true
            }
        }
    }
    ```
    
    这样配置后,打包release的app也能在logcat看到进程, 但是跟Debug版本没什么区别, 只不过是有一个正式的签名而已, BuildConfig.DEBUG的值也是true, 所以最好不要这么搞。
    
5. 版本最低兼容: API 15(Android4.0.3)

# 二、使用示例

## 1、初始化，以及保存未捕捉异常

```kotlin
class App: Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.init(this, BuildConfig.DEBUG) // 注意,这里的BuildConfig要使用自己应用包名下的,不能用其他包名下的
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            Timber.fe(e, "未捕捉的异常")
        }
    }
    
}
```

## 2、基本使用

```kotLin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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

        // 同时输出日志到控制台和文件
        Timber.fi("Hello")
        Timber.fe(Exception("惨了"))
        Timber.fe(Exception("又出异常了"), "不慌，没事")

        // Timber.setLogSwitch(true)   // 设置日志开关为开状态(当天有效，第二天自动变成关）
        Timber.logToggle() // 切换日志开关，开变成关，关变成开(当天有效，第二天自动变成关）
    }
}
```



# 三、依赖方式

## 1、使用dependencies的方式

1. 在module的build.gradle中添加依赖
   
   ```groovy
   repositories {
       maven { url 'http://192.168.1.251:8081/content/repositories/android_repositories/'}
   }
   
   dependencies {
       implementation 'cn.dazhou.android.log:timber:2.0.0'
   }
   ```
   
3. 初始化Timber

   ```kotlin
   Timber.init(application, BuildConfig.DEBUG) // 建议在Application中初始化. 注意,这里的BuildConfig要使用自己应用包名下的,不能用其他包名下的
   ```

## 2、使用导入aar文件到libs目录的方式

1. 把aar放入libs目录
2. 导出的aar没有依赖传递功能，所以除了添加aar之外还要添加日志库所依赖的库
   ```groovy
   implementation fileTree(dir: "libs", include: ["*.jar", "*.aar"])
   implementation 'org.slf4j:slf4j-api:1.7.30'
   implementation 'com.github.tony19:logback-android:2.0.0'
   ```
2. 初始化Timber

   ```kotlin
   Timber.init(application, BuildConfig.DEBUG) // 建议在Application中初始化. 注意,这里的BuildConfig要使用自己应用包名下的,不能用其他包名下的
   ```
