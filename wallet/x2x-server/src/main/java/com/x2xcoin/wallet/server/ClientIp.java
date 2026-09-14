package com.x2xcoin.wallet.server;

import io.javalin.http.Context;

/**
 * Client IP behind nginx ({@code X-Real-IP}) with a conservative fallback.
 */
final class ClientIp {
    private ClientIp() {
    }

    static String of(Context ctx) {
        String realIp = firstHop(ctx.header("X-Real-IP"));
        if (realIp != null) {
            return realIp;
        }
        String forwarded = firstHop(ctx.header("X-Forwarded-For"));
        if (forwarded != null) {
            return forwarded;
        }
        String ip = ctx.ip();
        return ip == null || ip.isBlank() ? "unknown" : ip.trim();
    }

    private static String firstHop(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        String first = header.split(",")[0].trim();
        if (first.isEmpty() || first.indexOf(' ') >= 0 || first.length() > 64) {
            return null;
        }
        return first;
    }
}
