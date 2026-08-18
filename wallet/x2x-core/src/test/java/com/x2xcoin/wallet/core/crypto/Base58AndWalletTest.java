package com.x2xcoin.wallet.core.crypto;

import com.x2xcoin.wallet.core.wallet.WalletAccount;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Base58AndWalletTest {
    @Test
    void decodesVersion3P2pkhAddress() {
        String address = "2NHBXKyRY4ZBvyfyuZ2fZvqaGyo89vMGFW";
        byte[] hash160 = AddressCodec.decodeHash160(address);
        assertEquals("65a16059864a2fdbc7c99a4723a8395bc6f188eb", HexUtils.toHex(hash160));
        assertEquals(address, Base58.encodeCheck((byte) 0x03, hash160));
        assertTrue(AddressCodec.isValidP2pkh(address));
    }

    @Test
    void roundTripsCompressedWif() {
        String wif = "Kz6UJmQACJmLtaQj5A3JAge4kVTNQ8gbvXuwbmCj7bsaabudb3RD";
        BigInteger privateKey = WifCodec.decode(wif);
        assertEquals("55c9bccb9ed68446d1b75273bbce89d7fe013a8acd1625514420fb2aca1a21c4", privateKey.toString(16));
        assertEquals(wif, WifCodec.encode(privateKey, true));
        WalletAccount account = WalletAccount.fromWif("test", wif);
        assertEquals(AddressCodec.fromPrivateKey(privateKey), account.address());
        assertTrue(AddressCodec.isValidP2pkh(account.address()));
    }

    @Test
    void createsDeterministicAddressFromKnownHash160() {
        String address = "2NxsdtnTFDZMLKsz1Ceh1fNFFt7HBxARpz";
        assertEquals("6d23156cbbdcc82a5a47eee4c2c7c583c18b6bf4", HexUtils.toHex(AddressCodec.decodeHash160(address)));
        assertTrue(AddressCodec.isValidP2pkh(address));
    }
}
