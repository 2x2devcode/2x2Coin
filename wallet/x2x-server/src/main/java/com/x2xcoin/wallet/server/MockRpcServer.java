package com.x2xcoin.wallet.server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Minimal JSON-RPC stand-in for 2x2coind. Used to compile/run and smoke-test
 * the wallet API on Ubuntu when the full daemon is not available.
 */
public final class MockRpcServer implements AutoCloseable {
    private static final Gson GSON = new Gson();
    private final HttpServer httpServer;
    private final String expectedAuth;

    public MockRpcServer(String host, int port, String user, String password) throws IOException {
        this.expectedAuth = "Basic " + Base64.getEncoder()
                .encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
        this.httpServer = HttpServer.create(new InetSocketAddress(host, port), 0);
        this.httpServer.createContext("/", this::handle);
        this.httpServer.setExecutor(null);
    }

    public void start() {
        httpServer.start();
    }

    public int port() {
        return httpServer.getAddress().getPort();
    }

    public String host() {
        return httpServer.getAddress().getHostString();
    }

    @Override
    public void close() {
        httpServer.stop(0);
    }

    public static void main(String[] args) throws IOException {
        String host = env("X2X_RPC_HOST", "127.0.0.1");
        int port = Integer.parseInt(env("X2X_RPC_PORT", "18589"));
        String user = env("X2X_RPC_USER", "x2xrpc");
        String password = env("X2X_RPC_PASSWORD", "x2xrpc");
        MockRpcServer server = new MockRpcServer(host, port, user, password);
        server.start();
        System.out.println("[x2x-mock-rpc] listening on http://" + host + ":" + server.port() + " user=" + user);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void handle(HttpExchange exchange) throws IOException {
        if (!expectedAuth.equals(header(exchange, "Authorization"))) {
            send(exchange, 401, "Unauthorized");
            return;
        }
        String body = new String(readAll(exchange.getRequestBody()), StandardCharsets.UTF_8);
        JsonObject request = GSON.fromJson(body, JsonObject.class);
        String method = request.get("method").getAsString();
        JsonElement id = request.get("id");
        JsonObject response = new JsonObject();
        response.add("id", id);
        response.add("error", null);
        response.add("result", resultFor(method, request.getAsJsonArray("params")));
        send(exchange, 200, GSON.toJson(response));
    }

    private static JsonElement resultFor(String method, JsonArray params) {
        return switch (method) {
            case "getblockcount" -> GSON.toJsonTree(1);
            case "getinfo" -> info();
            case "getblockhash" -> GSON.toJsonTree("00000eea834a06692bc4f56d6f0061631c72fd75431ce9e5d7f3b9d712dc3a9b");
            case "getblock", "getblockbynumber" -> genesisBlock();
            case "getrawtransaction" -> genesisTx();
            case "sendrawtransaction" -> GSON.toJsonTree(
                    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            );
            default -> {
                JsonObject error = new JsonObject();
                error.addProperty("code", -32601);
                error.addProperty("message", "Method not found: " + method);
                yield error;
            }
        };
    }

    private static JsonObject info() {
        JsonObject info = new JsonObject();
        info.addProperty("version", 1000000);
        info.addProperty("protocolversion", 70015);
        info.addProperty("blocks", 1);
        info.addProperty("headers", 1);
        info.addProperty("connections", 8);
        info.addProperty("testnet", false);
        info.addProperty("moneysupply", "11000000.00000000");
        return info;
    }

    private static JsonObject genesisBlock() {
        JsonObject block = new JsonObject();
        block.addProperty("hash", "00000eea834a06692bc4f56d6f0061631c72fd75431ce9e5d7f3b9d712dc3a9b");
        block.addProperty("height", 0);
        JsonArray txs = new JsonArray();
        txs.add(genesisTx());
        block.add("tx", txs);
        return block;
    }

    private static JsonObject genesisTx() {
        JsonObject tx = new JsonObject();
        tx.addProperty("txid", "8ec923e8d644631a549ddbd51cd350f597e29ca665979f01a501456bbc77f16b");
        tx.addProperty("blockheight", 0);
        JsonArray vin = new JsonArray();
        JsonObject coinbase = new JsonObject();
        coinbase.addProperty("coinbase", "00");
        vin.add(coinbase);
        tx.add("vin", vin);
        JsonArray vout = new JsonArray();
        JsonObject output = new JsonObject();
        output.addProperty("n", 0);
        output.addProperty("value", 0);
        JsonObject script = new JsonObject();
        script.addProperty("hex", "76a91465a16059864a2fdbc7c99a4723a8395bc6f188eb88ac");
        JsonArray addresses = new JsonArray();
        addresses.add("2NHBXKyRY4ZBvyfyuZ2fZvqaGyo89vMGFW");
        script.add("addresses", addresses);
        output.add("scriptPubKey", script);
        vout.add(output);
        tx.add("vout", vout);
        return tx;
    }

    private static String header(HttpExchange exchange, String name) {
        return exchange.getRequestHeaders().getFirst(name);
    }

    private static byte[] readAll(InputStream in) throws IOException {
        return in.readAllBytes();
    }

    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
