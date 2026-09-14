package com.x2xcoin.wallet.server;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpResponseException;

import java.io.IOException;

final class ServerSupport {
    private ServerSupport() {
    }

    static void configureErrors(Javalin app) {
        app.exception(ClientRequestException.class, (exception, ctx) -> {
            JsonResponses.error(ctx, exception.status(), exception.getMessage());
        });
        app.exception(HttpResponseException.class, (exception, ctx) -> {
            int status = exception.getStatus();
            System.err.println("[x2x-server] " + requestLabel(ctx) + " -> HTTP " + status);
            if (status == 429) {
                JsonResponses.rateLimited(ctx);
            } else if (status == 413) {
                JsonResponses.error(ctx, 413, "invalid request");
            } else if (status >= 400 && status < 500) {
                JsonResponses.error(ctx, status, "invalid request");
            } else {
                JsonResponses.upstreamError(ctx);
            }
        });
        app.exception(Exception.class, (exception, ctx) -> {
            int status = exception instanceof IOException ? 502 : 500;
            System.err.println("[x2x-server] " + requestLabel(ctx) + " -> " + exception.getMessage());
            exception.printStackTrace(System.err);
            if (status == 502) {
                JsonResponses.upstreamError(ctx);
            } else {
                JsonResponses.error(ctx, 500, "internal error");
            }
        });
    }

    static Handler rpc(RpcAction action) {
        return ctx -> JsonResponses.write(ctx, action.run());
    }

    @FunctionalInterface
    interface RpcAction {
        Object run() throws IOException;
    }

    private static String requestLabel(Context ctx) {
        return ctx.method() + " " + ctx.path();
    }
}
