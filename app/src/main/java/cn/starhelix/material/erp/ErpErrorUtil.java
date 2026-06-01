package cn.starhelix.material.erp;

import cn.starhelix.material.entity.ApiException;
import cn.starhelix.material.util.StrUtil;

public final class ErpErrorUtil {
    private ErpErrorUtil() {
    }

    public static boolean isApiException(Throwable error) {
        return error instanceof ApiException;
    }

    public static String resolveMessage(Throwable error, String fallback) {
        if (error instanceof ApiException) {
            return error.getMessage();
        }

        if (error != null && !StrUtil.isEmpty(error.getMessage())) {
            return error.getMessage();
        }

        return fallback;
    }
}
