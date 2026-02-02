package io.github.sts8.vanitygen.model;

import io.github.sts8.vanitygen.crypto.SyncthingCertificateFactory;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.encoders.Base32;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Represents a Syncthing device identity, consisting of a public/private key pair,
 * an X.509 certificate, and a unique Syncthing Device ID.
 * <p>
 * This class handles the derivation of the Device ID from the certificate
 * and provides functionality to export the identity to PEM files.
 */
public class SyncthingIdentity {

    private static final String HASHING_ALGORITHM = "SHA-256";

    private final KeyPair keyPair;
    private final X509Certificate certificate;
    private final String syncthingID;

    /**
     * Creates a new Syncthing identity from a provided key pair.
     * The X.509 certificate is generated automatically using the {@link SyncthingCertificateFactory}.
     *
     * @param keyPair the {@link KeyPair} to use for this identity
     * @throws CertificateException      if certificate generation fails
     * @throws OperatorCreationException if the certificate signer cannot be created
     * @throws IOException               if an I/O error occurs during processing
     * @throws NoSuchAlgorithmException  if the SHA-256 algorithm is not available
     */
    public SyncthingIdentity(KeyPair keyPair)
            throws CertificateException, OperatorCreationException, IOException, NoSuchAlgorithmException {

        this.keyPair = keyPair;
        this.certificate = SyncthingCertificateFactory.create(keyPair);
        this.syncthingID = computeSyncthingID();
    }

    /**
     * Creates a Syncthing identity from an existing certificate and private key.
     *
     * @param certificate the existing {@link X509Certificate}
     * @param privateKey  the corresponding {@link PrivateKey}
     * @throws CertificateEncodingException if the certificate cannot be encoded for ID calculation
     * @throws NoSuchAlgorithmException     if the SHA-256 algorithm is not available
     */
    public SyncthingIdentity(X509Certificate certificate, PrivateKey privateKey)
            throws CertificateEncodingException, NoSuchAlgorithmException {

        this.keyPair = new KeyPair(certificate.getPublicKey(), privateKey);
        this.certificate = certificate;
        this.syncthingID = computeSyncthingID();
    }

    /**
     * Generates a Luhn-32 checksum character for a given string based on the Syncthing/Base32 alphabet.
     *
     * @param string the input string to calculate the checksum for
     * @return the calculated checksum character
     * @see <a href="https://github.com/syncthing/syncthing-java/blob/master/core/src/main/kotlin/net/syncthing/java/core/beans/DeviceId.kt">Syncthing DeviceId Reference</a>
     */
    private static char generateLuhn32Checksum(String string) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        int n = alphabet.length();

        int factor = 1;
        int sum = 0;

        for (char c : string.toCharArray()) {
            int index = alphabet.indexOf(c);
            var add = factor * index;
            factor = factor == 2 ? 1 : 2;
            add = add / n + add % n;
            sum += add;
        }

        int remainder = sum % n;
        int check = (n - remainder) % n;
        return alphabet.charAt(check);
    }

    /**
     * Returns the formatted Syncthing Device ID.
     * <p>
     * The ID is a 63-character string consisting of 8 blocks of 7 characters
     * each (total 56 alphanumeric characters), separated by hyphens.
     *
     * @return the formatted ID (e.g., ABCDEFG-HIJKLMN-PQRSTUV-WXYZ234-...)
     */
    public String getSyncthingID() {
        return syncthingID;
    }

    /**
     * Computes the Syncthing Device ID by hashing the certificate, encoding to Base32,
     * adding Luhn-32 checksums, and formatting into blocks.
     *
     * @return the computed ID string
     * @throws NoSuchAlgorithmException     if SHA-256 is unavailable
     * @throws CertificateEncodingException if the certificate data cannot be retrieved
     */
    private String computeSyncthingID() throws NoSuchAlgorithmException, CertificateEncodingException {

        MessageDigest digest = MessageDigest.getInstance(HASHING_ALGORITHM);
        byte[] certHash = digest.digest(certificate.getEncoded());
        byte[] certHashBase32 = Base32.encode(certHash);

        String str52 = new String(certHashBase32, StandardCharsets.UTF_8).toUpperCase().substring(0, 52);
        String[] blocks = {
                str52.substring(0, 13),
                str52.substring(13, 26),
                str52.substring(26, 39),
                str52.substring(39, 52)
        };

        for (int b = 0; b < 4; b++) {
            blocks[b] = blocks[b] + generateLuhn32Checksum(blocks[b]);
        }

        return String.format("%s-%s-%s-%s-%s-%s-%s-%s",
                blocks[0].substring(0, 7), blocks[0].substring(7),
                blocks[1].substring(0, 7), blocks[1].substring(7),
                blocks[2].substring(0, 7), blocks[2].substring(7),
                blocks[3].substring(0, 7), blocks[3].substring(7)
        );
    }

    /**
     * Writes the identity (certificate and private key) to a directory named after the Device ID.
     * Creates two files: {@code cert.pem} and {@code key.pem}.
     *
     * @param parentDir the directory where the identity folder should be created
     * @throws IOException                  if writing the files fails
     * @throws CertificateEncodingException if the certificate data cannot be encoded
     */
    public void writeToDirectory(Path parentDir)
            throws IOException, CertificateEncodingException {

        Path identityDir = parentDir.resolve(syncthingID);
        Files.createDirectories(identityDir);

        try (PemWriter certWriter = new PemWriter(
                new OutputStreamWriter(
                        Files.newOutputStream(identityDir.resolve("cert.pem")),
                        StandardCharsets.US_ASCII))) {

            certWriter.writeObject(
                    new PemObject("CERTIFICATE", certificate.getEncoded())
            );
        }

        try (PemWriter keyWriter = new PemWriter(
                new OutputStreamWriter(
                        Files.newOutputStream(identityDir.resolve("key.pem")),
                        StandardCharsets.US_ASCII))) {

            keyWriter.writeObject(
                    new PemObject("EC PRIVATE KEY", keyPair.getPrivate().getEncoded())
            );
        }
    }

}
