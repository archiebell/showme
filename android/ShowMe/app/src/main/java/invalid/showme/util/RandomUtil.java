package invalid.showme.util;

import android.util.Log;

import java.security.SecureRandom;

public class RandomUtil
{
    private final static String TAG = "RandomUtil";

    private static SecureRandom sr;

    public static void getBytes(byte[] b)
    {
        if(sr == null) sr = new SecureRandom();

        sr.nextBytes(b);

        //TODO: Make check debug-only?
        int length = b.length;
        int numZeros = 0;
        for(int i=0; i<length; i++)
            numZeros += b[i] == 0 ? 1 : 0;
        if(numZeros == length) {
            Log.e(TAG, "Random number generator failure: got all zeros.");
            throw new RuntimeException("Random Number Generator Failure.");
        }
    }

    public static long getRandomNegativeNumber() {
        if(sr == null) sr = new SecureRandom();

        long l = sr.nextLong();
        if(l == 0 || l == -1) return getRandomNegativeNumber();
        else if(l > 0) return -l;
        else return l;
    }
}
