package com.x2xcoin.wallet.server;

import com.x2xcoin.wallet.core.chain.NetworkParameters;

final class RpcClientFactory {
    private RpcClientFactory() {
    }

    static RpcClient fromEnvironment() {
        String host = env("X2X_RPC_HOST", "127.0.0.1");
        int port = Integer.parseInt(env("X2X_RPC_PORT", String.valueOf(NetworkParameters.RPC_PORT)));
        String user = env("X2X_RPC_USER", "x2xrpc");
        String password = env("X2X_RPC_PASSWORD", "x2xrpc");
        System.out.println("[x2x-server] RPC target http://" + host + ":" + port + " user=" + user);
        return new RpcClient(host, port, user, password);
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
