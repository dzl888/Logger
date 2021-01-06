package com.evendai.loglibrary

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream

//配置应该框架初始化之前就设置好，否则只生效默认的设置
object TimberConfig {

    //source，不允许带空格
    private var graylogSource = "localhost"

    //日志前缀
    var graylogSuffixPattern = " default "

    //服务器地址
    var graylogHost = "192.168.1.214"

    //服务器端口
    var graylogPort = 1514

    //本地缓存大小上限，单位 MB
    var localSize = 600

    //本地存储时间长度,保存 30 天的历史记录(30天后会删除最老的日志文件)
    var localMaxHistory = 30

    //日志格式
    var logFormat = "%d{yyyy_MM_dd HH:mm:ss} [%thread] %-5level %msg%n"

    //声明日志来源【来自系统、用户、或者其他等等】
    var facility = "USER"

    var rootPath = " /storage/emulated/0"
    var packName = "aaa.aaa.logger"

    //生成配置字符串
    private fun getConfigStr(): String {
        return """
        <configuration>
            <property name="LOG_DIR" value="/data/data/com.example/files" />
            
            <!--当Logback运行起来后创建了日志文件，如果此时手动删除，则Logback不会再创建文件出来了，写日志也就写不到了，除非杀进程重启才行-->

            <!--用于保存运行日志的Appender配置-->
            <appender name="FILE_LOG" class="ch.qos.logback.core.rolling.RollingFileAppender">

                <!--过滤：不要ERROR级别，其它级别都要-->
                <filter class="ch.qos.logback.classic.filter.LevelFilter">
                    <level>ERROR</level>
                    <onMatch>DENY</onMatch>
                    <onMismatch>ACCEPT</onMismatch>
                </filter>

                <!--PACKAGE_NAME 应用包名-->
                <!--好像写数据到/sdcard/Android/data/应用包名目录下是不需要权限，但是实验证明需要写外部存储权限才可以-->
                <file>$rootPath/Android/data/$packName/files/Documents/logs/log.txt</file>
                <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                    <!-- 按天轮转，注：默认是使用UTC时间来轮转的，中国的时区要比它慢8小时，所以在中国时区的0点的时候并不会轮转，因为此时UTC时间为16:00，所以下面指定了上海时区 -->
                    <fileNamePattern>$rootPath/Android/data/$packName/files/Documents/logs/log_%d{yyyy_MM_dd,Asia/Shanghai}.txt
                    </fileNamePattern>
                    <!-- 保存 30 天的历史记录(30天后会删除最老的日志文件)，最大大小为 600M -->
                    <maxHistory>${localMaxHistory}</maxHistory>
                    <totalSizeCap>${localSize}MB</totalSizeCap>
                </rollingPolicy>

                <encoder>
                    <!--
                    %d{HH:mm:ss}    时间：12:06:35
                    [%thread]       线程：[main]
                    %-5level        等级：INFO  （长度为5位，不够的补空格）
                    %msg            日志内容
                    %n              换行
                    -->
                    <pattern>${logFormat}</pattern>
                </encoder>
            </appender>

            <!--用于保存Bug的Appender配置-->
            <appender name="FILE_BUG" class="ch.qos.logback.core.rolling.RollingFileAppender">
                <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
                    <!-- 过虑掉小于ERROR级别的日志，所有级别: TRACE < DEBUG < INFO < WARN < ERROR -->
                    <level>ERROR</level>
                </filter>

                <file>$rootPath/Android/data/$packName/files/Documents/bugs/bug.txt</file>
                <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                    <fileNamePattern>$rootPath/Android/data/$packName/files/Documents/bugs/bug_%d{yyyy_MM_dd,Asia/Shanghai}.txt
                    </fileNamePattern>
                    <maxHistory>${localMaxHistory}</maxHistory>
                    <totalSizeCap>${localSize}MB</totalSizeCap>
                </rollingPolicy>

                <encoder>
                    <pattern>${logFormat}</pattern>
                </encoder>
            </appender>
            
            <appender name="PAPERTRAIL" class="com.evendai.loglibrary.MySyslogAppender">
                <suffixPattern>${graylogSuffixPattern} ${logFormat}</suffixPattern>
                <syslogHost>${graylogHost}</syslogHost>
                <port>${graylogPort}</port>
                <facility>$facility</facility>
                <lazy>true</lazy>
            </appender>

            <appender name="ASYNC" class="ch.qos.logback.classic.AsyncAppender">
                <appender-ref ref="PAPERTRAIL" />
            </appender>


            <root level="trace">
                <appender-ref ref="FILE_LOG" />
                <appender-ref ref="FILE_BUG" />
                <appender-ref ref="ASYNC" />
            </root>

        </configuration>

    """.trimIndent()
    }

    fun getGraylogSource(): String = graylogSource

    fun setGraylogSource(graylogSource: String) {
        this.graylogSource = graylogSource
    }

    //%d{yyyy_MM_dd HH:mm:ss} %-5level %logger{35}: %m%n%xEx
    //ch.qos.logback.classic.net.SyslogAppender

    //设置配置
    fun configureLogbackByString() {
        val lc = LoggerFactory.getILoggerFactory() as LoggerContext
        lc.stop()

        val config = JoranConfigurator()
        config.context = lc

        val stream = ByteArrayInputStream(getConfigStr().toByteArray())
        try {
            config.doConfigure(stream)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}