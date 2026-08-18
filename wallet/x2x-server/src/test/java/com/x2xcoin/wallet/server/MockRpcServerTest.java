package com.x2xcoin.wallet.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockRpcServerTest {
    @Test
    void answersWalletIndexerRpcs() throws Exception {
        try (MockRpcServer mock = new MockRpcServer("127.0.0.1", 0, "x2xrpc", "secret")) {
            mock.start();
            RpcClient client = new RpcClient(RpcClient.mockCliCommand(), mock.host(), mock.port(), "x2xrpc", "secret");
            assertEquals(1, client.call("getblockcount", new JsonArray()).getAsInt());
            JsonObject info = client.call("getinfo", new JsonArray()).getAsJsonObject();
            assertEquals(8, info.get("connections").getAsInt());
            JsonObject block = client.call("getblockbynumber", params(0, true)).getAsJsonObject();
            assertTrue(block.getAsJsonArray("tx").get(0).isJsonObject());
            assertEquals(
                    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                    client.call("sendrawtransaction", params("00")).getAsString()
            );
        }
    }

    private static JsonArray params(Object... values) {
        JsonArray array = new JsonArray();
        for (Object value : values) {
            if (value instanceof Integer integer) {
                array.add(integer);
            } else if (value instanceof Boolean bool) {
                array.add(bool);
            } else {
                array.add(String.valueOf(value));
            }
        }
        return array;
    }
}
