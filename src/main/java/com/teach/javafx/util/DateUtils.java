package com.teach.javafx.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class DateUtils {

    /**
     * 计算两个日期之间的天数
     * @param startDateStr 开始日期字符串 (yyyy-MM-dd)
     * @param endDateStr 结束日期字符串 (yyyy-MM-dd)
     * @return 两个日期之间的天数，包括开始和结束日期
     */
    public static int calculateDaysBetween(String startDateStr, String endDateStr) {
        if (startDateStr == null || endDateStr == null ||
            startDateStr.isEmpty() || endDateStr.isEmpty()) {
            return 0;
        }

        try {
            LocalDate startDate = LocalDate.parse(startDateStr);
            LocalDate endDate = LocalDate.parse(endDateStr);

            // 计算天数差，加1是因为要包括开始和结束日期
            return (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        } catch (Exception e) {
            return 0;
        }
    }
}