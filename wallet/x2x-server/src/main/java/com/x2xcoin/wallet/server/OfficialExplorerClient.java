package com.x2xcoin.wallet.server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.x2xcoin.wallet.core.wallet.Amount;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Read-only client for the public 2x2Coin block explorer API.
 * Used when the local chain index is behind or a deep scan has not finished yet.
 */
final class OfficialExplorerClient {
    private static final Gson GSON = new Gson();
    private static final Duration TIMEOUT = Duration.ofSeconds(6);

    private final HttpClient httpClient;
    private final String baseUrl;
    private final boolean enabled;

    OfficialExplorerClient(String baseUrl, boolean enabled) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.enabled = enabled;
        this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    }

    static OfficialExplorerClient fromEnvironment() {
        String baseUrl = env("EXPLORER_FALLBACK_URL", "https://explorer.2x2coin.com");
        boolean enabled = !"false".equalsIgnoreCase(env("EXPLORER_FALLBACK_ENABLED", "true"));
        return new OfficialExplorerClient(baseUrl, enabled);
    }

    boolean enabled() {
        return enabled;
    }

    Long balanceSatoshis(String address) throws IOException {
        if (!enabled) {
            return null;
        }
        String body = get("/ext/getbalance/" + address);
        if (body == null || body.isBlank()) {
            return null;
        }
        String trimmed = body.trim();
        if (trimmed.startsWith("{")) {
            JsonObject json = GSON.fromJson(trimmed, JsonObject.class);
            if (json.has("balance")) {
                trimmed = json.get("balance").getAsString();
            } else if (json.has("final_balance")) {
                trimmed = json.get("final_balance").getAsString();
            }
        }
        try {
            return Amount.toSatoshis(trimmed);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored) {
            return null;
        }
    }

    List<String> recentTxids(String address, int limit) throws IOException {
        if (!enabled) {
            return List.of();
        }
        int max = Math.max(1, limit);
        List<String> txids = txidsFromAddressTxsBody(getQuiet("/ext/getaddresstxs/" + address + "/0/" + max), max);
        if (!txids.isEmpty()) {
            return txids;
        }
        return txidsFromAddressBody(getQuiet("/ext/getaddress/" + address), max);
    }

    static List<String> txidsFromAddressTxsBody(String body, int limit) {
        if (body == null || body.isBlank() || !body.trim().startsWith("[")) {
            return List.of();
        }
        JsonArray array = GSON.fromJson(body, JsonArray.class);
        if (array == null) {
            return List.of();
        }
        List<String> txids = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject item = element.getAsJsonObject();
            if (item.has("txid")) {
                txids.add(item.get("txid").getAsString());
            }
        }
        Collections.reverse(txids);
        if (txids.size() > limit) {
            return new ArrayList<>(txids.subList(0, limit));
        }
        return txids;
    }

    /**
     * Iquidus {@code /ext/getaddress} puts txids in {@code last_txs[].addresses}.
     */
    static List<String> txidsFromAddressBody(String body, int limit) {
        if (body == null || body.isBlank() || !body.trim().startsWith("{")) {
            return List.of();
        }
        JsonObject json = GSON.fromJson(body, JsonObject.class);
        if (json == null || !json.has("last_txs") || !json.get("last_txs").isJsonArray()) {
            return List.of();
        }
        List<String> txids = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray("last_txs")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject item = element.getAsJsonObject();
            String txid = null;
            if (item.has("txid") && item.get("txid").isJsonPrimitive()) {
                txid = item.get("txid").getAsString();
            } else if (item.has("addresses") && item.get("addresses").isJsonPrimitive()) {
                txid = item.get("addresses").getAsString();
            }
            if (txid != null && !txid.isBlank()) {
                txids.add(txid);
            }
        }
        Collections.reverse(txids);
        if (txids.size() > limit) {
            return new ArrayList<>(txids.subList(0, limit));
        }
        return txids;
    }

    private String get(String path) throws IOException {
        URI uri = URI.create(baseUrl + path);
        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("explorer HTTP " + response.statusCode() + " for " + uri);
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("explorer request interrupted", e);
        }
    }

    private String getQuiet(String path) {
        try {
            return get(path);
        } catch (IOException ignored) {
            return null;
        }
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
