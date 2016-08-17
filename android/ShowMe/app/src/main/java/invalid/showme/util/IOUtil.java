package invalid.showme.util;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import org.acra.ACRA;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class IOUtil
{
    private final static String TAG = "IOUtil";

    public static boolean isJUnitTest() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        List<StackTraceElement> list = Arrays.asList(stackTrace);
        for (StackTraceElement element : list) {
            if (element.getClassName().startsWith("org.junit.")) {
                return true;
            }
        }
        return false;
    }

    public static String intentToString(Intent i)
    {
        Bundle bundle = i.getExtras();
        if (bundle != null) {
            StringBuilder str = new StringBuilder();
            Set<String> keys = bundle.keySet();
            Iterator<String> it = keys.iterator();
            while (it.hasNext()) {
                String key = it.next();
                str.append(key);
                str.append(":");
                str.append(bundle.get(key));
                str.append("\n\r");
            }
            return str.toString();
        }
        return "";
    }

    public static String AppBuildTime(PackageManager pkg, String pkgname) {
        String s = "";
        try{
            ApplicationInfo ai = pkg.getApplicationInfo(pkgname, 0);
            ZipFile zf = new ZipFile(ai.sourceDir);
            ZipEntry ze = zf.getEntry("classes.dex");
            long time = ze.getTime();
            s = SimpleDateFormat.getInstance().format(new java.util.Date(time));
            zf.close();
        }catch(Exception e){
        }
        return s;
    }

    public static String join(Collection<?> s) {
        return join(s, ", ");
    }
    private static String join(Collection<?> s, String delimiter) {
            StringBuilder builder = new StringBuilder();
        Iterator<?> iter = s.iterator();
        while (iter.hasNext()) {
            builder.append(iter.next());
            if (!iter.hasNext()) {
                break;
            }
            builder.append(delimiter);
        }
        return builder.toString();
    }

    public static byte[] readFully(InputStream stream)
    {
        int n = 0;
        byte[] buffer = new byte[1024 * 4];
        ByteArrayOutputStream writer = new ByteArrayOutputStream();
        try {
            while (-1 != (n = stream.read(buffer)))
                writer.write(buffer, 0, n);
        } catch (IOException e) {
            Log.e(TAG, "In readFully() caught IOException");
            ACRA.getErrorReporter().handleException(e);
        }
        return writer.toByteArray();
    }

    public static long streamStreams(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024 * 4];
        long count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }
}
