package com.evendai.loglibrary;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.util.LevelToSyslogSeverity;
import ch.qos.logback.core.net.SyslogAppenderBase;

/**
 * 为了解决 localhostName 不能被修改，导致后台source一直是错误的源的问题
 */
public class MySyslogStartConverter extends ClassicConverter {

    long lastTimestamp = -1;
    String timesmapStr = null;
    SimpleDateFormat simpleMonthFormat;
    SimpleDateFormat simpleTimeFormat;
    private final Calendar calendar  = Calendar.getInstance(Locale.US);

    int facility;

    public void start() {
        int errorCount = 0;

        String facilityStr = getFirstOption();
        if (facilityStr == null) {
            addError("was expecting a facility string as an option");
            return;
        }

        facility = SyslogAppenderBase.facilityStringToint(facilityStr);

        try {
            // hours should be in 0-23, see also http://jira.qos.ch/browse/LBCLASSIC-48
            simpleMonthFormat = new SimpleDateFormat("MMM", Locale.US);
            simpleTimeFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);
            simpleTimeFormat.setDateFormatSymbols(new DateFormatSymbols(Locale.US));
            simpleMonthFormat.setDateFormatSymbols(new DateFormatSymbols(Locale.US));
        } catch (IllegalArgumentException e) {
            addError("Could not instantiate SimpleDateFormat", e);
            errorCount++;
        }

        if(errorCount == 0) {
            super.start();
        }
    }

    public String convert(ILoggingEvent event) {
        StringBuilder sb = new StringBuilder();

        int pri = facility + LevelToSyslogSeverity.convert(event);

        sb.append("<");
        sb.append(pri);
        sb.append(">");
        sb.append(computeTimeStampString(event.getTimeStamp()));
        sb.append(' ');
        sb.append(TimberConfig.INSTANCE.getGraylogSource());
        sb.append(' ');

        return sb.toString();
    }

    private String computeTimeStampString(long now) {
        synchronized (this) {
            // Since the formatted output is only precise to the second, we can use the same cached string if the current
            // second is the same (stripping off the milliseconds).
            if ((now / 1000) != lastTimestamp) {
                lastTimestamp = now / 1000;
                Date nowDate = new Date(now);
                calendar.setTime(nowDate);
                timesmapStr = String.format(Locale.US, "%s %2d %s", simpleMonthFormat.format(nowDate),
                        calendar.get(Calendar.DAY_OF_MONTH), simpleTimeFormat.format(nowDate));
            }
            return timesmapStr;
        }
    }
}
