package com.x2xcoin.wallet.server;

import io.javalin.Javalin;
import io.javalin.http.Context;

final class JavalinSupport {
    static final long MAX_REQUEST_SIZE = 64 * 1024L;

    private JavalinSupport() {
    }

    static Javalin createApp() {
        return Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.jsonMapper(new io.javalin.json.JavalinGson());
            config.http.maxRequestSize = MAX_REQUEST_SIZE;
        });
    }

    static void applySecurityHeaders(Context ctx) {
        ctx.header("X-Content-Type-Options", "nosniff");
        ctx.header("X-Frame-Options", "DENY");
        ctx.header("Referrer-Policy", "no-referrer");
        ctx.header("Cache-Control", "no-store");
    }
}
