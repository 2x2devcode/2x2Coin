package com.x2xcoin.wallet.server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * Drop-in stand-in for {@code 2x2coin-cli}: same flags and stdout as the real binary,
 * forwarding JSON-RPC to a local daemon or {@link MockRpcServer}.
 */
public final class MockCoinCli {
    private static final Gson GSON = new Gson();

    private MockCoinCli() {
    }

    public static void main(String[] args) {
        try {
            Parsed parsed = parse(args);
            JsonElement result = call(parsed);
            System.out.println(formatLikeCoinCli(result));
        } catch (Exception e) {
            System.err.println("error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static JsonElement call(Parsed parsed) throws Exception {
        JsonObject request = new JsonObject();
        request.addProperty("jsonrpc", "1.0");
        request.addProperty("id", 1);
        request.addProperty("method", parsed.method);
        request.add("params", parsed.params);
        String token = Base64.getEncoder().encodeToString(
                (parsed.user + ":" + parsed.password).getBytes(StandardCharsets.UTF_8)
        );
        URI uri = URI.create(String.format(Locale.US, "http://%s:%d/", parsed.host, parsed.port));
        HttpRequest httpRequest = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(Math.max(3, parsed.timeoutSeconds)))
                .header("Authorization", "Basic " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(request)))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 401) {
            throw new IllegalStateException("incorrect rpcuser or rpcpassword (authorization failed)");
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("server returned HTTP error " + response.statusCode());
        }
        JsonObject json = GSON.fromJson(response.body(), JsonObject.class);
        if (json.has("error") && !json.get("error").isJsonNull()) {
            throw new IllegalStateException(json.get("error").toString());
        }
        return json.get("result");
    }

    static String formatLikeCoinCli(JsonElement result) {
        if (result == null || result.isJsonNull()) {
            return "";
        }
        if (result.isJsonPrimitive() && result.getAsJsonPrimitive().isString()) {
            return result.getAsString();
        }
        return GSON.toJson(result);
    }

    private static Parsed parse(String[] args) {
        Parsed parsed = new Parsed();
        List<String> positional = new ArrayList<>();
        for (String arg : args) {
            if (arg.startsWith("-rpcconnect=")) {
                parsed.host = arg.substring("-rpcconnect=".length());
            } else if (arg.startsWith("-rpcport=")) {
                parsed.port = Integer.parseInt(arg.substring("-rpcport=".length()));
            } else if (arg.startsWith("-rpcuser=")) {
                parsed.user = arg.substring("-rpcuser=".length());
            } else if (arg.startsWith("-rpcpassword=")) {
                parsed.password = arg.substring("-rpcpassword=".length());
            } else if (arg.startsWith("-rpcclienttimeout=")) {
                parsed.timeoutSeconds = Integer.parseInt(arg.substring("-rpcclienttimeout=".length()));
            } else if (arg.startsWith("-conf=") || arg.startsWith("-datadir=")) {
                // ignored by the mock; real 2x2coin-cli reads these from disk
            } else if (arg.startsWith("-")) {
                throw new IllegalArgumentException("unknown option " + arg);
            } else {
                positional.add(arg);
            }
        }
        if (positional.isEmpty()) {
            throw new IllegalArgumentException("too few parameters (need at least command)");
        }
        parsed.method = positional.get(0);
        parsed.params = new JsonArray();
        for (int i = 1; i < positional.size(); i++) {
            parsed.params.add(parsePositional(positional.get(i)));
        }
        return parsed;
    }

    private static JsonElement parsePositional(String value) {
        if ("true".equals(value) || "false".equals(value)) {
            return new JsonPrimitive(Boolean.parseBoolean(value));
        }
        if ("null".equals(value)) {
            return com.google.gson.JsonNull.INSTANCE;
        }
        try {
            if (value.startsWith("{") || value.startsWith("[")) {
                return com.google.gson.JsonParser.parseString(value);
            }
            if (value.matches("-?\\d+")) {
                return new JsonPrimitive(Long.parseLong(value));
            }
        } catch (RuntimeException ignored) {
            // fall through to string
        }
        return new JsonPrimitive(value);
    }

    private static final class Parsed {
        String host = "127.0.0.1";
        int port = 15189;
        String user = "";
        String password = "";
        int timeoutSeconds = 8;
        String method;
        JsonArray params;
    }
}
