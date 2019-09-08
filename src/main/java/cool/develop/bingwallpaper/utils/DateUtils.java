package cool.develop.bingwallpaper.utils;

import cool.develop.bingwallpaper.model.dto.CountryCode;
import cool.develop.bingwallpaper.model.dto.Images;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Date;
import java.util.Locale;

/**
 * 日期时间相关工具类
 *
 * @author vpday
 * @create 2019/6/28
 */
public final class DateUtils {

    private static final DateTimeFormatterBuilder BUILDER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive().parseLenient()
            .appendPattern("[MMM. dd, yyyy]")
            .appendPattern("[MMM dd, yyyy]")
            .appendPattern("[MM dd, yyyy]")
            .appendPattern("[yyyyMMdd]");

    /**
     * epochMilli to LocalDateTime
     */
    public static LocalDateTime epochMilliToLocalDateTime(long epochMilli) {
        Instant instant = Instant.ofEpochMilli(epochMilli);
        return instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * epochMilli to LocalDate
     */
    public static LocalDate epochMilliToLocalDate(long epochMilli) {
        Instant instant = Instant.ofEpochMilli(epochMilli);
        return instant.atZone(ZoneId.systemDefault()).toLocalDateTime().toLocalDate();
    }

    /**
     * epochMilli to Date
     */
    public static Date epochMilliToDate(long epochMilli) {
        return Date.from(Instant.ofEpochMilli(epochMilli));
    }

    /**
     * 解析日期
     */
    public static Long parseDate(CountryCode countryCode, Images images) {
        LocalDate toParse;

        if (countryCode.equals(CountryCode.ZH_CN)) {
            toParse = LocalDate.parse(images.getEndDate(), BUILDER.toFormatter(Locale.CHINA));
        } else if (countryCode.equals(CountryCode.JA_JP)) {
            toParse = LocalDate.parse(images.getDate(), BUILDER.toFormatter(Locale.JAPAN));
        } else if (countryCode.equals(CountryCode.FR_FR)) {
            toParse = LocalDate.parse(images.getDate(), BUILDER.toFormatter(Locale.FRANCE));
        } else if (countryCode.equals(CountryCode.DE_DE)) {
            toParse = LocalDate.parse(images.getEndDate(), BUILDER.toFormatter(Locale.GERMANY));
        } else {
            toParse = LocalDate.parse(images.getDate(), BUILDER.toFormatter(Locale.ENGLISH));
        }
        return toParse.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}