package io.github.sts8.vanitygen.model;

import io.github.sts8.vanitygen.crypto.SyncthingKeyPairGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SyncthingIdentityTest {
    private static final String BOUNCY_CASTLE = "BC";

    private static String sampleSyncthingId;
    private static X509Certificate sampleCertificate;
    private static PrivateKey samplePrivateKey;

    @BeforeAll
    static void setUp() throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        Path sampleDir = Paths.get("src/test/resources/sample-cert");

        sampleSyncthingId = Files.readString(sampleDir.resolve("syncthing-id.txt")).trim();

        try (InputStream certInput = Files.newInputStream(sampleDir.resolve("cert.pem"))) {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509", BOUNCY_CASTLE);
            sampleCertificate = (X509Certificate) certificateFactory.generateCertificate(certInput);
        }

        try (BufferedReader reader = Files.newBufferedReader(sampleDir.resolve("key.pem"));
             PemReader pemReader = new PemReader(reader)) {

            PemObject pemObject = pemReader.readPemObject();
            byte[] keyBytes = pemObject.getContent();

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("Ed25519");
            samplePrivateKey = keyFactory.generatePrivate(keySpec);
        }
    }

    @Test
    void differentKeyPairsProduceDifferentSyncthingIDs() throws Exception {
        SyncthingKeyPairGenerator generator = new SyncthingKeyPairGenerator();

        SyncthingIdentity identity1 = new SyncthingIdentity(generator.generate());
        SyncthingIdentity identity2 = new SyncthingIdentity(generator.generate());

        assertNotEquals(identity1.getSyncthingID(), identity2.getSyncthingID());
    }

    @Test
    void computeSyncthingID() throws Exception {
        SyncthingIdentity syncthingIdentity = new SyncthingIdentity(sampleCertificate, samplePrivateKey);
        assertEquals(sampleSyncthingId, syncthingIdentity.getSyncthingID());
    }

    @Test
    void writeToDirectoryCreatesValidIdentityFiles() throws Exception {
        SyncthingKeyPairGenerator generator = new SyncthingKeyPairGenerator();
        SyncthingIdentity identity = new SyncthingIdentity(generator.generate());

        Path tempDir = Files.createTempDirectory("syncthing-identity-test");
        String expectedId = identity.getSyncthingID();

        identity.writeToDirectory(tempDir);

        Path identityDir = tempDir.resolve(expectedId);
        Path certPath = identityDir.resolve("cert.pem");
        Path keyPath = identityDir.resolve("key.pem");

        assertTrue(Files.exists(identityDir));
        assertTrue(Files.exists(certPath));
        assertTrue(Files.exists(keyPath));
    }

}
