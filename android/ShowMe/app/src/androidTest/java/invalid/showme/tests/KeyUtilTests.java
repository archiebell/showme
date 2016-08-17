package invalid.showme.tests;

import junit.framework.Assert;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import invalid.showme.util.KeyUtil;

public class KeyUtilTests extends TestCaseWithUtils
{
    public void testKeyConversion() throws NoSuchAlgorithmException {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
        KeyPair kp = getRandomRSAKey();

        byte[] pub = kp.getPublic().getEncoded();
        byte[] priv = kp.getPrivate().getEncoded();

        PublicKey newPub = KeyUtil.BytesToRSAPublicKey(pub);
        assertEquals(kp.getPublic(), newPub);

        PrivateKey newPriv = KeyUtil.BytesToRSAPrivateKey(priv);
        assertEquals(kp.getPrivate(), newPriv);
    }

    public void testModulusConversion() throws NoSuchAlgorithmException {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
        KeyPair kp = getRandomRSAKey();
        PublicKey p = kp.getPublic();

        byte[] b = KeyUtil.GetModulusBytes(p);
        assertNotNull(b);

        PublicKey p2 = KeyUtil.BytesToModulus(b);
        assertEquals(p, p2);
    }

    public void testCertConversion() throws NoSuchAlgorithmException, CertificateEncodingException {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        X509Certificate cert1 = KeyUtil.CreateCertificate(kp);
        X509Certificate cert2 = KeyUtil.BytesToX509Cert(cert1.getEncoded());
        Assert.assertEquals(cert1, cert2);
    }
}
