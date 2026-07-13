package com.yjh.base.utils.util;

import android.annotation.SuppressLint;
import androidx.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by jiahui on 2025/12/30
 */
public class DateUtils extends android.text.format.DateUtils {
    public static final int Second = 0;
    public static final int Minute = 1;
    public static final int Hour = 2;
    public static final int Day = 3;

    @IntDef(value = {Second, Minute, Hour, Day})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DifferenceMode {
    }

    /**
     * 相差时间的秒的部分
     */
    public static long getSecond(Date startDate, Date endDate) {
        return getDayHourMinuteSecondByMode(startDate, endDate, Second);
    }

    /**
     * 相差时间的分钟的部分
     */
    public static long getMinute(Date startDate, Date endDate) {
        return getDayHourMinuteSecondByMode(startDate, endDate, Minute);
    }

    /**
     * 相差时间的小时的部分
     */
    public static long getHour(Date startDate, Date endDate) {
        return getDayHourMinuteSecondByMode(startDate, endDate, Hour);
    }

    /**
     * 相差时间的天的部分
     */
    public static long getDay(Date startDate, Date endDate) {
        return getDayHourMinuteSecondByMode(startDate, endDate, Day);
    }

    public static long getSecond(long startTimeMillis, long endTimeMillis) {
        return getDayHourMinuteSecondByMode(startTimeMillis, endTimeMillis, Second);
    }

    public static long getMinute(long startTimeMillis, long endTimeMillis) {
        return getDayHourMinuteSecondByMode(startTimeMillis, endTimeMillis, Minute);
    }

    public static long getHour(long startTimeMillis, long endTimeMillis) {
        return getDayHourMinuteSecondByMode(startTimeMillis, endTimeMillis, Hour);
    }

    public static long getDay(long startTimeMillis, long endTimeMillis) {
        return getDayHourMinuteSecondByMode(startTimeMillis, endTimeMillis, Day);
    }

    public static int calculateDaysInMonth(int month) {
        return calculateDaysInMonth(0, month);
    }

    public static int calculateDaysInMonth(int year, int month) {
        String[] bigMonths = {"1", "3", "5", "7", "8", "10", "12"};
        String[] littleMonths = {"4", "6", "9", "11"};
        List<String> bigList = Arrays.asList(bigMonths);
        List<String> littleList = Arrays.asList(littleMonths);
        if (bigList.contains(String.valueOf(month))) {
            return 31;
        } else if (littleList.contains(String.valueOf(month))) {
            return 30;
        } else {
            if (year <= 0) {
                return 29;
            }
            if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) {
                return 29;
            } else {
                return 28;
            }
        }
    }

    /**
     * 月日时分秒，0-9前补0
     */
    public static String fillZero(int number) {
        return number < 10 ? "0" + number : "" + number;
    }

    /**
     * 截取掉前缀0以便转换为整数
     */
    public static int trimZero(String text) {
        try {
            if (text.startsWith("0")) {
                text = text.substring(1);
            }
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            LogUtils.warn(e);
            return 0;
        }
    }

    /**
     * 判断传入的Date类型日期是否与「当前系统时间」属于同一天
     */
    public static boolean isSameDay(Date date) {
        if (date == null) {
            throw new IllegalArgumentException("date is null");
        }
        Calendar nowCalendar = Calendar.getInstance();
        Calendar newCalendar = Calendar.getInstance();
        newCalendar.setTime(date);
        return (nowCalendar.get(Calendar.ERA) == newCalendar.get(Calendar.ERA) &&
                nowCalendar.get(Calendar.YEAR) == newCalendar.get(Calendar.YEAR) &&
                nowCalendar.get(Calendar.DAY_OF_YEAR) == newCalendar.get(Calendar.DAY_OF_YEAR));
    }

    /**
     * 将yyyy-MM-dd HH:mm:ss字符串转换成日期
     */
    public static Date parseDate(String dateStr, String dataFormat) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(dataFormat, Locale.PRC);
            Date date = dateFormat.parse(dateStr);
            if (date == null) {
                throw new ParseException("dateStr解析结果为 null", 0);
            }
            return new Date(date.getTime());
        } catch (ParseException e) {
            LogUtils.warn(e);
            return null;
        }
    }

    public static Date parseDate(String dateStr) {
        return parseDate(dateStr, "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 将指定的日期转换为一定格式的字符串
     */
    public static String formatDate(Date date, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.PRC);
        return sdf.format(date);
    }

    public static String formatCurrentDate(String format) {
        return formatDate(Calendar.getInstance(Locale.CHINA).getTime(), format);
    }

    /**
     * 格式化时间
     */
    public static long dateFormat(String time, String format) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        Date date;
        try {
            date = simpleDateFormat.parse(time);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return date == null ? 0 : date.getTime();
    }

    /**
     * 格式化时间
     */
    public static String dateFormat(long time, String format) {
        if (time == 0) {
            time = System.currentTimeMillis();
        }
        @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        return simpleDateFormat.format(time);
    }

    /**
     * 计算两个日期之间相差的时间戳数
     *
     * @param startDate startDate
     * @param endDate   endDate
     * @param mode      mode
     * @return 1d/1h/1m/1s
     */
    public static long getDayHourMinuteSecondByMode(Date startDate, Date endDate, @DifferenceMode int mode) {
        long[] array = getDayHourMinuteSecondArray(startDate, endDate);
        if (mode == Minute) {
            return array[2];
        } else if (mode == Hour) {
            return array[1];
        } else if (mode == Day) {
            return array[0];
        } else {
            return array[3];
        }
    }

    public static long getDayHourMinuteSecondByMode(long startTimeMillis, long endTimeMillis, @DifferenceMode int mode) {
        return getDayHourMinuteSecondByMode(new Date(startTimeMillis), new Date(endTimeMillis), mode);
    }

    private static long[] getDayHourMinuteSecondArray(Date startDate, Date endDate) {
        return getDayHourMinuteSecondArray(endDate.getTime() - startDate.getTime());
    }

    private static long[] getDayHourMinuteSecondArray(long milliSeconds) {

        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;

        long elapsedDays = milliSeconds / daysInMilli;
        milliSeconds = milliSeconds % daysInMilli;
        long elapsedHours = milliSeconds / hoursInMilli;
        milliSeconds = milliSeconds % hoursInMilli;
        long elapsedMinutes = milliSeconds / minutesInMilli;
        milliSeconds = milliSeconds % minutesInMilli;
        long elapsedSeconds = milliSeconds / secondsInMilli;
        return new long[]{elapsedDays, elapsedHours, elapsedMinutes, elapsedSeconds};
    }

}
