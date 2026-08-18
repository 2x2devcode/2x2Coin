package com.x2xcoin.wallet.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WalletApiSmokeTest {
    @TempDir
    Path indexDir;

    @Test
    void servesHealthStatusFeeAndBalanceFromMockDaemon() throws Exception {
        System.setProperty("x2x.index.dir", indexDir.toString());
        try (MockRpcServer mock = new MockRpcServer("127.0.0.1", 0, "x2xrpc", "secret")) {
            mock.start();
            RpcClient rpcClient = new RpcClient(RpcClient.mockCliCommand(), mock.host(), mock.port(), "x2xrpc", "secret");
            AddressQueryService addressQuery = new AddressQueryService(
                    rpcClient,
                    ChainIndexer.open(),
                    new OfficialExplorerClient("http://127.0.0.1:1", false)
            );
            io.javalin.Javalin app = JavalinSupport.createApp();
            app.get("/api/health", ctx -> {
                rpcClient.call("getblockcount", new com.google.gson.JsonArray());
                JsonObject ok = new JsonObject();
                ok.addProperty("api", "ok");
                ok.addProperty("rpc", "ok");
                JsonResponses.write(ctx, ok);
            });
            app.get("/api/status", ServerSupport.rpc(() -> {
                JsonObject info = RpcNodeInfo.getInfo(rpcClient);
                JsonObject out = new JsonObject();
                out.addProperty("online", true);
                out.addProperty("chain", "main");
                out.addProperty("blocks", RpcNodeInfo.blocks(info));
                out.addProperty("headers", RpcNodeInfo.headers(info));
                out.addProperty("progress", 100.0);
                out.addProperty("peers", RpcNodeInfo.connections(info));
                return out;
            }));
            app.get("/api/fee", ctx -> {
                JsonObject out = new JsonObject();
                out.addProperty("feePerKbSatoshis", 10_000L);
                JsonResponses.write(ctx, out);
            });
            app.get("/api/address/{address}/balance", ctx -> JsonResponses.write(ctx, addressQuery.balance(ctx.pathParam("address"))));
            app.start("127.0.0.1", 0);
            try {
                int port = app.port();
                HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
                JsonObject health = get(http, port, "/api/health");
                assertEquals("ok", health.get("api").getAsString());
                assertEquals("ok", health.get("rpc").getAsString());
                JsonObject status = get(http, port, "/api/status");
                assertTrue(status.get("online").getAsBoolean());
                assertEquals(1, status.get("blocks").getAsInt());
                JsonObject fee = get(http, port, "/api/fee");
                assertEquals(10_000L, fee.get("feePerKbSatoshis").getAsLong());
                JsonObject balance = get(http, port, "/api/address/2NHBXKyRY4ZBvyfyuZ2fZvqaGyo89vMGFW/balance");
                assertEquals("2NHBXKyRY4ZBvyfyuZ2fZvqaGyo89vMGFW", balance.get("address").getAsString());
                assertTrue(balance.has("balance"));
            } finally {
                app.stop();
            }
        }
    }

    private static JsonObject get(HttpClient http, int port, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), response.body());
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }
}
