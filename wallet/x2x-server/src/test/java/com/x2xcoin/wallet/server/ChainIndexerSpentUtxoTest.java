package com.x2xcoin.wallet.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChainIndexerSpentUtxoTest {
    @TempDir
    Path indexDir;

    @Test
    void dropsIndexedUtxoWhenGetTxOutSaysSpent() throws Exception {
        System.setProperty("x2x.index.dir", indexDir.toString());
        String address = "2NHBXKyRY4ZBvyfyuZ2fZvqaGyo89vMGFW";
        String spentTxid = "adbbd0b79bdab077240e15cfae4a6be6d889b7f72bb87d9efdc5d2adaa6f680f";
        Files.writeString(indexDir.resolve("chain-index.json"), """
                {
                  "indexedHeight": -1,
                  "chainTip": 201104,
                  "outpoints": {
                    "%s:1": {
                      "txid": "%s",
                      "vout": 1,
                      "address": "%s",
                      "amountSatoshis": 1000000000,
                      "blockHeight": 186388
                    }
                  },
                  "utxosByAddress": {
                    "%s": ["%s:1"]
                  }
                }
                """.formatted(spentTxid, spentTxid, address, address, spentTxid), StandardCharsets.UTF_8);

        try (MockRpcServer mock = new MockRpcServer("127.0.0.1", 0, "x2xrpc", "secret")) {
            mock.start();
            RpcClient rpcClient = new RpcClient(RpcClient.mockCliCommand(), mock.host(), mock.port(), "x2xrpc", "secret");
            ChainIndexer indexer = ChainIndexer.open(indexDir);
            JsonArray params = new JsonArray();
            params.add(spentTxid);
            params.add(1);
            params.add(true);
            assertTrue(rpcClient.call("gettxout", params).isJsonNull(), "mock gettxout must report the outpoint spent");
            ChainIndexer.BalanceResult result = indexer.balanceFast(address, 1, rpcClient);
            assertEquals(0L, result.satoshis());
            assertTrue(indexer.utxosFor(address, 1, rpcClient).isEmpty());
        }
    }

    @Test
    void apiBalanceShowsZeroAfterSpentUtxoIsDropped() throws Exception {
        System.setProperty("x2x.index.dir", indexDir.toString());
        String address = "2NHBXKyRY4ZBvyfyuZ2fZvqaGyo89vMGFW";
        String spentTxid = "adbbd0b79bdab077240e15cfae4a6be6d889b7f72bb87d9efdc5d2adaa6f680f";
        Files.writeString(indexDir.resolve("chain-index.json"), """
                {
                  "indexedHeight": -1,
                  "chainTip": 201104,
                  "outpoints": {
                    "%s:1": {
                      "txid": "%s",
                      "vout": 1,
                      "address": "%s",
                      "amountSatoshis": 1000000000,
                      "blockHeight": 186388
                    }
                  },
                  "utxosByAddress": {
                    "%s": ["%s:1"]
                  }
                }
                """.formatted(spentTxid, spentTxid, address, address, spentTxid), StandardCharsets.UTF_8);

        try (MockRpcServer mock = new MockRpcServer("127.0.0.1", 0, "x2xrpc", "secret")) {
            mock.start();
            RpcClient rpcClient = new RpcClient(RpcClient.mockCliCommand(), mock.host(), mock.port(), "x2xrpc", "secret");
            AddressQueryService service = new AddressQueryService(
                    rpcClient,
                    ChainIndexer.open(indexDir),
                    new OfficialExplorerClient("http://127.0.0.1:1", false)
            );
            JsonObject balance = service.balance(address);
            assertEquals("0.00000000", balance.get("balance").getAsString());
            assertEquals(address, balance.get("address").getAsString());
        }
    }

    @Test
    void emptyBalanceIsNotScanningWhenIndexIsCaughtUp() throws Exception {
        System.setProperty("x2x.index.dir", indexDir.toString());
        String address = "2NHBXKyRY4ZBvyfyuZ2fZvqaGyo89vMGFW";
        Files.writeString(indexDir.resolve("chain-index.json"), """
                {
                  "indexedHeight": 203857,
                  "chainTip": 203857,
                  "outpoints": {},
                  "utxosByAddress": {}
                }
                """, StandardCharsets.UTF_8);

        try (MockRpcServer mock = new MockRpcServer("127.0.0.1", 0, "x2xrpc", "secret")) {
            mock.start();
            RpcClient rpcClient = new RpcClient(RpcClient.mockCliCommand(), mock.host(), mock.port(), "x2xrpc", "secret");
            ChainIndexer indexer = ChainIndexer.open(indexDir);
            ChainIndexer.BalanceResult result = indexer.balanceFast(address, 1, rpcClient);
            assertEquals(0L, result.satoshis());
            assertFalse(result.scanning());
            assertTrue(indexer.isFullyIndexed());
        }
    }
}
