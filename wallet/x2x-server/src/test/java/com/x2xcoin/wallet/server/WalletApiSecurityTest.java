package com.x2xcoin.wallet.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WalletApiSecurityTest {
    @Test
    void broadcastRejectsInvalidHexAndSetsSecurityHeaders() throws Exception {
        io.javalin.Javalin app = JavalinSupport.createApp();
        HttpSecurity.install(app);
        ServerSupport.configureErrors(app);
        app.post("/api/tx/broadcast", ctx -> {
            String hex = TxBroadcast.requireRawHex(X2xServer.readRawTx(ctx.body()));
            JsonResponses.write(ctx, Map.of("hex", hex));
        });
        app.get("/api/health", ctx -> JsonResponses.write(ctx, Map.of("api", "ok")));
        app.error(404, JsonResponses::notFound);
        app.start("127.0.0.1", 0);
        try {
            int port = app.port();
            HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

            HttpResponse<String> missing = post(http, port, "/api/tx/broadcast", "{}");
            assertEquals(400, missing.statusCode(), missing.body());
            assertEquals("invalid transaction", error(missing));

            HttpResponse<String> badHex = post(http, port, "/api/tx/broadcast", "{\"rawTx\":\"xyz\"}");
            assertEquals(400, badHex.statusCode(), badHex.body());
            assertEquals("invalid transaction", error(badHex));

            HttpResponse<String> ok = post(http, port, "/api/tx/broadcast", "{\"rawTx\":\"00ff\"}");
            assertEquals(200, ok.statusCode(), ok.body());
            assertEquals("00ff", JsonParser.parseString(ok.body()).getAsJsonObject().get("hex").getAsString());

            HttpResponse<String> health = get(http, port, "/api/health");
            assertEquals(200, health.statusCode(), health.body());
            assertEquals("nosniff", health.headers().firstValue("X-Content-Type-Options").orElse(""));
            assertEquals("DENY", health.headers().firstValue("X-Frame-Options").orElse(""));
            assertTrue(health.headers().firstValue("Cache-Control").orElse("").contains("no-store"));
        } finally {
            app.stop();
        }
    }

    private static HttpResponse<String> post(HttpClient http, int port, String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> get(HttpClient http, int port, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static String error(HttpResponse<String> response) {
        JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
        return body.get("error").getAsString();
    }
}
