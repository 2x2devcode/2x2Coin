package com.x2xcoin.wallet.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CertificatePinTest {
    @Test
    void convertsHexSpkiToOkHttpPin() {
        String pin = CertificatePin.okHttpPinFromHex(
                "f74d430abec99f630e85d292a1ed7eb44fe2b4cd1035ed1b1ab73becd8334ef9"
        );
        assertTrue(pin.startsWith("sha256/"));
        assertEquals("sha256/901DCr7Jn2MOhdKSoe1+tE/itM0QNe0bGrc77NgzTvk=", pin);
    }
}
