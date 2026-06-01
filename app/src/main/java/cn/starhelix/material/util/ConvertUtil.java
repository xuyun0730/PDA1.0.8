package cn.starhelix.material.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;

public class ConvertUtil {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    public static final Type MAP_STR_OBJ_TYPE = new TypeToken<Map<String, Object>>() {
    }.getType();

    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> classOfT) {
        // JsonSyntaxException
        return GSON.fromJson(json, classOfT);
    }

    public static <T> T fromJson(String json, Type type) {
        // JsonSyntaxException
        return GSON.fromJson(json, type);
    }
}
