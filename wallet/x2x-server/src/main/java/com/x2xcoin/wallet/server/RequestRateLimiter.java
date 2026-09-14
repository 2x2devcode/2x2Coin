package com.x2xcoin.wallet.server;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-key fixed window limiter for public JSON endpoints.
 */
final class RequestRateLimiter {
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final long windowMs;

    RequestRateLimiter(long windowMs) {
        this.windowMs = windowMs;
    }

    static RequestRateLimiter fromEnvironment() {
        long windowMs = readLong("API_RATE_WINDOW_MS", 60_000L);
        return new RequestRateLimiter(Math.max(1_000L, windowMs));
    }

    boolean allow(String ip, String method, String path) {
        return allow(bucketKey(ip, method, path), limitFor(method, path));
    }

    boolean allow(String key, int limit) {
        if (limit <= 0) {
            return true;
        }
        long now = System.currentTimeMillis();
        Window window = windows.computeIfAbsent(key, ignored -> new Window(now));
        synchronized (window) {
            if (now - window.startMs >= windowMs) {
                window.startMs = now;
                window.count = 0;
            }
            if (window.count >= limit) {
                return false;
            }
            window.count++;
            return true;
        }
    }

    void maybePrune() {
        if (windows.size() < 8_000) {
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Window>> iterator = windows.entrySet().iterator();
        while (iterator.hasNext()) {
            Window window = iterator.next().getValue();
            synchronized (window) {
                if (now - window.startMs >= windowMs * 2L) {
                    iterator.remove();
                }
            }
        }
    }

    int limitFor(String method, String path) {
        if ("POST".equals(method) && path.startsWith("/api/tx/broadcast")) {
            return readInt("API_BROADCAST_RATE_LIMIT", 12);
        }
        if (path.contains("/address/") || path.contains("/getaddress/")) {
            return readInt("API_ADDRESS_RATE_LIMIT", 40);
        }
        return readInt("API_RATE_LIMIT", 120);
    }

    static String bucketKey(String ip, String method, String path) {
        return ip + "|" + method + "|" + routeClass(path);
    }

    static String routeClass(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        if (path.startsWith("/api/tx/broadcast")) {
            return "/api/tx/broadcast";
        }
        if (path.contains("/getaddress/")) {
            return "/ext/getaddress";
        }
        if (path.contains("/address/")) {
            return "/api/address";
        }
        return path;
    }

    private static int readInt(String key, int fallback) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static long readLong(String key, long fallback) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static final class Window {
        long startMs;
        int count;

        Window(long startMs) {
            this.startMs = startMs;
            this.count = 0;
        }
    }
}
