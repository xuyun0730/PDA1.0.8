package cn.starhelix.material.util;

import android.content.Context;
import android.content.SharedPreferences;

import cn.starhelix.material.entity.AccountBasicInfo;

public class PreferenceUtil {
    private static final String SP_NAME = "SP_CN_STAR_HELIX_PDA_MATERIAL";

    private static final String TOKEN = "TOKEN";
    private static final String ACCOUNT_BASIC_INFO = "ACCOUNT_BASIC_INFO";
    private static final String SERVER_HOST = "SERVER_HOST";
    private static final String SERVER_PORT = "SERVER_PORT";
    private static final String WORK_SERIAL_PREFIX = "WORK_SERIAL_";

    private static volatile PreferenceUtil instance;

    private PreferenceUtil() {
    }

    public static PreferenceUtil getInstance() {
        if (instance != null) {
            return instance;
        }

        synchronized (PreferenceUtil.class) {
            if (instance == null) {
                instance = new PreferenceUtil();
            }
        }

        return instance;
    }

    private SharedPreferences getSp(Context context) {
        return context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
    }

    public String retrieveToken(Context context) {
        if (context == null) return null;
        SharedPreferences sharedPreferences = getSp(context);
        return sharedPreferences.getString(TOKEN, null);
    }

    public void storeToken(Context context, String token) {
        if (context == null) return;
        SharedPreferences sharedPreferences = getSp(context);
        SharedPreferences.Editor spEditor = sharedPreferences.edit();
        spEditor.putString(TOKEN, token);
        spEditor.apply();
    }

    public void removeToken(Context context) {
        if (context == null) {
            return;
        }

        SharedPreferences sharedPreferences = getSp(context);
        SharedPreferences.Editor spEditor = sharedPreferences.edit();
        spEditor.remove(TOKEN);
        spEditor.apply();
    }

    public AccountBasicInfo retrieveAccountInfo(Context context) {
        if (context == null) return null;
        SharedPreferences sharedPreferences = getSp(context);
        String infoStr = sharedPreferences.getString(ACCOUNT_BASIC_INFO, null);
        if (!StrUtil.isEmpty(infoStr)) {
            return ConvertUtil.fromJson(infoStr, AccountBasicInfo.class);
        } else {
            return null;
        }
    }

    public void storeAccountInfo(Context context, AccountBasicInfo accountInfo) {
        if (context == null) return;
        String st;
        if (accountInfo == null) {
            st = "{}";
        } else {
            st = ConvertUtil.toJson(accountInfo);
        }
        SharedPreferences sharedPreferences = getSp(context);
        SharedPreferences.Editor spEditor = sharedPreferences.edit();
        spEditor.putString(ACCOUNT_BASIC_INFO, st);
        spEditor.apply();
    }

    public void removeAccountInfo(Context context) {
        if (context == null) {
            return;
        }

        SharedPreferences sharedPreferences = getSp(context);
        SharedPreferences.Editor spEditor = sharedPreferences.edit();
        spEditor.remove(ACCOUNT_BASIC_INFO);
        spEditor.apply();
    }

    public String retrieveServerHost(Context context) {
        if (context == null) return null;
        SharedPreferences sharedPreferences = getSp(context);
        return sharedPreferences.getString(SERVER_HOST, null);
    }

    public void storeServerHost(Context context, String host) {
        if (context == null) return;
        SharedPreferences sharedPreferences = getSp(context);
        SharedPreferences.Editor spEditor = sharedPreferences.edit();
        spEditor.putString(SERVER_HOST, host);
        spEditor.apply();
    }

    public int retrieveServerPort(Context context, int defaultPort) {
        if (context == null) return defaultPort;
        SharedPreferences sharedPreferences = getSp(context);
        return sharedPreferences.getInt(SERVER_PORT, defaultPort);
    }

    public void storeServerPort(Context context, int port) {
        if (context == null) return;
        SharedPreferences sharedPreferences = getSp(context);
        SharedPreferences.Editor spEditor = sharedPreferences.edit();
        spEditor.putInt(SERVER_PORT, port);
        spEditor.apply();
    }

    public int retrieveWorkSerialNumber(Context context, String key, int defaultValue) {
        if (context == null || StrUtil.isEmpty(key)) {
            return defaultValue;
        }
        SharedPreferences sharedPreferences = getSp(context);
        return sharedPreferences.getInt(WORK_SERIAL_PREFIX + key, defaultValue);
    }

    public void storeWorkSerialNumber(Context context, String key, int serialNumber) {
        if (context == null || StrUtil.isEmpty(key)) {
            return;
        }
        SharedPreferences sharedPreferences = getSp(context);
        SharedPreferences.Editor spEditor = sharedPreferences.edit();
        spEditor.putInt(WORK_SERIAL_PREFIX + key, serialNumber);
        spEditor.apply();
    }

    public void removeWorkSerialNumber(Context context, String key) {
        if (context == null || StrUtil.isEmpty(key)) {
            return;
        }
        SharedPreferences sharedPreferences = getSp(context);
        SharedPreferences.Editor spEditor = sharedPreferences.edit();
        spEditor.remove(WORK_SERIAL_PREFIX + key);
        spEditor.apply();
    }
}
