package com.x2xcoin.wallet.server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.x2xcoin.wallet.core.chain.NetworkParameters;

import java.io.IOException;
import java.util.List;
import java.util.Map;

final class X2xApiService {
    private final RpcClient rpcClient;
    private final AddressQueryService addressQuery;

    X2xApiService(RpcClient rpcClient, AddressQueryService addressQuery) {
        this.rpcClient = rpcClient;
        this.addressQuery = addressQuery;
    }

    JsonObject status() throws IOException {
        JsonObject info = RpcNodeInfo.getInfo(rpcClient);
        JsonObject out = new JsonObject();
        out.addProperty("online", true);
        out.addProperty("chain", "main");
        out.addProperty("blocks", RpcNodeInfo.blocks(info));
        out.addProperty("headers", RpcNodeInfo.headers(info));
        out.addProperty("progress", 100.0);
        out.addProperty("peers", RpcNodeInfo.connections(info));
        return out;
    }

    JsonObject balance(String address) throws IOException {
        return addressQuery.balance(address);
    }

    JsonObject utxos(String address) throws IOException {
        return addressQuery.utxos(address);
    }

    JsonObject fee() {
        JsonObject out = new JsonObject();
        out.addProperty("feePerKbSatoshis", NetworkParameters.DEFAULT_FEE_PER_KB);
        return out;
    }

    JsonObject broadcast(String rawTx) throws IOException {
        JsonArray params = new JsonArray();
        params.add(TxBroadcast.requireRawHex(rawTx));
        String txid = rpcClient.call("sendrawtransaction", params).getAsString();
        JsonObject out = new JsonObject();
        out.addProperty("txid", txid);
        return out;
    }

    void invalidate(String address) {
        addressQuery.invalidate(address);
    }
}

/**
 * JSON-only official API for the wallet app (no web UI).
 * Endpoints under /api/*
 */
public final class X2xServer {
    public static void main(String[] args) throws IOException {
        RpcClient rpcClient = RpcClientFactory.fromEnvironment();
        AddressQueryService addressQuery = AddressQueryService.create(rpcClient);
        X2xApiService service = new X2xApiService(rpcClient, addressQuery);
        int listenPort = Integer.parseInt(env("PORT", String.valueOf(NetworkParameters.OFFICIAL_API_PORT)));
        String bindHost = env("BIND_HOST", NetworkParameters.SERVER_BIND_HOST);

        io.javalin.Javalin app = JavalinSupport.createApp();
        HttpSecurity.install(app);
        ServerSupport.configureErrors(app);
        app.get("/api/health", ctx -> {
            try {
                rpcClient.call("getblockcount", new JsonArray());
                JsonObject ok = new JsonObject();
                ok.addProperty("api", "ok");
                ok.addProperty("rpc", "ok");
                JsonResponses.write(ctx, ok);
            } catch (IOException error) {
                JsonResponses.upstreamError(ctx);
            }
        });
        app.get("/api/status", ServerSupport.rpc(service::status));
        app.get("/api/fee", ServerSupport.rpc(service::fee));
        app.get("/api/address/{address}/balance", ctx -> {
            long started = System.nanoTime();
            JsonObject body = service.balance(ctx.pathParam("address"));
            long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
            if (elapsedMs > 2_000L) {
                System.out.println("[x2x-api] slow balance " + ctx.pathParam("address") + " in " + elapsedMs + "ms");
            }
            JsonResponses.write(ctx, body);
        });
        app.get("/api/address/{address}/utxos", ctx -> JsonResponses.write(ctx, service.utxos(ctx.pathParam("address"))));
        app.get("/api/address/{address}/txs", ctx -> JsonResponses.write(ctx, Map.of("transactions", List.of())));
        app.post("/api/tx/broadcast", ctx -> JsonResponses.write(ctx, service.broadcast(readRawTx(ctx.body()))));
        app.post("/api/cache/invalidate/{address}", ctx -> {
            service.invalidate(ctx.pathParam("address"));
            JsonResponses.write(ctx, Map.of("ok", true));
        });
        app.error(404, JsonResponses::notFound);
        app.start(bindHost, listenPort);
    }

    static String readRawTx(String body) {
        JsonObject json;
        try {
            json = new Gson().fromJson(body, JsonObject.class);
        } catch (RuntimeException e) {
            throw new ClientRequestException("invalid transaction");
        }
        if (json == null || !json.has("rawTx") || json.get("rawTx").isJsonNull() || !json.get("rawTx").isJsonPrimitive()) {
            throw new ClientRequestException("invalid transaction");
        }
        JsonPrimitive primitive = json.get("rawTx").getAsJsonPrimitive();
        if (!primitive.isString()) {
            throw new ClientRequestException("invalid transaction");
        }
        return primitive.getAsString();
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
