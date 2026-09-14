package com.x2xcoin.wallet.server;

import io.javalin.Javalin;
import io.javalin.http.Context;

/**
 * Shared request filters for the public JSON API and explorer.
 */
final class HttpSecurity {
    private static final RequestRateLimiter RATE_LIMITER = RequestRateLimiter.fromEnvironment();

    private HttpSecurity() {
    }

    static void install(Javalin app) {
        app.before(HttpSecurity::before);
    }

    static void before(Context ctx) {
        JavalinSupport.applySecurityHeaders(ctx);
        RATE_LIMITER.maybePrune();
        String method = ctx.method().name();
        String path = ctx.path();
        if (!RATE_LIMITER.allow(ClientIp.of(ctx), method, path)) {
            throw new ClientRequestException(429, "too many requests");
        }
    }
}
