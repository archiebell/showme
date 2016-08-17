package invalid.showme.model.server;

import android.util.Base64;
import android.util.Log;

import org.acra.ACRA;

import java.io.UnsupportedEncodingException;

public class ServerResponse
{
    final private static String TAG = "ServerResponse";

    public int code;
    public byte[] response;
    public ServerResponse(int code, byte[] res)
    {
        this.code = code;
        this.response = res;
    }

    public ServerResponse(int code, String res)
    {
        try {
            this.code = code;
            this.response = res.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "not able to converse response from String to UTF-8 byte sequence.");
            ACRA.getErrorReporter().handleException(e);
        }
    }

    public String getResponseAsString()
    {
        try {
            if(this.response == null) return "";
            return new String(this.response, "UTF-8");
        } catch(UnsupportedEncodingException e) {
            return "ERROR: Could not decode string. Base64: " + Base64.encodeToString(this.response, 0);
        }
    }
}
