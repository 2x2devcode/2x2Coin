package com.x2xcoin.wallet.core.wallet;

import com.x2xcoin.wallet.core.chain.NetworkParameters;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Amount {
    private Amount() {
    }

    public static long toSatoshis(String decimal) {
        String[] parts = decimal.trim().split("\\.");
        long whole = Long.parseLong(parts[0]);
        long fraction = 0;
        if (parts.length > 1) {
            String padded = (parts[1] + "00000000").substring(0, 8);
            fraction = Long.parseLong(padded);
        }
        return whole * NetworkParameters.COIN + fraction;
    }

    public static String fromSatoshis(long satoshis) {
        BigDecimal value = new BigDecimal(satoshis)
                .divide(new BigDecimal(NetworkParameters.COIN), NetworkParameters.COIN_DECIMALS, RoundingMode.DOWN);
        return value.toPlainString();
    }

    public static String formatDisplay(String balance) {
        if (balance == null || balance.isBlank()) {
            return "0.00";
        }
        try {
            return new BigDecimal(balance.trim()).setScale(2, RoundingMode.DOWN).toPlainString();
        } catch (NumberFormatException e) {
            return balance.trim();
        }
    }

    public static boolean isZero(String balance) {
        if (balance == null || balance.isBlank()) {
            return true;
        }
        try {
            return new BigDecimal(balance.trim()).compareTo(BigDecimal.ZERO) == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
