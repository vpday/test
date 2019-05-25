package cool.develop.bingwallpaper.utils;

import com.blade.kit.NamedThreadFactory;
import com.blade.kit.StringKit;
import com.blade.mvc.RouteContext;
import com.sun.syndication.feed.rss.Channel;
import com.sun.syndication.feed.rss.Content;
import com.sun.syndication.feed.rss.Item;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.WireFeedOutput;
import cool.develop.bingwallpaper.bootstrap.BingWallpaperConst;
import cool.develop.bingwallpaper.exception.TipException;
import cool.develop.bingwallpaper.extension.Site;
import cool.develop.bingwallpaper.model.dto.CountryCode;
import cool.develop.bingwallpaper.model.dto.Images;
import cool.develop.bingwallpaper.model.entity.BingWallpaper;
import io.github.biezhi.request.Request;
import lombok.extern.slf4j.Slf4j;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 工具类
 *
 * @author vpday
 * @create 2018/11/23
 */
@Slf4j
public final class SiteUtils {

    private static final DateTimeFormatterBuilder BUILDER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive().parseLenient()
            .appendPattern("[MMM. dd, yyyy]")
            .appendPattern("[MMM dd, yyyy]")
            .appendPattern("[MM dd, yyyy]")
            .appendPattern("[yyyyMMdd]");


    public static String buildImageArchiveUrl(Integer index, Integer number, String mkt) {
        return BingWallpaperConst.IMAGE_ARCHIVE + "?" +
                "format=js&pid=hp&video=1&setlang=en-us&" +
                "idx=" + index + "&" +
                "n=" + number + "&" +
                "mkt=" + mkt;
    }

    public static String requestBing(String url) {
        Request request = Request.get(url).acceptJson()
                .userAgent(BingWallpaperConst.USER_AGENT);

        checkStatus(request);
        return request.body();
    }

    public static byte[] downLoadWallpaper(String url) {
        Request request = Request.get(url).userAgent(BingWallpaperConst.USER_AGENT);
        checkStatus(request);

        return request.bytes();
    }

    private static void checkStatus(Request request) {
        if (!request.ok()) {
            String message = String.format("请求失败，状态码：%s，URL：%s", request.code(), request.url().toString());
            log.error(message);
            throw new TipException(message);
        }
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

    /**
     * 构建线程池
     */
    public static ExecutorService newFixedThreadPool(int corePoolSize, String threadPrefix) {
        return new ThreadPoolExecutor(corePoolSize, (corePoolSize / 2 + corePoolSize),
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingDeque<>(1024),
                new NamedThreadFactory(threadPrefix),
                new ThreadPoolExecutor.AbortPolicy());
    }

    /**
     * 构建 RSS 订阅的 XML 内容
     */
    public static String getRssXml(List<BingWallpaper> wallpapers, CountryCode country) throws FeedException {
        Channel channel = new Channel("rss_2.0");
        channel.setTitle(BingWallpaperConst.HEAD_TITLE);
        channel.setLink(BingWallpaperConst.SITE_URL);
        channel.setDescription(BingWallpaperConst.META_DESCRIPTION);
        channel.setLanguage(country.code());
        channel.setCopyright(BingWallpaperConst.META_AUTHOR);

        List<Item> items = new ArrayList<>();
        wallpapers.forEach(wallpaper -> {
            Item item = new Item();
            item.setTitle(wallpaper.getCopyright());
            item.setContent(buildContent(wallpaper));
            item.setLink(BingWallpaperConst.SITE_URL + Site.detailsHref(wallpaper));
            item.setPubDate(Date.from(Instant.ofEpochMilli(wallpaper.getDate())));
            items.add(item);
        });
        channel.setItems(items);

        WireFeedOutput out = new WireFeedOutput();
        return out.outputString(channel);
    }

    private static Content buildContent(BingWallpaper wallpaper) {
        Content content = new Content();
        String url = BingWallpaperConst.SITE_URL + Site.imgHref(wallpaper, "1920x1080");

        StringBuilder contentStr = new StringBuilder();
        contentStr.append("<img src=\"").append(url).append("\" border=\"0\"/><h2>");
        if (StringKit.isNotEmpty(wallpaper.getTitle())) {
            contentStr.append(wallpaper.getTitle());
        }
        if (StringKit.isNotEmpty(wallpaper.getCaption())) {
            contentStr.append(" —— ").append(wallpaper.getCaption());
        }
        contentStr.append("</h2><h4>");
        if (StringKit.isNotEmpty(wallpaper.getDescription())) {
            contentStr.append(wallpaper.getDescription());
        }
        contentStr.append("</h4>");

        content.setValue(contentStr.toString());
        return content;
    }

    public static String getStackTrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    public static Locale acceptLanguage(RouteContext context) {
        Locale locale = Locale.CHINA;

        String languages = context.header("Accept-Language");
        if (StringKit.isNotEmpty(languages)) {
            Optional<Locale> useLocal = Locale.LanguageRange.parse(languages).stream()
                    .sorted(Comparator.comparing(Locale.LanguageRange::getWeight).reversed())
                    .map(range -> new Locale(range.getRange())).findFirst();
            locale = useLocal.orElse(Locale.CHINA);
        }

        return locale;
    }
}
