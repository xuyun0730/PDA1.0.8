package cn.starhelix.material.data;

import cn.starhelix.material.MaterialApplication;
import cn.starhelix.material.entity.AccountBasicInfo;
import cn.starhelix.material.util.PreferenceUtil;

public class CacheData {
    private static volatile CacheData instance;

    private String token;
    private AccountBasicInfo accountInfo;

    private CacheData() {

    }

    public static CacheData getInstance() {
        if (instance != null) {
            return instance;
        }

        synchronized (CacheData.class) {
            if (instance == null) {
                instance = new CacheData();
            }
        }

        return instance;
    }

    public String getToken() {
        if (token == null) {
            token = PreferenceUtil.getInstance().retrieveToken(MaterialApplication.getInstance());
        }

        return token;
    }

    public void setToken(String token) {
        instance.token = token;
    }

    public void clearToken() {
        instance.token = null;
    }

    public AccountBasicInfo getAccountInfo() {
        if (accountInfo == null) {
            accountInfo = PreferenceUtil.getInstance().retrieveAccountInfo(MaterialApplication.getInstance());
        }

        return accountInfo;
    }

    public void setAccountInfo(AccountBasicInfo accountInfo) {
        instance.accountInfo = accountInfo;
    }

    public void clearAccountInfo() {
        instance.accountInfo = null;
    }
}
