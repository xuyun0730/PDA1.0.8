package cn.starhelix.material;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.LinkedList;
import java.util.List;

import cn.starhelix.material.data.CacheData;
import cn.starhelix.material.util.PreferenceUtil;

public class MaterialApplication extends Application implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "MaterialApplication";

    private static MaterialApplication instance;
    private static List<Activity> activeActivityList = new LinkedList<>();

    public static MaterialApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        registerActivityLifecycleCallbacks(this);
    }

    public synchronized void clearLoginInfo() {
        PreferenceUtil.getInstance().removeToken(this);
        PreferenceUtil.getInstance().removeAccountInfo(this);
        CacheData.getInstance().clearToken();
        CacheData.getInstance().clearAccountInfo();
    }

    // 结合 launchMode="singleInstance" 确保只启动一个登陆activity
    public synchronized void redirectLogin() {
        for (Activity activity : activeActivityList) {
            if (activity instanceof LoginActivity) {
                Log.d(TAG, "find existing login activity, skipping...");
                return;
            }
        }

        clearActiveActivities();

        Log.d(TAG, "starting login activity...");
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public synchronized void clearActiveActivities() {
        Log.d(TAG, "finish active activity size: " + activeActivityList.size());

        // take care of ConcurrentModificationException
        for (int i = activeActivityList.size() - 1; i >= 0; i--) {
            Activity activity = activeActivityList.get(i);
            if (activity != null && !activity.isDestroyed()) {
                Log.w(TAG, "finishing: " + activity.getLocalClassName());
                activity.finish();
            }
        }

        activeActivityList.clear();
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
        Log.d(TAG, "onActivity created: " + activity.getLocalClassName());
        synchronized (this) {
            activeActivityList.add(activity);
        }
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {

    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {

    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {

    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        Log.d(TAG, "onActivity destroyed: " + activity.getLocalClassName());
        synchronized (this) {
            activeActivityList.remove(activity);
        }
    }
}
