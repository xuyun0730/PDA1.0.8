package cn.starhelix.material.util;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class StrUtil {
    private static final String TAG = "StrUtil";

    private static final Pattern INTEGER_PATTERN = Pattern.compile("[0-9]+");

    private static final Pattern NUMERIC_PATTERN = Pattern.compile("-?\\d+(\\.\\d+)?");

    public static boolean isEmpty(Object str) {
        if (str == null)
            return true;
        String tempStr = str.toString().trim();
        if (tempStr.length() == 0)
            return true;
        if (tempStr.equals("null"))
            return true;
        return false;
    }

    // 精确到毫秒
    public static String currentDateTimeMsStr() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    /**
     * 判断是否是正整数的方法
     */
    public static boolean isInteger(String string) {
        return INTEGER_PATTERN.matcher(string).matches();
    }

    /**
     * 简单判断手机号
     */
    public static boolean isChinesePhoneNumber(String string) {
        return !isEmpty(string) && string.length() == 11 && isInteger(string);
    }

    public static boolean isNumeric(String string) {
        return NUMERIC_PATTERN.matcher(string).matches();
    }

    // mm2
    public static SpannableString mmSquareString() {
        SpannableString mmSquare = new SpannableString("mm2");
        // 一半大小
        mmSquare.setSpan(new RelativeSizeSpan(0.5f), 2, 3, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        // 设置为上标
        mmSquare.setSpan(new SuperscriptSpan(), 2, 3, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        return mmSquare;
    }

    // cm2
    public static SpannableString cmSquareString() {
        SpannableString mmSquare = new SpannableString("cm2");
        // 一半大小
        mmSquare.setSpan(new RelativeSizeSpan(0.5f), 2, 3, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        // 设置为上标
        mmSquare.setSpan(new SuperscriptSpan(), 2, 3, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        return mmSquare;
    }

    // 如果有相同的key，value直接连接在一起
    public static Map<String, String> parseUrlQueryParams(String urlStr) throws MalformedURLException {
        URL url = new URL(urlStr);
        final Map<String, String> queryPairs = new LinkedHashMap<>();
        final String[] pairs = url.getQuery().split("&");
        for (String pair : pairs) {
            final int idx = pair.indexOf("=");
            final String key;
            String keyRaw = "";
            try {
                if (idx > 0) {
                    keyRaw = pair.substring(0, idx);
                } else {
                    keyRaw = pair;
                }
                key = URLDecoder.decode(keyRaw, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "无效的key，无法通过utf8解码:" + keyRaw);
                continue;
            }

            String tmpVal = "";
            if (queryPairs.containsKey(key)) {
                tmpVal = queryPairs.get(key);
            }
            final String value;
            String valRaw = "";
            try {
                if (idx > 0 && pair.length() > idx + 1) {
                    valRaw = pair.substring(idx + 1);
                    value = URLDecoder.decode(valRaw, "UTF-8").trim();
                } else {
                    value = "";
                }
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "无效的value，无法通过utf8解码: " + valRaw);
                continue;
            }

            queryPairs.put(key, tmpVal + value);
        }
        return queryPairs;
    }
}
