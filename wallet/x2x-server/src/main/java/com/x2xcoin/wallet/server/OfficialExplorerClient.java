package com.x2xcoin.wallet.server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.x2xcoin.wallet.core.chain.NetworkParameters;
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
    private static final Duration TIMEOUT = Duration.ofSeconds(3);
    private static final Duration PEEK_TIMEOUT = Duration.ofMillis(800);

    private final HttpClient httpClient;
    private final HttpClient peekClient;
    private final String baseUrl;
    private final boolean enabled;

    OfficialExplorerClient(String baseUrl, boolean enabled) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.enabled = enabled;
        this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
        this.peekClient = HttpClient.newBuilder().connectTimeout(PEEK_TIMEOUT).build();
    }

    static OfficialExplorerClient fromEnvironment() {
        String baseUrl = env("EXPLORER_FALLBACK_URL", NetworkParameters.IQUIDUS_EXPLORER_BASE_URL);
        boolean enabled = !"false".equalsIgnoreCase(env("EXPLORER_FALLBACK_ENABLED", "true"));
        return new OfficialExplorerClient(baseUrl, enabled);
    }

    boolean enabled() {
        return enabled;
    }

    Long peekBalanceSatoshis(String address) {
        try {
            return readBalanceSatoshis(address, peekClient, PEEK_TIMEOUT);
        } catch (IOException ignored) {
            return null;
        }
    }

    Long balanceSatoshis(String address) throws IOException {
        return readBalanceSatoshis(address, httpClient, TIMEOUT);
    }

    private Long readBalanceSatoshis(String address, HttpClient client, Duration timeout) throws IOException {
        if (!enabled) {
            return null;
        }
        String body = get(client, timeout, "/ext/getbalance/" + address);
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

    /**
     * Decoded tx the indexer can apply (vin/vout + {@code blockheight}).
     * Prefers Iquidus {@code /api/getrawtransaction} so spends have prevouts;
     * height comes from {@code /ext/gettx}.
     */
    JsonObject decodedTransaction(String txid) {
        if (!enabled || txid == null || txid.isBlank()) {
            return null;
        }
        JsonObject tx = indexableTx(getQuiet("/api/getrawtransaction?txid=" + txid + "&decrypt=1"));
        if (tx == null) {
            tx = indexableTx(getQuiet("/ext/gettx/" + txid));
        }
        if (tx == null) {
            return null;
        }
        if (!tx.has("blockheight")) {
            Integer height = blockHeightFromGetTx(getQuiet("/ext/gettx/" + txid));
            if (height != null) {
                tx.addProperty("blockheight", height);
            }
        }
        return tx.has("blockheight") ? tx : null;
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

    static JsonObject indexableTx(String body) {
        if (body == null || body.isBlank() || !body.trim().startsWith("{")) {
            return null;
        }
        JsonObject json = GSON.fromJson(body, JsonObject.class);
        if (json == null) {
            return null;
        }
        if (json.has("tx") && json.get("tx").isJsonObject()) {
            return indexableFromIquidusGetTx(json);
        }
        if (json.has("vout") && json.has("txid")) {
            return json;
        }
        return null;
    }

    static Integer blockHeightFromGetTx(String body) {
        if (body == null || body.isBlank() || !body.trim().startsWith("{")) {
            return null;
        }
        JsonObject json = GSON.fromJson(body, JsonObject.class);
        if (json == null || !json.has("tx") || !json.get("tx").isJsonObject()) {
            return null;
        }
        JsonObject tx = json.getAsJsonObject("tx");
        if (tx.has("blockindex")) {
            return tx.get("blockindex").getAsInt();
        }
        if (tx.has("blockheight")) {
            return tx.get("blockheight").getAsInt();
        }
        return null;
    }

    static JsonObject indexableFromIquidusGetTx(JsonObject wrapper) {
        JsonObject src = wrapper.getAsJsonObject("tx");
        JsonObject tx = new JsonObject();
        if (src.has("txid")) {
            tx.addProperty("txid", src.get("txid").getAsString());
        }
        if (src.has("blockindex")) {
            tx.addProperty("blockheight", src.get("blockindex").getAsInt());
        }
        JsonArray vinOut = new JsonArray();
        if (src.has("vin") && src.get("vin").isJsonArray()) {
            for (JsonElement element : src.getAsJsonArray("vin")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject vin = element.getAsJsonObject();
                if (vin.has("txid") && vin.has("vout")) {
                    vinOut.add(vin);
                }
            }
        }
        tx.add("vin", vinOut);
        JsonArray voutOut = new JsonArray();
        if (src.has("vout") && src.get("vout").isJsonArray()) {
            int n = 0;
            for (JsonElement element : src.getAsJsonArray("vout")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject srcOut = element.getAsJsonObject();
                JsonObject out = new JsonObject();
                int index = srcOut.has("n") ? srcOut.get("n").getAsInt() : n;
                out.addProperty("n", index);
                if (srcOut.has("value")) {
                    out.add("value", srcOut.get("value"));
                } else if (srcOut.has("amount")) {
                    try {
                        long sats = Long.parseLong(srcOut.get("amount").getAsString().split("\\.")[0]);
                        out.addProperty("value", sats / (double) NetworkParameters.COIN);
                    } catch (NumberFormatException ignored) {
                        n++;
                        continue;
                    }
                }
                JsonObject script = new JsonObject();
                JsonArray addresses = new JsonArray();
                if (srcOut.has("addresses")) {
                    if (srcOut.get("addresses").isJsonArray()) {
                        addresses = srcOut.getAsJsonArray("addresses");
                    } else if (srcOut.get("addresses").isJsonPrimitive()) {
                        addresses.add(srcOut.get("addresses").getAsString());
                    }
                }
                script.add("addresses", addresses);
                out.add("scriptPubKey", script);
                voutOut.add(out);
                n++;
            }
        }
        tx.add("vout", voutOut);
        return tx;
    }

    private String get(String path) throws IOException {
        return get(httpClient, TIMEOUT, path);
    }

    private String get(HttpClient client, Duration timeout, String path) throws IOException {
        URI uri = URI.create(baseUrl + path);
        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(timeout)
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
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
