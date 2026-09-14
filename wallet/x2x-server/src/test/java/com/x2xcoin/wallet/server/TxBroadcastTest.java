package com.x2xcoin.wallet.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TxBroadcastTest {
    @Test
    void acceptsEvenLengthHex() {
        assertEquals("00ff", TxBroadcast.requireRawHex("00ff"));
        assertEquals("ab", TxBroadcast.requireRawHex("0xab"));
        assertEquals("AB", TxBroadcast.requireRawHex("  AB  "));
    }

    @Test
    void rejectsMissingOddOrNonHex() {
        assertThrows(ClientRequestException.class, () -> TxBroadcast.requireRawHex(null));
        assertThrows(ClientRequestException.class, () -> TxBroadcast.requireRawHex(""));
        assertThrows(ClientRequestException.class, () -> TxBroadcast.requireRawHex("0"));
        assertThrows(ClientRequestException.class, () -> TxBroadcast.requireRawHex("zz"));
        assertThrows(ClientRequestException.class, () -> TxBroadcast.requireRawHex("0xgg"));
    }

    @Test
    void rejectsOversizedHex() {
        String huge = "aa".repeat((TxBroadcast.MAX_HEX_CHARS / 2) + 1);
        assertThrows(ClientRequestException.class, () -> TxBroadcast.requireRawHex(huge));
    }

    @Test
    void readRawTxRequiresStringField() {
        assertEquals("00", X2xServer.readRawTx("{\"rawTx\":\"00\"}"));
        assertThrows(ClientRequestException.class, () -> X2xServer.readRawTx(""));
        assertThrows(ClientRequestException.class, () -> X2xServer.readRawTx("{}"));
        assertThrows(ClientRequestException.class, () -> X2xServer.readRawTx("{\"rawTx\":null}"));
        assertThrows(ClientRequestException.class, () -> X2xServer.readRawTx("{\"rawTx\":1}"));
        assertThrows(ClientRequestException.class, () -> X2xServer.readRawTx("not-json"));
    }
}
