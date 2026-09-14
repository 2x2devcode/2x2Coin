package com.x2xcoin.wallet.server;

/**
 * Rejects oversized or non-hex raw transactions before they reach 2x2coin-cli.
 */
final class TxBroadcast {
    static final int MAX_HEX_CHARS = 131_072;

    private TxBroadcast() {
    }

    static String requireRawHex(String rawTx) {
        if (rawTx == null || rawTx.isBlank()) {
            throw new ClientRequestException("invalid transaction");
        }
        String hex = rawTx.trim();
        if (hex.startsWith("0x") || hex.startsWith("0X")) {
            hex = hex.substring(2);
        }
        if ((hex.length() & 1) != 0 || hex.length() > MAX_HEX_CHARS) {
            throw new ClientRequestException("invalid transaction");
        }
        for (int i = 0; i < hex.length(); i++) {
            char c = hex.charAt(i);
            boolean hexDigit = (c >= '0' && c <= '9')
                    || (c >= 'a' && c <= 'f')
                    || (c >= 'A' && c <= 'F');
            if (!hexDigit) {
                throw new ClientRequestException("invalid transaction");
            }
        }
        return hex;
    }
}
