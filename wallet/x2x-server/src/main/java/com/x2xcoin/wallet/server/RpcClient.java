package com.x2xcoin.wallet.server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Talks to the 2x2 node through {@code 2x2coin-cli} (same binary operators use on the VPS).
 * Production: {@code 2x2coin-cli -rpcconnect=... getinfo}
 * Tests/smoke: {@code MockCoinCli} with the same argument layout.
 */
final class RpcClient {
    private static final Gson GSON = new Gson();
    private final List<String> cliCommand;
    private final String host;
    private final int port;
    private final String user;
    private final String password;
    private final String confFile;
    private final String dataDir;
    private final int callTimeoutSeconds;

    RpcClient(String host, int port, String user, String password) {
        this(defaultCliCommand(), host, port, user, password, env("X2XCOIN_CONF", ""), env("X2X_DATADIR", ""), readTimeoutSeconds());
    }

    RpcClient(List<String> cliCommand, String host, int port, String user, String password) {
        this(cliCommand, host, port, user, password, env("X2XCOIN_CONF", ""), env("X2X_DATADIR", ""), readTimeoutSeconds());
    }

    RpcClient(
            List<String> cliCommand,
            String host,
            int port,
            String user,
            String password,
            String confFile,
            String dataDir,
            int callTimeoutSeconds
    ) {
        if (cliCommand == null || cliCommand.isEmpty()) {
            throw new IllegalArgumentException("2x2coin-cli command is required");
        }
        this.cliCommand = List.copyOf(cliCommand);
        this.host = host;
        this.port = port;
        this.user = user == null ? "" : user;
        this.password = password == null ? "" : password;
        this.confFile = confFile == null ? "" : confFile;
        this.dataDir = dataDir == null ? "" : dataDir;
        this.callTimeoutSeconds = callTimeoutSeconds;
    }

    static List<String> defaultCliCommand() {
        String configured = env("X2X_CLI", "2x2coin-cli");
        return List.of(configured);
    }

    static List<String> mockCliCommand() {
        return List.of(
                PathResolver.javaBinary(),
                "-cp",
                System.getProperty("java.class.path"),
                MockCoinCli.class.getName()
        );
    }

    JsonElement call(String method, JsonArray params) throws IOException {
        List<String> command = new ArrayList<>(cliCommand);
        if (host != null && !host.isBlank()) {
            command.add("-rpcconnect=" + host);
        }
        if (port > 0) {
            command.add("-rpcport=" + port);
        }
        if (!user.isBlank()) {
            command.add("-rpcuser=" + user);
        }
        if (!password.isBlank()) {
            command.add("-rpcpassword=" + password);
        }
        command.add("-rpcclienttimeout=" + callTimeoutSeconds);
        if (!confFile.isBlank()) {
            command.add("-conf=" + confFile);
        }
        if (!dataDir.isBlank()) {
            command.add("-datadir=" + dataDir);
        }
        command.add(method);
        if (params != null) {
            for (JsonElement element : params) {
                command.add(toCliArg(element));
            }
        }

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(false);
        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new IOException(
                    "failed to start 2x2coin-cli (" + cliCommand.get(0) + "): " + e.getMessage()
                            + " — install 2x2coin-cli on PATH or set X2X_CLI",
                    e
            );
        }

        CompletableFuture<String> stdout = CompletableFuture.supplyAsync(() -> readStream(process.getInputStream()));
        CompletableFuture<String> stderr = CompletableFuture.supplyAsync(() -> readStream(process.getErrorStream()));
        boolean finished;
        try {
            finished = process.waitFor(callTimeoutSeconds + 2L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IOException("2x2coin-cli interrupted", e);
        }
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("2x2coin-cli timed out calling " + method);
        }
        String out = stdout.join().trim();
        String err = stderr.join().trim();
        int exit = process.exitValue();
        if (exit != 0) {
            String detail = err.isBlank() ? out : err;
            throw new IOException("2x2coin-cli " + method + " failed (exit " + exit + "): " + detail);
        }
        return parseCliOutput(out);
    }

    private static JsonElement parseCliOutput(String output) {
        if (output == null || output.isBlank()) {
            return com.google.gson.JsonNull.INSTANCE;
        }
        try {
            return JsonParser.parseString(output);
        } catch (JsonSyntaxException ignored) {
            return new JsonPrimitive(output);
        }
    }

    private static String toCliArg(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return "null";
        }
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isBoolean()) {
                return primitive.getAsBoolean() ? "true" : "false";
            }
            if (primitive.isNumber()) {
                BigDecimal value = primitive.getAsBigDecimal();
                BigDecimal stripped = value.stripTrailingZeros();
                if (stripped.scale() <= 0) {
                    return stripped.toBigInteger().toString();
                }
                return stripped.toPlainString();
            }
            return primitive.getAsString();
        }
        return GSON.toJson(element);
    }

    private static String readStream(InputStream stream) {
        try {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private static int readTimeoutSeconds() {
        String value = System.getenv("RPC_TIMEOUT_SECONDS");
        if (value == null || value.isBlank()) {
            return 8;
        }
        try {
            return Math.max(3, Integer.parseInt(value.trim()));
        } catch (NumberFormatException ignored) {
            return 8;
        }
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static final class PathResolver {
        private PathResolver() {
        }

        static String javaBinary() {
            String home = System.getProperty("java.home");
            if (home == null || home.isBlank()) {
                return "java";
            }
            return java.nio.file.Path.of(home, "bin", "java").toString();
        }
    }
}
