package com.x2xcoin.wallet.core.wallet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmountTest {
    @Test
    void formatDisplayTruncatesToTwoDecimals() {
        assertEquals("10.00", Amount.formatDisplay("10.00000000"));
        assertEquals("0.00", Amount.formatDisplay("0.00000000"));
        assertTrue(Amount.isZero("0.00000000"));
        assertFalse(Amount.isZero("10.00000000"));
    }
}
