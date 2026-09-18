package com.x2xcoin.wallet.core.wallet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WalletStoreTest {
    @Test
    void encryptsAndRestoresWallet() throws Exception {
        WalletStore original = new WalletStore();
        original.addAccount(WalletAccount.createNew("Principal"));
        String encrypted = original.exportEncrypted("wallet-pass-123");
        WalletStore restored = WalletStore.importEncrypted("wallet-pass-123", encrypted);
        assertEquals(1, restored.accounts().size());
        assertEquals(
                original.activeAccount().orElseThrow().address(),
                restored.activeAccount().orElseThrow().address()
        );
        assertFalse(restored.exportEncrypted("wallet-pass-123").isEmpty());
    }

    @Test
    void generatingAnotherAddressDoesNotStealTheActiveAccount() {
        WalletStore store = new WalletStore();
        WalletAccount principal = WalletAccount.createNew("Principal");
        store.addAccount(principal);
        WalletAccount extra = WalletAccount.createNew("Conta 2");
        store.addAccount(extra);
        assertEquals(principal.id(), store.activeAccount().orElseThrow().id());
        store.setActiveAccount(extra.id());
        assertEquals(extra.id(), store.activeAccount().orElseThrow().id());
    }
}
