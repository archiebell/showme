package invalid.showme.model.server;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import org.acra.ACRA;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.asn1.x509.X509ObjectIdentifiers;
import org.spongycastle.jcajce.provider.util.AsymmetricKeyInfoConverter;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.whispersystems.libaxolotl.ecc.ECKeyPair;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import info.guardianproject.netcipher.proxy.OrbotHelper;
import invalid.showme.activities.SharedActivityLogic;
import invalid.showme.exceptions.StrangeUsageException;
import invalid.showme.model.UserProfile;
import invalid.showme.model.db.DBHelper;
import invalid.showme.model.server.clientcert.ClientCertificate;
import invalid.showme.model.server.result.GetResult;
import invalid.showme.model.server.result.PollResult;
import invalid.showme.model.server.result.PutResult;
import invalid.showme.model.server.result.ServerResultEvent;
import invalid.showme.model.server.result.StatusResult;
import invalid.showme.util.BitStuff;
import invalid.showme.util.IOUtil;
import invalid.showme.util.JSONStuff;

public class ServerInterface
{
    static final private String TAG = "ServerInterface";

    private static ECKeyPair identityKey;
    private static KeyPair certificateKey;
    private static X509Certificate certificate;
    private static boolean clientCertificateDirty;

    public static Proxy torProxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", 9050));
    private static KeyStore keyStore;
    private static KeyManager[] keyManagers;
    private static TrustManager[] PinnedSSLCertificateChecking = new TrustManager[] { new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
            ACRA.getErrorReporter().handleException(new StrangeUsageException("Somehow wound up in getAcceptedIssuers?"));
            return null;
        }
        @Override
        public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
            ACRA.getErrorReporter().handleException(new StrangeUsageException("Somehow wound up in checkClientTrusted?"));
        }
        @Override
        public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
            if (arg0.length != 1) {
                throw new CertificateException("do not allow certificate chains.");
                /*
                TODO: Support certificate chains.  Carefully.
                Code vulnerable. Consider:
                Custom Leaf signed by
                |- Custom Root signed by
                |- CA-signed Leaf for domain signed by
                |- CA-intermediate signed by
                |- CA-root
                a valid chain of signatures, but can't trust java to not build a connection using CA-signed leaf for domain

                //Confirm everything signs prior
                for(int i=0; i<arg0.length-1; i++) {
                    try {
                        arg0[i].verify(arg0[i + 1].getPublicKey());
                    } catch (NoSuchAlgorithmException|InvalidKeyException|NoSuchProviderException|SignatureException e) {
                        throw new CertificateException("Certificate chain not clean.");
                    }
                }
                */
            }
            //Copy https://github.com/moxie0/AndroidPinning/blob/master/src/org/thoughtcrime/ssl/pinning/PinningTrustManager.java#L114
            final MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("SHA-256");
                final byte[] spki = arg0[0].getPublicKey().getEncoded();
                final byte[] pin = digest.digest(spki);

                if(!BitStuff.CompareArrays(pin, ServerConfiguration.SERVER_PIN))
                    throw new CertificateException("Server pin does not match");
            } catch (NoSuchAlgorithmException e) {
                ACRA.getErrorReporter().handleException(new StrangeUsageException("Somehow don't have SHA-256??"));
                throw new CertificateException("Somehow don't have SHA-256??");
            }
        }
    } };

    private static UserProfile context;
    public static void setContext(Context context) {
        ServerInterface.context = (UserProfile)context;
    }

    public static void setIdentity(ECKeyPair identityKey) {
        ServerInterface.identityKey = identityKey;
    }

    public static boolean clientCertificateDirty() {
        return clientCertificateDirty || ServerInterface.certificate == null || ServerInterface.certificateKey == null;
    }

    public static void setCertificate(ClientCertificate cert) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, UnrecoverableKeyException {
        try {
            cert.DeleteFromDatabase(DBHelper.getInstance(ServerInterface.context));
        } catch (Exception e) {
            if(!IOUtil.isJUnitTest())
                ACRA.getErrorReporter().handleException(e);
        }
        ServerInterface.certificate = cert.cert;
        ServerInterface.certificateKey = cert.keyPair;
        ServerInterface.clientCertificateDirty = false;

        ServerInterface.keyStore = KeyStore.getInstance("PKCS12");
        ServerInterface.keyStore.load(null, null);
        Certificate[] certArray = { ServerInterface.certificate};
        BouncyCastleProvider provider = (BouncyCastleProvider) Security.getProvider("SC");

        AsymmetricKeyInfoConverter keyFactory = new org.spongycastle.jcajce.provider.asymmetric.rsa.KeyFactorySpi();
        for(ASN1ObjectIdentifier oid : new ASN1ObjectIdentifier[]{PKCSObjectIdentifiers.rsaEncryption, X509ObjectIdentifiers.id_ea_rsa})
            provider.addKeyInfoConverter(oid, keyFactory);

        KeyStore.PrivateKeyEntry pkEntry = new KeyStore.PrivateKeyEntry(ServerInterface.certificateKey.getPrivate(), certArray);

        //no security not important
        KeyStore.ProtectionParameter protParam = new KeyStore.PasswordProtection(new char[] { '!' });
        ServerInterface.keyStore.setEntry("clientKey", pkEntry, protParam);

        KeyManagerFactory kmf;
        kmf = KeyManagerFactory.getInstance("X509");
        kmf.init(ServerInterface.keyStore, null);
        ServerInterface.keyManagers = kmf.getKeyManagers();
    }

    //Public for testing
    public static ServerResponse sendRequest(ServerRequest request) {
        int responseCode = 999;
        byte[] result = null;
        SSLContext sslContext;
        HttpURLConnection urlConnection = null;

        if(ServerInterface.identityKey == null || ServerInterface.certificateKey == null || ServerInterface.certificate == null || ServerInterface.keyStore == null) {

            if(!ServerInterface.context.Initialized())
            {
                if(!ServerInterface.context.Initialize())
                    return new ServerResponse(998, "Failed to initialize ServerInterface inside of sendRequest.");
                else if(ServerInterface.certificateKey == null || ServerInterface.certificate == null || ServerInterface.keyStore == null)
                    return new ServerResponse(998, "Successfully initialized ServerInterface inside of sendRequest, but members are STILL null!");
            }
            else
                return new ServerResponse(998, "Somehow ServerInterface members null when *were* initialized.");
        }
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(ServerInterface.keyManagers, ServerInterface.PinnedSSLCertificateChecking, null);

            URL requestedUrl = new URL(request.URL);
            if(PreferenceManager.getDefaultSharedPreferences(ServerInterface.context).getBoolean("pref_usetor", false)) {
                urlConnection = (HttpURLConnection) requestedUrl.openConnection(torProxy);
                if (!OrbotHelper.isOrbotInstalled(context)) {
                    //TODO: How did hit?
                    SharedActivityLogic.PromptForOrbot(context);
                } else if (!OrbotHelper.isOrbotRunning(context)) {
                    OrbotHelper.requestStartTor(context);
                    int i = -1;
                    do {
                        //sleep arbitrary time for orbot
                        Thread.sleep(5000);
                        i++;
                    }while(i < 6 && !OrbotHelper.isOrbotRunning(context));
                }
            }
            else
                urlConnection = (HttpURLConnection) requestedUrl.openConnection();
            if(urlConnection instanceof HttpsURLConnection) {
                ((HttpsURLConnection)urlConnection)
                        .setSSLSocketFactory(sslContext.getSocketFactory());
            }
            else {
                ACRA.getErrorReporter().handleException(new StrangeUsageException("Visiting non-SSL URL: " + request.URL));
            }
            urlConnection.setRequestMethod(request.Method);
            urlConnection.setConnectTimeout(10000);
            urlConnection.setReadTimeout(10000);
            urlConnection.setRequestProperty("Content-Type", "application/octet-stream");

            byte[] postBody = request.getSignedRequest(ServerInterface.certificate, ServerInterface.identityKey);
            urlConnection.setRequestProperty("Content-Length", Integer.toString(postBody.length));
            if(postBody != null) {
                DataOutputStream wr = new DataOutputStream( urlConnection.getOutputStream());
                wr.write( postBody );
            }
            responseCode = urlConnection.getResponseCode();
            result = IOUtil.readFully(urlConnection.getInputStream());
        } catch(Exception e) {
            Log.e(TAG, "got another, unexpected exception in sendRequest: " + e.toString());
            ACRA.getErrorReporter().handleException(e);
        } finally {
            if(urlConnection != null) {
                urlConnection.disconnect();
            }
            ServerInterface.clientCertificateDirty = true;
        }
        return new ServerResponse(responseCode, result);
    }

    public static ServerResponse Register() throws FileNotFoundException {
        String url = ServerConfiguration.ROOT_URL + "/v1/register";
        return sendRequest(new ServerRequest(url));
    }

    public static ServerResponse Token(String id) throws FileNotFoundException {
        String url = ServerConfiguration.ROOT_URL + "/v1/token/" + id;
        return sendRequest(new ServerRequest(url));
    }

    public static ServerResponse Deregister() throws FileNotFoundException {
        String url = ServerConfiguration.ROOT_URL + "/v1/deregister";
        return sendRequest(new ServerRequest(url));
    }

    public static ServerResultEvent Poll() throws FileNotFoundException {
        String url = ServerConfiguration.ROOT_URL + "/v1/poll";
        ServerResponse result = sendRequest(new ServerRequest(url));
        if(result.code != 200)
            return new PollResult(false, result.code, result.response);
        else {
            try {
                List<String> l = JSONStuff.JSONArrayToStringArray(new String(result.response, "UTF-8"));
                return new PollResult(true, result.code, l);
            } catch (Exception e) {
                Log.e(TAG, "could not parse server response to JSON in Poll()");
                return new PollResult(false, result.code, result.response);
            }
        }
    }

    public static ServerResultEvent Get(String id) throws FileNotFoundException {
        String url = ServerConfiguration.ROOT_URL + "/v1/get/" + id;
        ServerResponse result = sendRequest(new ServerRequest(url));
        return new GetResult(result.code == 200, result.code, id, result.response);
    }

    public static StatusResult Status(List<String> ids) throws FileNotFoundException, UnsupportedEncodingException {
        String url = ServerConfiguration.ROOT_URL + "/v1/status";
        ServerResponse result = sendRequest(new ServerRequest(url, IOUtil.join(ids).getBytes("UTF-8")));
        if(result.code != 200)
            return new StatusResult(false, result.code, result.response);
        else {
            try {
                Map<String, String> l = JSONStuff.JSONObjectToMap(new String(result.response, "UTF-8"));
                return new StatusResult(true, result.code, l);
            } catch (Exception e) {
                Log.e(TAG, "could not parse server response to JSON in Status()");
                return new StatusResult(false, result.code, result.response);
            }
        }
    }

    public static ServerResponse Delete(String id) throws FileNotFoundException {
        String url = ServerConfiguration.ROOT_URL + "/v1/delete/" + id;
        return sendRequest(new ServerRequest(url));
    }

    public static PutResult Put(byte[] data) throws FileNotFoundException {
        String url = ServerConfiguration.ROOT_URL + "/v1/put";
        ServerResponse result = sendRequest(new ServerRequest(url, data));
        if(result.code != 200)
            return new PutResult(false, result.code, result.response);
        else {
            try {
                String messageID = JSONStuff.ExtractKeyFromJSONDictionary(new String(result.response, "UTF-8"), "messageID");
                if(messageID != null)
                    return new PutResult(true, result.code, messageID);
                else
                    return new PutResult(false, result.code, result.response);
            } catch (Exception e) {
                Log.e(TAG, "could not parse server response to JSON in Put()");
                return new PutResult(false, result.code, result.response);
            }
        }
    }
}
