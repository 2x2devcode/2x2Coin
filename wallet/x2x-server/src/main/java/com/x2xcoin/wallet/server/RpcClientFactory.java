package com.x2xcoin.wallet.server;

import com.x2xcoin.wallet.core.chain.NetworkParameters;

final class RpcClientFactory {
    private RpcClientFactory() {
    }

    static RpcClient fromEnvironment() {
        String host = env("X2X_RPC_HOST", "127.0.0.1");
        int port = Integer.parseInt(env("X2X_RPC_PORT", String.valueOf(NetworkParameters.RPC_PORT)));
        String user = env("X2X_RPC_USER", "");
        String password = env("X2X_RPC_PASSWORD", "");
        String cli = env("X2X_CLI", "2x2coin-cli");
        System.out.println("[x2x-server] RPC via " + cli + " -> " + host + ":" + port
                + (user.isBlank() ? " (credentials from 2x2coin-cli conf/cookie)" : " user=" + user));
        return new RpcClient(java.util.List.of(cli), host, port, user, password);
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
