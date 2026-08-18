package com.x2xcoin.wallet.core.tx;

import com.x2xcoin.wallet.core.crypto.HexUtils;

import java.util.Objects;

public final class OutPoint {
    private final String txid;
    private final int index;

    public OutPoint(String txid, int index) {
        this.txid = Objects.requireNonNull(txid, "txid");
        this.index = index;
    }

    public String txid() {
        return txid;
    }

    public int index() {
        return index;
    }

    public String key() {
        return txid + ":" + index;
    }
}
