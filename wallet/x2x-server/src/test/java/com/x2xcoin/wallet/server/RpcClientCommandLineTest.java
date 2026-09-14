package com.x2xcoin.wallet.server;

import com.google.gson.JsonArray;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RpcClientCommandLineTest {
    @Test
    void passesCredentialsWithoutConfFile() {
        RpcClient client = new RpcClient(List.of("2x2coin-cli"), "127.0.0.1", 15189, "rpcuser", "secret");
        List<String> command = client.commandLine("getblockcount", new JsonArray());
        assertTrue(command.contains("-rpcuser=rpcuser"));
        assertTrue(command.contains("-rpcpassword=secret"));
        assertFalse(command.stream().anyMatch(arg -> arg.startsWith("-conf=")));
    }

    @Test
    void omitsPasswordFromArgvWhenConfIsSet(@TempDir Path dir) throws Exception {
        Path conf = dir.resolve("2x2coin.conf");
        Files.writeString(conf, "rpcuser=x2xrpc\nrpcpassword=from-file\n");
        RpcClient client = new RpcClient(
                List.of("2x2coin-cli"),
                "127.0.0.1",
                15189,
                "ignored",
                "ignored-secret",
                conf.toString(),
                "",
                8
        );
        List<String> command = client.commandLine("getinfo", new JsonArray());
        assertTrue(command.contains("-conf=" + conf));
        assertFalse(command.stream().anyMatch(arg -> arg.startsWith("-rpcuser=")));
        assertFalse(command.stream().anyMatch(arg -> arg.startsWith("-rpcpassword=")));
    }
}
