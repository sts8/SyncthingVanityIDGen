package io.github.sts8.vanitygen.crypto;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

public class SyncthingKeyPairGenerator {
    private static final String BOUNCY_CASTLE = "BC";
    private static final String ALGORITHM = "Ed25519";

    private final KeyPairGenerator kpg;

    public SyncthingKeyPairGenerator() throws NoSuchAlgorithmException, NoSuchProviderException {
        kpg = KeyPairGenerator.getInstance(ALGORITHM, BOUNCY_CASTLE);
    }

    public KeyPair generate() {
        return kpg.generateKeyPair();
    }
}
