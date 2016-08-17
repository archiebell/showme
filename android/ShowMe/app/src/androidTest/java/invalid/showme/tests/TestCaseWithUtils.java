package invalid.showme.tests;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.whispersystems.libaxolotl.ecc.Curve;
import org.whispersystems.libaxolotl.ecc.ECKeyPair;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.List;

import invalid.showme.util.KeyUtil;

public class TestCaseWithUtils extends TestCase
{
    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    Boolean CompareArrays(byte[] b1, byte[] b2)
    {
        if(b1.length != b2.length) return false;

        for(int i=0; i<b1.length; i++)
        {
            if(b1[i] != b2[i]) return false;
        }
        return true;
    }

    Boolean CompareArrays(String[] b1, List<String> b2)
    {
        if(b1.length != b2.size()) return false;

        for(int i=0; i<b1.length; i++)
        {
            if(!b1[i].equals(b2.get(i))) return false;
        }
        return true;
    }

    protected X509Certificate getRandomCert()
    {
        KeyPair kp = getRandomRSAKey();
        return KeyUtil.CreateCertificate(kp);
    }

    KeyPair getRandomRSAKey()
    {
        KeyPairGenerator kpg = null;
        try {
            kpg = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        kpg.initialize(2048);
        return kpg.generateKeyPair();
    }

    ECKeyPair getRandomECCKey()
    {
        return Curve.generateKeyPair();
    }

    protected void CompareFiles(File expected, File actual) throws IOException
    {
        Assert.assertNotNull(expected);
        Assert.assertNotNull(actual);

        Assert.assertTrue("File not exist [" + expected.getAbsolutePath() + "]", expected.exists());
        Assert.assertTrue("File not exist [" + actual.getAbsolutePath() + "]", actual.exists());

        Assert.assertTrue("Expected file not readable", expected.canRead());
        Assert.assertTrue("Actual file not readable", actual.canRead());

        Assert.assertEquals(expected.length(), actual.length());

        FileInputStream eis;
        FileInputStream ais;

        eis = new FileInputStream(expected);
        ais = new FileInputStream(actual);

        CompareStreams(eis, ais);
    }
    private void CompareStreams(InputStream expected, InputStream actual) throws IOException
    {
        try {
            int b1_result, b2_result;
            byte[] b1 = new byte[4096];
            byte[] b2 = new byte[4096];
            do {
                b1_result = expected.read(b1);
                b2_result = actual.read(b2);
                Assert.assertEquals(b1_result, b2_result);
                Assert.assertTrue(CompareArrays(b1, b2));
            }while(b1_result != -1);
        }
        catch(Exception e){
            Assert.assertTrue(false);
        }
        finally {
            expected.close();
            actual.close();
        }
    }
}
