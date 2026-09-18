package com.x2xcoin.wallet.server;

import com.google.gson.JsonObject;
import com.x2xcoin.wallet.core.wallet.Amount;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OfficialExplorerClientTest {
    @Test
    void parsesWholeCoinBalance() {
        assertEquals(200_000_000L, Amount.toSatoshis("2"));
        assertEquals(200_000_000L, Amount.toSatoshis("2.0"));
    }

    @Test
    void readsIquidusLastTxsFromGetAddress() {
        String body = "{"
                + "\"address\":\"2aEv33T2jg7iczvGtoVvvX2ERz1ZJDk7m2\","
                + "\"sent\":\"0\",\"received\":\"10\",\"balance\":\"10\","
                + "\"last_txs\":[{\"addresses\":\"874218e55315afd7951f743accd0e5a948e3d1af483eaf41a18c1f0e2c37082e\",\"type\":\"vout\"}]"
                + "}";
        List<String> txids = OfficialExplorerClient.txidsFromAddressBody(body, 20);
        assertEquals(
                List.of("874218e55315afd7951f743accd0e5a948e3d1af483eaf41a18c1f0e2c37082e"),
                txids
        );
    }

    @Test
    void ignoresExplorerAddressPayloadWithoutTxids() {
        String body = "{\"balance\":\"10.00000000\",\"final_balance\":\"10.00000000\"}";
        assertEquals(List.of(), OfficialExplorerClient.txidsFromAddressBody(body, 20));
    }

    @Test
    void indexesIquidusGetTxOutputForFundedAddress() {
        String body = "{"
                + "\"tx\":{"
                + "\"txid\":\"874218e55315afd7951f743accd0e5a948e3d1af483eaf41a18c1f0e2c37082e\","
                + "\"vout\":["
                + "{\"addresses\":\"2XVRXYNzVCx3PSe3ikFcQFNTc133avfP1x\",\"amount\":\"48195980000\"},"
                + "{\"addresses\":\"2aEv33T2jg7iczvGtoVvvX2ERz1ZJDk7m2\",\"amount\":\"1000000000\"}"
                + "],"
                + "\"blockindex\":198498"
                + "}}";
        JsonObject tx = OfficialExplorerClient.indexableTx(body);
        assertEquals("874218e55315afd7951f743accd0e5a948e3d1af483eaf41a18c1f0e2c37082e", tx.get("txid").getAsString());
        assertEquals(198498, tx.get("blockheight").getAsInt());
        JsonObject funded = tx.getAsJsonArray("vout").get(1).getAsJsonObject();
        assertEquals(1, funded.get("n").getAsInt());
        assertEquals(10.0, funded.get("value").getAsDouble(), 0.00000001);
        assertEquals(
                "2aEv33T2jg7iczvGtoVvvX2ERz1ZJDk7m2",
                funded.getAsJsonObject("scriptPubKey").getAsJsonArray("addresses").get(0).getAsString()
        );
        assertEquals(198498, OfficialExplorerClient.blockHeightFromGetTx(body));
    }
}
