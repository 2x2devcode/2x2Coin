package com.x2xcoin.wallet.server;

import com.x2xcoin.wallet.core.chain.NetworkParameters;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class RpcClientFactory {
    private RpcClientFactory() {
    }

    static RpcClient fromEnvironment() {
        String host = env("X2X_RPC_HOST", "127.0.0.1");
        int port = Integer.parseInt(env("X2X_RPC_PORT", String.valueOf(NetworkParameters.RPC_PORT)));
        String cli = env("X2X_CLI", "2x2coin-cli");
        String dataDir = env("X2X_DATADIR", "");
        ResolvedCredentials credentials = resolveCredentials();
        System.out.println("[x2x-server] RPC via " + cli + " -> " + host + ":" + port
                + (credentials.confFile.isBlank()
                ? (credentials.user.isBlank() ? " (credentials from 2x2coin-cli cookie)" : " user=" + credentials.user)
                : " conf=" + credentials.confFile));
        return new RpcClient(
                List.of(cli),
                host,
                port,
                credentials.user,
                credentials.password,
                credentials.confFile,
                dataDir,
                RpcClient.readTimeoutSeconds()
        );
    }

    /**
     * Prefer {@code -conf} so the Java process never holds {@code rpcpassword} in its environment
     * or argv. Explicit {@code X2X_RPC_USER}/{@code X2X_RPC_PASSWORD} win when no conf path is set
     * (mock CLI / tests).
     */
    static ResolvedCredentials resolveCredentials() {
        String explicitConf = System.getenv("X2XCOIN_CONF");
        String user = env("X2X_RPC_USER", "");
        String password = env("X2X_RPC_PASSWORD", "");
        if (explicitConf != null && !explicitConf.isBlank() && Files.isRegularFile(Path.of(explicitConf))) {
            return new ResolvedCredentials("", "", explicitConf);
        }
        if (!user.isBlank() || !password.isBlank()) {
            return new ResolvedCredentials(user, password, "");
        }
        String fallback = defaultConfIfPresent();
        if (!fallback.isBlank()) {
            return new ResolvedCredentials("", "", fallback);
        }
        return new ResolvedCredentials("", "", "");
    }

    private static String defaultConfIfPresent() {
        String home = System.getProperty("user.home", "");
        if (home.isBlank()) {
            return "";
        }
        Path candidate = Path.of(home, ".2x2coin", "2x2coin.conf");
        return Files.isRegularFile(candidate) ? candidate.toString() : "";
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    static final class ResolvedCredentials {
        final String user;
        final String password;
        final String confFile;

        ResolvedCredentials(String user, String password, String confFile) {
            this.user = user == null ? "" : user;
            this.password = password == null ? "" : password;
            this.confFile = confFile == null ? "" : confFile;
        }
    }
}
