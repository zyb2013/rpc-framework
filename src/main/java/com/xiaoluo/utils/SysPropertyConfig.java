package com.xiaoluo.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Created by Administrator on 2015/9/11.
 */
public class SysPropertyConfig {
    private static Properties sysProperties=null;
    private static Map<String,Object> attributeMap=new HashMap<>();
    private static final Pattern INTEGER_PATTERN = Pattern.compile("-?[0-9]+");
    public static final void doConfig(Properties properties){
        sysProperties=properties;
    }
    public static final Properties getProperties(){
        return sysProperties;
    }
    public static final String get(String key){
        return sysProperties.getProperty(key);
    }
    public static boolean getBoolean(String key, boolean def) {
        String value = get(key);
        if (value == null) {
            return def;
        }

        value = value.trim().toLowerCase();
        if (value.isEmpty()) {
            return true;
        }

        if ("true".equals(value) || "yes".equals(value) || "1".equals(value)) {
            return true;
        }

        if ("false".equals(value) || "no".equals(value) || "0".equals(value)) {
            return false;
        }
        throw new IllegalArgumentException("Unable to parse the boolean  property '" + key + "':" + value + " - " +
                "using the default value: " + def);

    }
    public static int getInt(String key, int def) {
        String value = get(key);
        if (value == null) {
            return def;
        }

        value = value.trim().toLowerCase();
        if (INTEGER_PATTERN.matcher(value).matches()) {
            try {
                return Integer.parseInt(value);
            } catch (Exception e) {
                // Ignore
            }
        }
        throw new IllegalArgumentException(
                "Unable to parse the integer property '" + key + "':" + value + " - " +
                        "using the default value: " + def);
    }
    public static long getLong(String key, long def) {
        String value = get(key);
        if (value == null) {
            return def;
        }

        value = value.trim().toLowerCase();
        if (INTEGER_PATTERN.matcher(value).matches()) {
            try {
                return Long.parseLong(value);
            } catch (Exception e) {
                // Ignore
            }
        }
        throw new IllegalArgumentException(
                "Unable to parse the long integer system property '" + key + "':" + value + " - " +
                        "using the default value: " + def);
    }
    public static float getFloat(String key,float def){
        String value = get(key);
        if (value == null) {
            return def;
        }
        return Float.valueOf(value);
    }
    public static void setAttribute(String key,Object attribute){
        attributeMap.put(key,attribute);
    }
    public static Object getAttribute(String key){
        return attributeMap.get(key);
    }
    public static <T> T getAttribute(String key,Class<T> clazz){
        return (T)attributeMap.get(key);
    }
}
