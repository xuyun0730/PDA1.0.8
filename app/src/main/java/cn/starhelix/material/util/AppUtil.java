package cn.starhelix.material.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

public class AppUtil {
    private static final String TAG = "AppUtil";

    public static String getVerName(Context context) {
        String verName = "";
        try {
            verName = context.getPackageManager().
                    getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "error on try to get version name");
        }
        return verName;
    }
}
