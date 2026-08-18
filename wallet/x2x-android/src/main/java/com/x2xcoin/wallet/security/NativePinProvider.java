package com.x2xcoin.wallet.security;

import com.x2xcoin.wallet.api.PinProvider;
import com.x2xcoin.wallet.core.chain.NetworkParameters;

import java.util.Arrays;
import java.util.List;

public final class NativePinProvider implements PinProvider {
    static {
        System.loadLibrary("x2xpin");
    }

    private native String[] getApiPinnedHashes();
    private native String[] getExplorerPinnedHashes();

    @Override
    public String host() {
        return NetworkParameters.OFFICIAL_API_HOST;
    }

    @Override
    public List<String> pinnedHosts() {
        return List.of(NetworkParameters.OFFICIAL_API_HOST, NetworkParameters.EXPLORER_HOST);
    }

    @Override
    public List<String> pinsForHost(String host) {
        if (NetworkParameters.OFFICIAL_API_HOST.equalsIgnoreCase(host)) {
            return Arrays.asList(getApiPinnedHashes());
        }
        if (NetworkParameters.EXPLORER_HOST.equalsIgnoreCase(host)) {
            return Arrays.asList(getExplorerPinnedHashes());
        }
        return List.of();
    }
}
