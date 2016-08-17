package invalid.showme.util;


import java.nio.ByteBuffer;
import java.util.Formatter;

public class BitStuff
{
    private final String TAG = "BitStuff";

    public static String toHexString(byte b)
    {
        StringBuilder sb = new StringBuilder(2);

        Formatter formatter = new Formatter(sb);
        formatter.format("%02X", b);

        return sb.toString();
    }
    public static String toHexString(byte[] bytes)
    {
        StringBuilder sb = new StringBuilder(bytes.length * 2);

        Formatter formatter = new Formatter(sb);
        for (byte b : bytes) {
            formatter.format("%02X", b);
        }

        return sb.toString();
    }
    public static byte[] fromHexString(String hexString)
    {
        byte[] bytes = new byte[(hexString.length()+1) / 2];
        int byte_indx = 0;
        int hex_index = 0;

        if(hexString.length() % 2 == 1) {
            bytes[byte_indx] = (byte)Integer.parseInt(hexString.substring(0, 1), 16);
            byte_indx++;
            hex_index++;
        }

        for(;byte_indx < bytes.length; byte_indx++) {
            bytes[byte_indx] = (byte)Integer.parseInt(hexString.substring(hex_index, hex_index+2), 16);
            hex_index += 2;
        }

        return bytes;
    }

    public static byte[] toByteArray(long value)
    {
        ByteBuffer bb = ByteBuffer.allocate(8);
        return bb.putLong(value).array();
    }

    public static byte[] toByteArray(int value)
    {
        ByteBuffer bb = ByteBuffer.allocate(4);
        return bb.putInt(value).array();
    }

    public static int toInt(byte[] bytes)
    {
        return toInt(bytes, 0);
    }

    public static int toInt(byte[] bytes, int offset)
    {
        return ((bytes[offset++] << 24) & 0xFF000000) + ((bytes[offset++] << 16) & 0x00FF0000) + ((bytes[offset++] << 8) & 0x0000FF00) + ((bytes[offset]) & 0x000000FF);
    }

    public static byte[] substring(byte[] bytes, int start)
    {
        byte[] b = new byte[bytes.length - start];
        System.arraycopy(bytes, start, b, 0, b.length);
        return b;
    }

    public static void copy(byte[] src, int offset, int length, byte[] dst)
    {
        System.arraycopy(src, offset, dst, 0, length);
    }

    public static Boolean CompareArrays(byte[] b1, byte[] b2)
    {
        if(b1.length != b2.length) return false;

        for(int i=0; i<b1.length; i++)
        {
            if(b1[i] != b2[i]) return false;
        }
        return true;
    }
    public static Boolean CompareArrays(int len, byte[] b1, int i1, byte[] b2, int i2)
    {
        if(i1 + len > b1.length) return false;
        if(i2 + len > b2.length) return false;

        for(int i=0; i<len; i++)
        {
            if(b1[i1 + i] != b2[i2 + i]) return false;
        }
        return true;
    }
}
