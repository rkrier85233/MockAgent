package com.cleo.prototype.agent;

import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.x509.X509V3CertificateGenerator;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import javax.security.auth.x500.X500Principal;

public class CertificateGenerator {
    private static final AtomicLong ATOMIC_LONG = new AtomicLong(System.currentTimeMillis());

    private static final KeyPairGenerator keyPairGenerator;

    static {
        Security.addProvider(new BouncyCastleProvider());
        try {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
        } catch (Exception e) {
            throw new RuntimeException("Unable to create key pair generator, cause: " + e, e);
        }
        keyPairGenerator.initialize(512, new SecureRandom());
    }

    private CertificateGenerator() {
    }

    public static KeyPair generateKeyPair() {
        return keyPairGenerator.generateKeyPair();
    }

    public static X509Certificate generateX509Certificate() {
        return generateX509Certificate(generateKeyPair());
    }

    public static X509Certificate generateX509Certificate(KeyPair keyPair) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -1);
        Date startDate = calendar.getTime();
        calendar.add(Calendar.YEAR, 1);
        Date expiryDate = calendar.getTime();

        try {
            X500Name issuer = new X500Name("cn=example");
            BigInteger serial = BigInteger.valueOf(ATOMIC_LONG.getAndIncrement());

            X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
            certGen.setSerialNumber(serial);
            certGen.setSubjectDN(new X500Principal(issuer.toString()));
            certGen.setIssuerDN(new X500Principal(issuer.toString())); // use the same
            certGen.setNotBefore(startDate);
            certGen.setNotAfter(expiryDate);
            certGen.setPublicKey(keyPair.getPublic());
            certGen.setSignatureAlgorithm(PKCSObjectIdentifiers.sha256WithRSAEncryption.getId());

            certGen.addExtension(X509Extension.extendedKeyUsage, true, new ExtendedKeyUsage(KeyPurposeId.id_kp_timeStamping));
            return certGen.generate(keyPair.getPrivate(), "BC");
        } catch (Exception e) {
            throw new RuntimeException("Unable to generate certificate, cause: " + e, e);
        }
    }

    public static String generatePemCer() {
        try {
            X509Certificate cert = generateX509Certificate();
            return Base64.toBase64String(cert.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("Unable to generate certificate, cause: " + e, e);
        }
    }
}
