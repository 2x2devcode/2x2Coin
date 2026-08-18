package com.x2xcoin.wallet.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoinCliRpcClientTest {
    @Test
    void invokesCliBinaryAndParsesUnquotedStringResult() throws Exception {
        try (MockRpcServer mock = new MockRpcServer("127.0.0.1", 0, "cliuser", "clipass")) {
            mock.start();
            RpcClient client = new RpcClient(
                    RpcClient.mockCliCommand(),
                    mock.host(),
                    mock.port(),
                    "cliuser",
                    "clipass"
            );
            assertEquals(1, client.call("getblockcount", new JsonArray()).getAsInt());
            JsonObject info = client.call("getinfo", new JsonArray()).getAsJsonObject();
            assertEquals(8, info.get("connections").getAsInt());
            String txid = client.call("sendrawtransaction", params("00")).getAsString();
            assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", txid);
            JsonArray heightParams = new JsonArray();
            heightParams.add(0);
            heightParams.add(true);
            JsonObject block = client.call("getblockbynumber", heightParams).getAsJsonObject();
            assertTrue(block.getAsJsonArray("tx").get(0).isJsonObject());
        }
    }

    private static JsonArray params(String value) {
        JsonArray array = new JsonArray();
        array.add(value);
        return array;
    }
}
