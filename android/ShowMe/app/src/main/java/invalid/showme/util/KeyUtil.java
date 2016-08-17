package invalid.showme.util;

import android.util.Log;

import org.acra.ACRA;
import org.spongycastle.asn1.x509.X509Name;
import org.spongycastle.x509.X509V3CertificateGenerator;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;
import java.util.Random;

public class KeyUtil
{
    private final static String TAG = "KeyUtil";

    public static byte[] GetModulusBytes(PublicKey k)
    {
        try {
            KeyFactory keyFac = KeyFactory.getInstance("RSA");
            RSAPublicKeySpec pkSpec = null;
                pkSpec = keyFac.getKeySpec(k, RSAPublicKeySpec.class);
            BigInteger m = pkSpec.getModulus();
            return m.toByteArray();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "In GetModulusBytes caught NoSuchAlgorithmException");
            ACRA.getErrorReporter().handleException(e);
        } catch (InvalidKeySpecException e) {
            Log.e(TAG, "In GetModulusBytes caught InvalidKeySpecException");
            ACRA.getErrorReporter().handleException(e);
        }
        return null;
    }
    public static PublicKey BytesToModulus(byte[] b)
    {
        try {
            //SEC: will break if E != 0x10001
            BigInteger m = new BigInteger(b);
            BigInteger e = new BigInteger(new byte[] { 0x01, 0x00, 0x01});
            RSAPublicKeySpec spec = new RSAPublicKeySpec(m, e);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(spec);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "In BytesToModulus caught NoSuchAlgorithmException");
            ACRA.getErrorReporter().handleException(e);
        } catch (InvalidKeySpecException e) {
            Log.e(TAG, "In BytesToModulus caught InvalidKeySpecException");
            ACRA.getErrorReporter().handleException(e);
        }

        return null;
    }

    public static X509Certificate BytesToX509Cert(byte[] bytes) {
        try {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            InputStream in = new ByteArrayInputStream(bytes);
            return (X509Certificate)certFactory.generateCertificate(in);
        } catch (CertificateException e) {
            Log.e(TAG, "In BytesToX509Cert caught CertificateException");
            ACRA.getErrorReporter().handleException(e);
        }
        return null;
    }

    public static PublicKey BytesToRSAPublicKey(byte[] bytes)
    {
        EncodedKeySpec keySpec = new X509EncodedKeySpec(bytes);
        KeyFactory keyFactory = null;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "In BytesToRSAPublicKey caught NoSuchAlgorithmException");
            ACRA.getErrorReporter().handleException(e);
        }

        PublicKey pubKey = null;
        try {
            pubKey = keyFactory.generatePublic(keySpec);
        } catch (InvalidKeySpecException e) {
            Log.e(TAG, "In BytesToRSAPublicKey caught InvalidKeySpecException");
            ACRA.getErrorReporter().handleException(e);
        } catch (NullPointerException e) {
            Log.e(TAG, "In BytesToRSAPublicKey caught NullPointerException");
            ACRA.getErrorReporter().handleException(e);
        }
        return pubKey;
    }

    public static PrivateKey BytesToRSAPrivateKey(byte[] bytes)
    {
        EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(bytes);
        KeyFactory keyFactory = null;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "In BytesToRSAPrivateKey caught NoSuchAlgorithmException");
        }

        PrivateKey privKey = null;
        try {
            privKey = keyFactory.generatePrivate(keySpec);
        } catch (InvalidKeySpecException e) {
            Log.e(TAG, "In BytesToRSAPrivateKey caught InvalidKeySpecException");
            ACRA.getErrorReporter().handleException(e);
        } catch (NullPointerException e) {
            Log.e(TAG, "In BytesToRSAPrivateKey caught NullPointerException");
            ACRA.getErrorReporter().handleException(e);
        }
        return privKey;
    }

    public static X509Certificate CreateCertificate(KeyPair kp) {
        //TODO: match default OpenSSL attributes for tiny amount of fingerprint resistance
        X509V3CertificateGenerator certGenerator = new X509V3CertificateGenerator();
        certGenerator.setSerialNumber(BigInteger.valueOf(Math.abs(new Random().nextLong())));
        certGenerator.setIssuerDN(new X509Name("CN = Google Internet Authority G2, O = Google Inc, C = US"));
        certGenerator.setSubjectDN(new X509Name("CN = Google Internet Authority G2, O = Google Inc, C = US"));
        certGenerator.setNotBefore(new Date("Jan 1, 1970 00:00"));
        certGenerator.setNotAfter(new Date("Dec 31, 2032 23:59"));
        certGenerator.setPublicKey(kp.getPublic());
        certGenerator.setSignatureAlgorithm("SHA1WithRSAEncryption");
        X509Certificate certificate = null;
        try {
            certificate = certGenerator.generate(kp.getPrivate());
        } catch (CertificateEncodingException e) {
            Log.e(TAG, "In CreateCertificate caught CertificateEncodingException");
            ACRA.getErrorReporter().handleException(e);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "In CreateCertificate caught NoSuchAlgorithmException");
            ACRA.getErrorReporter().handleException(e);
        } catch (SignatureException e) {
            Log.e(TAG, "In CreateCertificate caught SignatureException");
            ACRA.getErrorReporter().handleException(e);
        } catch (InvalidKeyException e) {
            Log.e(TAG, "In CreateCertificate caught InvalidKeySpecException");
            ACRA.getErrorReporter().handleException(e);
        }
        return certificate;
    }


}
