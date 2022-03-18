package com.ashelkov.owg.wallet.generators;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.bitcoinj.core.Bech32;
import org.web3j.crypto.Bip32ECKeyPair;
import org.web3j.crypto.Hash;

import com.ashelkov.owg.address.BIP44Address;
import com.ashelkov.owg.address.BIP84Address;
import com.ashelkov.owg.wallet.BitcoinWallet;
import com.ashelkov.owg.wallet.util.EncodingUtils;

import static com.ashelkov.owg.bip.Constants.CHECKSUM_LENGTH;
import static com.ashelkov.owg.bip.Constants.HARDENED;

public class BitcoinWalletGenerator extends ACIWalletGenerator {

    private static final String BECH32_HRP = "bc";
    private static final byte WITNESS_VERSION = (byte)0x00;
    private static final byte BTC_IDENTIFICATION_PREFIX = (byte)0x80;
    private static final int XPUB_VERSION = 0x04b24746;
    // https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki#Serialization_format
    private static final int XPUB_CORE_LENGTH = 78;

    public static String generatePrivateKey(byte[] rawKeyBytes, byte blockchainIdPrefix) {
        byte[] privKeyBase = new byte[34];
        byte[] privKeyBytes = new byte[38];

        System.arraycopy(rawKeyBytes, 0, privKeyBase, 0, 33);
        privKeyBase[0] = blockchainIdPrefix;
        privKeyBase[33] = (byte)0x01;

        System.arraycopy(privKeyBase, 0, privKeyBytes, 0, 34);
        System.arraycopy(Hash.sha256(Hash.sha256(privKeyBase)), 0, privKeyBytes, 34, CHECKSUM_LENGTH);

        return EncodingUtils.base58Bitcoin(privKeyBytes);
    }

    private final Bip32ECKeyPair masterKeyPair;

    public BitcoinWalletGenerator(byte[] seed, boolean genPrivKey, boolean genPubKey) {
        super(genPrivKey, genPubKey);
        this.masterKeyPair = Bip32ECKeyPair.generateKeyPair(seed);
    }

    @Override
    public BitcoinWallet generateDefaultWallet() {

        BIP84Address masterPubKey = generateExtendedKey(DEFAULT_FIELD_VAL);
        List<BIP44Address> wrapper = new ArrayList<>(1);
        wrapper.add(generateDerivedAddress(DEFAULT_FIELD_VAL, DEFAULT_FIELD_VAL, DEFAULT_FIELD_VAL));

        return new BitcoinWallet(masterPubKey, wrapper);
    }

    @Override
    public BitcoinWallet generateWallet(int account, int change, int index, int numAddresses) {

        BIP84Address masterPubKey = generateExtendedKey(account);

        List<BIP44Address> derivedAddresses = new ArrayList<>(numAddresses);
        for(int i = index; i < (index + numAddresses); ++i) {
            derivedAddresses.add(generateDerivedAddress(account, change, i));
        }

        return new BitcoinWallet(masterPubKey, derivedAddresses);
    }

    private BIP84Address generateDerivedAddress(int account, int change, int index) {

        int[] addressPath = getDerivedAddressPath(account, change, index);
        Bip32ECKeyPair derivedKeyPair = Bip32ECKeyPair.deriveKeyPair(masterKeyPair, addressPath);

        byte[] unencodedAddress = EncodingUtils.to5BitBytesSafe(
                Hash.sha256hash160(
                        derivedKeyPair
                                .getPublicKeyPoint()
                                .getEncoded(true)));
        byte[] unencodedAddressWithWitness = new byte[unencodedAddress.length + 1];
        unencodedAddressWithWitness[0] = WITNESS_VERSION;
        System.arraycopy(unencodedAddress, 0, unencodedAddressWithWitness, 1, unencodedAddress.length);

        String address = Bech32.encode(Bech32.Encoding.BECH32, BECH32_HRP, unencodedAddressWithWitness);

        String privKeyText = null;
        String pubKeyText = null;
        if (genPrivKey) {
            privKeyText = generatePrivateKey(derivedKeyPair.getPrivateKeyBytes33(), BTC_IDENTIFICATION_PREFIX);
        }
        if (genPubKey) {
            pubKeyText = EncodingUtils.bytesToHex(derivedKeyPair.getPublicKeyPoint().getEncoded(true));
        }

        return new BIP84Address(address, addressPath, privKeyText, pubKeyText);
    }

    private BIP84Address generateExtendedKey(int account) {

        int[] addressPath = getAccountAddressPath(account);
        Bip32ECKeyPair accountKeyPair = Bip32ECKeyPair.deriveKeyPair(masterKeyPair, addressPath);

        ByteBuffer xpubKeyBuilder = ByteBuffer.allocate(XPUB_CORE_LENGTH);
        xpubKeyBuilder.putInt(XPUB_VERSION);
        xpubKeyBuilder.put((byte)(accountKeyPair.getDepth()));
        xpubKeyBuilder.putInt(accountKeyPair.getParentFingerprint());
        xpubKeyBuilder.putInt(addressPath[2]);
        xpubKeyBuilder.put(accountKeyPair.getChainCode());
        xpubKeyBuilder.put(accountKeyPair.getPublicKeyPoint().getEncoded(true));
        byte[] xpubKeyCore = xpubKeyBuilder.array();
        byte[] checksum = Hash.sha256(Hash.sha256(xpubKeyCore));

        byte[] xpubKey = new byte[XPUB_CORE_LENGTH + CHECKSUM_LENGTH];
        System.arraycopy(xpubKeyCore, 0, xpubKey, 0, XPUB_CORE_LENGTH);
        System.arraycopy(checksum, 0, xpubKey, XPUB_CORE_LENGTH, CHECKSUM_LENGTH);

        String xpubKeySerialized = EncodingUtils.base58Bitcoin(xpubKey);

        return new BIP84Address(xpubKeySerialized, addressPath);
    }

    private int[] getAccountAddressPath(int account) {
        int purpose = BitcoinWallet.PURPOSE | HARDENED;
        int coinCode = BitcoinWallet.COIN.getCode() | HARDENED;

        return new int[] {purpose, coinCode, account | HARDENED};
    }

    private int[] getDerivedAddressPath(int account, int change, int index) {
        int purpose = BitcoinWallet.PURPOSE | HARDENED;
        int coinCode = BitcoinWallet.COIN.getCode() | HARDENED;

        return new int[] {purpose, coinCode, account | HARDENED, change, index};
    }
}
