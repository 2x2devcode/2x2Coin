package com.x2xcoin.wallet.core.chain;

/**
 * Immutable network parameters for 2x2Coin (2X2) mainnet.
 * Values sourced from src/chainparams.cpp and src/chainparamsbase.cpp.
 */
public final class NetworkParameters {
    public static final String TICKER = "2X2";
    public static final String COIN_NAME = "2x2Coin";
    public static final int COIN_DECIMALS = 8;
    public static final long COIN = 100_000_000L;
    public static final long CENT = 1_000_000L;

    /** P2PKH address version byte (chainparams.cpp PUBKEY_ADDRESS). */
    public static final int PUBKEY_ADDRESS_VERSION = 0x03;
    /** P2SH address version byte (chainparams.cpp SCRIPT_ADDRESS). */
    public static final int SCRIPT_ADDRESS_VERSION = 0x5A;
    /** WIF version byte (chainparams.cpp SECRET_KEY). */
    public static final int WIF_VERSION = 0x80;
    public static final byte WIF_COMPRESSED_SUFFIX = 0x01;

    public static final long MIN_TX_FEE = 10_000L;
    public static final long DEFAULT_FEE_PER_KB = 10_000L;
    public static final int COINBASE_MATURITY = 24;
    public static final int TX_VERSION = 1;
    public static final int LOCKTIME_THRESHOLD = 500_000_000;

    public static final String MESSAGE_MAGIC = "2x2Coin Signed Message:\n";

    public static final String OFFICIAL_API_HOST = "server.2x2coin.com";
    public static final String EXPLORER_HOST = "serverexplorer.2x2coin.com";
    public static final String PUBLIC_EXPLORER_HOST = "explorer.2x2coin.com";
    /** Local bind on VPS; public URL uses HTTPS :443 via reverse proxy. */
    public static final String SERVER_BIND_HOST = "127.0.0.1";
    public static final int OFFICIAL_API_PORT = 40012;
    public static final int EXPLORER_PORT = 40061;
    public static final String OFFICIAL_API_BASE_URL = "https://" + OFFICIAL_API_HOST;
    public static final String EXPLORER_BASE_URL = "https://" + EXPLORER_HOST;
    public static final String PUBLIC_EXPLORER_BASE_URL = "https://" + PUBLIC_EXPLORER_HOST;

    public static final int P2P_PORT = 15190;
    public static final int RPC_PORT = 15189;

    public static final int ADDRESS_MIN_LENGTH = 26;
    public static final int ADDRESS_MAX_LENGTH = 35;

    private NetworkParameters() {
    }

    public static boolean isMainnetP2pkhAddress(String address) {
        if (address == null) {
            return false;
        }
        String trimmed = address.trim();
        if (trimmed.length() < ADDRESS_MIN_LENGTH || trimmed.length() > ADDRESS_MAX_LENGTH) {
            return false;
        }
        return trimmed.matches("[123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz]+");
    }
}
