package invalid.showme.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeUtil
{
    public static long GetNow() {
        return System.currentTimeMillis() / 1000L;
    }

    public static boolean MoreThanOneHourAgo(long timestamp) {
        return timestamp - GetNow() > 60 * 60;
    }

    private static Date FromUnix(long timestamp) {
        return new java.util.Date(timestamp *1000);
    }

    public static String ToFriendlyWords(long timestamp) {
        return new SimpleDateFormat("hh:mm:ss aaa EEE, MMM d yyyy").format(FromUnix(timestamp));
    }

}
