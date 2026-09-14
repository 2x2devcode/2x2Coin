package com.x2xcoin.wallet.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestRateLimiterTest {
    @Test
    void groupsAddressLookupsAndCapsBroadcasts() {
        RequestRateLimiter limiter = new RequestRateLimiter(60_000L);
        assertEquals("/api/address", RequestRateLimiter.routeClass("/api/address/2abc/balance"));
        assertEquals("/ext/getaddress", RequestRateLimiter.routeClass("/ext/getaddress/2abc"));
        assertEquals("/api/tx/broadcast", RequestRateLimiter.routeClass("/api/tx/broadcast"));

        String ip = "203.0.113.9";
        for (int i = 0; i < 12; i++) {
            assertTrue(limiter.allow(ip, "POST", "/api/tx/broadcast"));
        }
        assertFalse(limiter.allow(ip, "POST", "/api/tx/broadcast"));

        assertTrue(limiter.allow(ip, "GET", "/api/address/one/balance"));
        for (int i = 1; i < 40; i++) {
            assertTrue(limiter.allow(ip, "GET", "/api/address/other-" + i + "/utxos"));
        }
        assertFalse(limiter.allow(ip, "GET", "/api/address/last/balance"));
    }
}
