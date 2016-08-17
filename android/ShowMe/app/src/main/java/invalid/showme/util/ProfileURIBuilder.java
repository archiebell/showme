package invalid.showme.util;

import android.util.Base64;
import android.util.Log;

import org.acra.ACRA;
import org.whispersystems.libaxolotl.IdentityKey;
import org.whispersystems.libaxolotl.InvalidKeyException;
import org.whispersystems.libaxolotl.ecc.Curve;
import org.whispersystems.libaxolotl.ecc.ECPublicKey;
import org.whispersystems.libaxolotl.state.PreKeyBundle;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.InvalidParameterException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import invalid.showme.model.Friend;
import invalid.showme.model.IFriend;

public class ProfileURIBuilder
{
    private final static String TAG = "ProfileURIBuilder";

    public static String buildURI(String displayName, PreKeyBundle bundle) {
        String uri = "showme:"; //Scheme

        try {
            uri += "v1?n=";
            uri += URLEncoder.encode(displayName, "UTF-8");
            uri += "&rid=";
            uri += Integer.toString(bundle.getRegistrationId());
            uri += "&did=";
            uri += Integer.toString(bundle.getDeviceId());
            uri += "&k=";
            uri += Base64.encodeToString(bundle.getIdentityKey().getPublicKey().serialize(), Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE);
            uri += "&pkid=";
            uri += Integer.toString(bundle.getPreKeyId());
            uri += "&pk=";
            uri += Base64.encodeToString(bundle.getPreKey().serialize(), Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE);
            uri += "&skid=";
            uri += Integer.toString(bundle.getSignedPreKeyId());
            uri += "&sk=";
            uri += Base64.encodeToString(bundle.getSignedPreKey().serialize(), Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE);
            uri += "&sksig=";
            uri += Base64.encodeToString(bundle.getSignedPreKeySignature(), Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Got UnsupportedEncodingException in buildUri");
            ACRA.getErrorReporter().handleException(e);
        }

        return uri;
    }

    private static Pattern URIRegex = Pattern.compile("^showme:v1\\?n=([a-zA-Z0-9\\.\\-\\*_\\+!@#\\(\\)\\[\\]\\{\\}\\$%\\^&'\"]+)&rid=([0-9]+)&did=([0-9]+)&k=([a-zA-Z0-9\\-_\\+]+)&pkid=([0-9]+)&pk=([a-zA-Z0-9\\-_\\+]+)&skid=([0-9]+)&sk=([a-zA-Z0-9\\-_\\+]+)&sksig=([a-zA-Z0-9\\-_\\+]+)");
    public static IFriend parseURI(String uri) throws UnsupportedEncodingException, InvalidParameterException, InvalidKeyException {
        Matcher m = URIRegex.matcher(uri);
        if(!m.find()) {
            Log.e(TAG, "URI did not match expected regex in parseURI");
            throw new InvalidParameterException("URI did not match expected regex:" + uri);
        }

        String displayName = URLDecoder.decode(m.group(1), "UTF-8");

        int regID = Integer.valueOf(m.group(2));
        int devID = Integer.valueOf(m.group(3));

        String encodedPubKey = m.group(4);
        byte[] keyBytes = Base64.decode(encodedPubKey, Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE);
        ECPublicKey pubKey = Curve.decodePoint(keyBytes, 0);

        int pkID = Integer.valueOf(m.group(5));
        String encodedPreKey = m.group(6);
        byte[] preKeyBytes = Base64.decode(encodedPreKey, Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE);
        ECPublicKey preKey = Curve.decodePoint(preKeyBytes, 0);

        int spkID = Integer.valueOf(m.group(7));
        String encodedSignedPreKey = m.group(8);
        byte[] signedPreKeyBytes = Base64.decode(encodedSignedPreKey, Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE);
        ECPublicKey signedPreKey = Curve.decodePoint(signedPreKeyBytes, 0);

        String signedPreKeySignature = m.group(9);
        byte[] signedPreKeySignatureBytes = Base64.decode(signedPreKeySignature, Base64.NO_WRAP | Base64.NO_PADDING | Base64.URL_SAFE);

        PreKeyBundle bundle = new PreKeyBundle(regID, devID, pkID, preKey, spkID, signedPreKey, signedPreKeySignatureBytes, new IdentityKey(pubKey));

        return new Friend(displayName, bundle);
    }
}

