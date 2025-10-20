package org.jngcoding.chat.app;

import java.util.HashMap;

public class Utility {
    public static String getValue(String tuple, String key) {
        HashMap<String, String> map = new HashMap<>();

        String[] pairs = tuple.split("--,");

        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                map.put(kv[0], kv[1]);
            }
        }

        return map.getOrDefault(key, null);
    }

    public static String getValue(String tuple, String key, String delimiter) {
        HashMap<String, String> map = new HashMap<>();

        String[] pairs = tuple.split(delimiter);

        for (String pair : pairs) {
            String[] kv = pair.trim().split(":", 2);
            if (kv.length == 2) {
                map.put(kv[0], kv[1]);
            }
        }

        return map.getOrDefault(key, null);
    }
}
