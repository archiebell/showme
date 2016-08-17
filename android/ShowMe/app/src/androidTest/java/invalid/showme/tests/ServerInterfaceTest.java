package invalid.showme.tests;

import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.whispersystems.libaxolotl.ecc.ECKeyPair;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import invalid.showme.model.server.ServerConfiguration;
import invalid.showme.model.server.ServerInterface;
import invalid.showme.model.server.ServerRequest;
import invalid.showme.model.server.ServerResponse;
import invalid.showme.model.server.clientcert.ClientCertificate;
import invalid.showme.util.KeyUtil;

public class ServerInterfaceTest extends TestCaseWithUtils
{
    public void test404() throws NoSuchAlgorithmException, UnrecoverableKeyException, CertificateException, KeyStoreException, IOException {
        Security.addProvider(new BouncyCastleProvider());

        ECKeyPair idKey1 = getRandomECCKey();
        KeyPair kp1 = getRandomRSAKey();
        X509Certificate cert1 = KeyUtil.CreateCertificate(kp1);

        ServerInterface.setIdentity(idKey1);
        ServerInterface.setCertificate(new ClientCertificate(kp1, cert1));

        ECKeyPair idKey2 = getRandomECCKey();
        KeyPair kp2 = getRandomRSAKey();
        X509Certificate cert2 = KeyUtil.CreateCertificate(kp2);

        ServerInterface.setIdentity(idKey2);
        ServerInterface.setCertificate(new ClientCertificate(kp2, cert2));

        ServerResponse res = ServerInterface.sendRequest(new ServerRequest(ServerConfiguration.ROOT_URL));
        assertEquals(res.code, 404);
        assertEquals(res.response, null);
    }
    public void test_register_deregister() throws NoSuchAlgorithmException, UnrecoverableKeyException, CertificateException, KeyStoreException, IOException {
        Security.addProvider(new BouncyCastleProvider());

        ECKeyPair idKey = getRandomECCKey();
        KeyPair kp = getRandomRSAKey();
        X509Certificate cert = KeyUtil.CreateCertificate(kp);

        ServerInterface.setIdentity(idKey);
        ServerInterface.setCertificate(new ClientCertificate(kp, cert));

        ServerResponse res = ServerInterface.Deregister();
        assertEquals(200, res.code);

        res = ServerInterface.Register();
        assertEquals(201, res.code);

        res = ServerInterface.Register();
        assertEquals(200, res.code);

        res = ServerInterface.Deregister();
        assertEquals(204, res.code);

        res = ServerInterface.Deregister();
        assertEquals(200, res.code);
    }
}
