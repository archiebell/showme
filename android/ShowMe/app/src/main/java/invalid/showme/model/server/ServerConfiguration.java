package invalid.showme.model.server;

import android.util.Base64;

public class ServerConfiguration
{
    final public static String ROOT_URL = "https://127.0.0.1:8082";

    //Set to true if below bugreport url functioning and configured for ACRA
    final public static Boolean REPORT_BUGS = true;
    final public static String BUG_URL = "https://127.0.0.1:8083/v1/bugreport";

    //https://tools.ietf.org/html/rfc7469#appendix-A
    //Currently only allow pinning leaf
    final public static byte[] SERVER_PIN = Base64.decode("", 0);

    //Create app/src/main/res/raw/acra_cert.pem with leaf certificate for bug endpoint

    //Project ID from Google GCM, in google-services.json
    final public static String GOOGLE_PROJECT_ID = "";
}
