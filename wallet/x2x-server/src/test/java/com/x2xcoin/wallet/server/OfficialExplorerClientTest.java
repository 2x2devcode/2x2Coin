package com.x2xcoin.wallet.server;

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
        String body = """
                {"address":"2aEv33T2jg7iczvGtoVvvX2ERz1ZJDk7m2","sent":"0","received":"10","balance":"10",
                "last_txs":[{"addresses":"874218e55315afd7951f743accd0e5a948e3d1af483eaf41a18c1f0e2c37082e","type":"vout"}]}
                """;
        List<String> txids = OfficialExplorerClient.txidsFromAddressBody(body, 20);
        assertEquals(
                List.of("874218e55315afd7951f743accd0e5a948e3d1af483eaf41a18c1f0e2c37082e"),
                txids
        );
    }

    @Test
    void ignoresExplorerAddressPayloadWithoutTxids() {
        String body = """
                {"balance":"10.00000000","final_balance":"10.00000000"}
                """;
        assertEquals(List.of(), OfficialExplorerClient.txidsFromAddressBody(body, 20));
    }
}

        String body = """
                {"address":"2aEv33T2jg7iczvGtoVvvX2ERz1ZJDk7m2","sent":"0","received":"10","balance":"10",
                "last_txs":[{"addresses":"874218e55315afd7951f743accd0e5a948e3d1af483eaf41a18c1f0e2c37082e","type":"vout"}]}
                """;
        List<String> txids = OfficialExplorerClient.txidsFromAddressBody(body, 20);
        assertEquals(
                List.of("874218e55315afd7951f743accd0e5a948e3d1af483eaf41a18c1f0e2c37082e"),
                txids
        );
    }

    @Test
    void ignoresExplorerAddressPayloadWithoutTxids() {
        String body = """
                {"balance":"10.00000000","final_balance":"10.00000000"}
                """;
        assertEquals(List.of(), OfficialExplorerClient.txidsFromAddressBody(body, 20));
    }
}
