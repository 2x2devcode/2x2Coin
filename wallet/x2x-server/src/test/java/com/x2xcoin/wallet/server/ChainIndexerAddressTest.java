package com.x2xcoin.wallet.server;

import com.google.gson.JsonObject;
import com.x2xcoin.wallet.core.crypto.AddressCodec;
import com.x2xcoin.wallet.core.crypto.HexUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ChainIndexerAddressTest {
    @TempDir
    Path indexDir;

    @Test
    void p2pkhScriptMatchesKnownAddress() throws Exception {
        System.setProperty("x2x.index.dir", indexDir.toString());
        String address = "2NHBXKyRY4ZBvyfyuZ2fZvqaGyo89vMGFW";
        byte[] script = AddressCodec.p2pkhScript(AddressCodec.decodeHash160(address));
        JsonObject output = new JsonObject();
        JsonObject scriptPubKey = new JsonObject();
        scriptPubKey.addProperty("hex", HexUtils.toHex(script));
        output.add("scriptPubKey", scriptPubKey);
        output.addProperty("n", 0);
        output.addProperty("value", 2.0);

        ChainIndexer indexer = ChainIndexer.open();
        var method = ChainIndexer.class.getDeclaredMethod("outputHasAddress", JsonObject.class, String.class);
        method.setAccessible(true);
        assertTrue((Boolean) method.invoke(indexer, output, address));
    }
}
