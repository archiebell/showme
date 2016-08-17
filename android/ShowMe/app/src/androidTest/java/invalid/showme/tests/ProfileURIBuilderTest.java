package invalid.showme.tests;

import junit.framework.Assert;

import org.whispersystems.libaxolotl.IdentityKey;
import org.whispersystems.libaxolotl.InvalidKeyException;
import org.whispersystems.libaxolotl.ecc.Curve;
import org.whispersystems.libaxolotl.ecc.ECKeyPair;
import org.whispersystems.libaxolotl.state.PreKeyBundle;

import java.io.UnsupportedEncodingException;

import invalid.showme.model.IFriend;
import invalid.showme.util.ProfileURIBuilder;

public class ProfileURIBuilderTest extends TestCaseWithUtils
{
    private void URIBuilderHelper(String name) throws UnsupportedEncodingException, InvalidKeyException {
        ECKeyPair kp1 = Curve.generateKeyPair();
        ECKeyPair kp2 = Curve.generateKeyPair();
        ECKeyPair kp3 = Curve.generateKeyPair();

        PreKeyBundle bundle = new PreKeyBundle(1, 2, 3, kp2.getPublicKey(), 4, kp3.getPublicKey(), new byte[] { 0x13, 0x37}, new IdentityKey(kp1.getPublicKey()));

        String uri = ProfileURIBuilder.buildURI(name, bundle);
        IFriend f = ProfileURIBuilder.parseURI(uri);

        Assert.assertEquals(f.getDisplayName(), name);
        Assert.assertEquals(f.getPreKeyBundle().getRegistrationId(), 1);
        Assert.assertEquals(f.getPreKeyBundle().getDeviceId(), 2);
        Assert.assertEquals(f.getPreKeyBundle().getPreKeyId(), 3);
        Assert.assertEquals(f.getPreKeyBundle().getPreKey(), kp2.getPublicKey());
        Assert.assertEquals(f.getPreKeyBundle().getSignedPreKeyId(), 4);
        Assert.assertEquals(f.getPreKeyBundle().getSignedPreKey(), kp3.getPublicKey());
        Assert.assertTrue(CompareArrays(f.getPreKeyBundle().getSignedPreKeySignature(), new byte[] { 0x13, 0x37}));
        Assert.assertEquals(f.getPublicKey(), kp1.getPublicKey());

    }

    public void testProfileURIBuilder() throws UnsupportedEncodingException, InvalidKeyException {
        URIBuilderHelper("Name");
        URIBuilderHelper("Na+me");
        URIBuilderHelper("Na!me");
        URIBuilderHelper("Na\nme");
        URIBuilderHelper("Na@#)@*$%#*#$)%*#@me");
        URIBuilderHelper("Name");
        URIBuilderHelper("Na?me");
        URIBuilderHelper("NameNameNameNameNameNameNameNameNameNameNameNameNameNameNameNameNameNameNameNameNameNameNameNameNameNameNameNameNameNameNameNameNameNameNameNameNameName");
        URIBuilderHelper("Name\"Name");
        URIBuilderHelper("Name\'Name");
    }
}
