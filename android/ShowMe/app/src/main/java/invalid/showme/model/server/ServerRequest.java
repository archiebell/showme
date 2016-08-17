package invalid.showme.model.server;

import android.util.Base64;

import org.acra.ACRA;
import org.whispersystems.libaxolotl.InvalidKeyException;
import org.whispersystems.libaxolotl.ecc.Curve;
import org.whispersystems.libaxolotl.ecc.ECKeyPair;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import invalid.showme.util.BitStuff;
import invalid.showme.util.CryptoUtil;
import invalid.showme.util.JSONStuff;
import invalid.showme.util.TimeUtil;

public class ServerRequest
{
    public String URL;
    public String Method;
    private byte[] data;

    public ServerRequest(String url) {
        this.URL = url;
        this.Method = "POST";
        this.data = new byte[0];
    }
    public ServerRequest(String url, byte[] d) {
        this(url);
        if(d != null)
            this.data = d;
    }

    public byte[] getSignedRequest(X509Certificate cert, ECKeyPair idkey) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] encoded = cert.getEncoded();
            md.update(encoded);
            byte[] certFingerprint = md.digest();
            if(certFingerprint.length != 32)
                throw new IllegalArgumentException("Digest length wrong length");

            Long now = TimeUtil.GetNow();
            byte[] timestampStr = now.toString().getBytes();
            if(timestampStr.length != 10)
                throw new IllegalArgumentException("Timestamp length wrong length");

            byte[] signatureData = new byte[1 + 10 + 32];
            signatureData[0] = CryptoUtil.SIGNATURE_REGISTRY_CERTIFICATE;
            System.arraycopy(timestampStr, 0, signatureData, 1, 10);
            System.arraycopy(certFingerprint, 0, signatureData, 1+10, 32);

            byte[] signature = Curve.calculateSignature(idkey.getPrivateKey(), signatureData);

            Map<String, Object> json = new HashMap<>();
            json.put("signature", Base64.encodeToString(signature, Base64.NO_WRAP));
            json.put("signatureVersion", CryptoUtil.SIGNATURE_REGISTRY_CERTIFICATE);
            json.put("identityKey", BitStuff.toHexString(idkey.getPublicKey().serialize()).substring(2).toUpperCase());//libaxolotl serialization with 0x05 for DJBType
            json.put("timestamp", now);
            json.put("data", Base64.encodeToString(this.data, Base64.NO_WRAP));

            //json.put("certFingerprint", BitStuff.toHexString(certFingerprint));
            //json.put("signatureData", BitStuff.toHexString(signatureData));
            return JSONStuff.MapToJSON(json).getBytes();
        } catch (CertificateEncodingException|NoSuchAlgorithmException|InvalidKeyException e) {
            ACRA.getErrorReporter().handleException(e);
            return null;
        }
    }
}
