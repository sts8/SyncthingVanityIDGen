package io.github.sts8.vanitygen.crypto;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

/**
 * A specialized generator for creating Ed25519 key pairs compatible with Syncthing.
 * <p>
 * This class leverages the Bouncy Castle provider to generate Edwards-curve
 * keys (Ed25519), which offer high security and excellent performance during
 * the brute-force generation process.
 */
public class SyncthingKeyPairGenerator {

    /**
     * The Bouncy Castle security provider name.
     */
    private static final String BOUNCY_CASTLE = "BC";

    /**
     * The Edwards-curve Digital Signature Algorithm (Ed25519).
     */
    private static final String ALGORITHM = "Ed25519";

    private final KeyPairGenerator kpg;

    /**
     * Initializes the key pair generator using the Ed25519 algorithm.
     *
     * @throws NoSuchAlgorithmException if the Ed25519 algorithm is not supported by the provider
     * @throws NoSuchProviderException  if the Bouncy Castle ("BC") provider is not registered
     */
    public SyncthingKeyPairGenerator() throws NoSuchAlgorithmException, NoSuchProviderException {
        kpg = KeyPairGenerator.getInstance(ALGORITHM, BOUNCY_CASTLE);
    }

    /**
     * Generates a new {@link KeyPair}.
     * <p>
     * Because this is called repeatedly in a brute-force loop, this method
     * is optimized for high-frequency execution.
     *
     * @return a freshly generated Ed25519 key pair
     */
    public KeyPair generate() {
        return kpg.generateKeyPair();
    }
}
