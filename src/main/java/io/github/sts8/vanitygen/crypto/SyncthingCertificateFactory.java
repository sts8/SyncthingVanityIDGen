package io.github.sts8.vanitygen.crypto;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public final class SyncthingCertificateFactory {

    private static final String BOUNCY_CASTLE = "BC";
    private static final JcaX509CertificateConverter CERT_CONVERTER =
            new JcaX509CertificateConverter().setProvider(BOUNCY_CASTLE);

    private static final String SIGNATURE_ALGORITHM = "Ed25519";
    private static final Duration VALIDITY = Duration.ofDays(20 * 365);
    private static final int SERIAL_BITS = 64;
    private static final BasicConstraints BASIC_CONSTRAINTS = new BasicConstraints(false);
    private static final KeyUsage KEY_USAGE = new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment);
    private static final ExtendedKeyUsage EXTENDED_KEY_USAGE = new ExtendedKeyUsage(
            new KeyPurposeId[]{KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth}
    );
    private static final X500Name SUBJECT = new X500NameBuilder(BCStyle.INSTANCE)
            .addRDN(BCStyle.O, "Syncthing")
            .addRDN(BCStyle.OU, "Automatically Generated")
            .addRDN(BCStyle.CN, "syncthing")
            .build();
    private static final GeneralNames SUBJECT_ALT_NAME = new GeneralNames(
            new GeneralName(GeneralName.dNSName, "syncthing")
    );

    private SyncthingCertificateFactory() {
        // utility class
    }

    public static X509Certificate create(KeyPair keyPair)
            throws CertificateException, IOException, OperatorCreationException {

        // Truncate to start of the current day
        Instant truncated = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Date notBefore = Date.from(truncated);
        Date notAfter = Date.from(truncated.plus(VALIDITY));

        BigInteger serial = new BigInteger(SERIAL_BITS, new SecureRandom());

        ContentSigner signer =
                new JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
                        .setProvider(BOUNCY_CASTLE)
                        .build(keyPair.getPrivate());

        X509CertificateHolder holder = new X509v3CertificateBuilder(
                SUBJECT,
                serial,
                notBefore,
                notAfter,
                SUBJECT,
                SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded())
        )
                .addExtension(Extension.subjectAlternativeName, false, SUBJECT_ALT_NAME)
                .addExtension(Extension.basicConstraints, true, BASIC_CONSTRAINTS)
                .addExtension(Extension.extendedKeyUsage, false, EXTENDED_KEY_USAGE)
                .addExtension(Extension.keyUsage, true, KEY_USAGE)

                .build(signer);

        return CERT_CONVERTER.getCertificate(holder);
    }
}
