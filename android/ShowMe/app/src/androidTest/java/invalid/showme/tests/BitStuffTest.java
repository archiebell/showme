package invalid.showme.tests;

import junit.framework.Assert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import invalid.showme.util.BitStuff;

public class BitStuffTest extends TestCaseWithUtils
{
    private byte[] intToBytes(int num)
    {
        return BitStuff.toByteArray(num);
    }

    private byte[] intToBytes(int num, int padBytes) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        byte b = 0x00;
        for(int i=0; i<padBytes; i++)
            out.write(b);

        out.write(BitStuff.toByteArray(num));
        return out.toByteArray();
    }

    public void testToHexString() {
        byte[] b1 = new byte[] { 0x00, 0x01 };
        String s1 = BitStuff.toHexString(b1);
        Assert.assertEquals("0001", s1);

        byte[] b2 = new byte[] {  };
        String s2 = BitStuff.toHexString(b2);
        Assert.assertEquals("", s2);

        byte[] b3 = new byte[] { (byte)0xFE, (byte)0xDE, 0x45, 0x00 };
        String s3 = BitStuff.toHexString(b3);
        Assert.assertEquals("FEDE4500", s3);
    }

    public void testFromHexString() {
        byte[] b1 = new byte[] { 0x00, 0x01 };
        byte[] b2 = BitStuff.fromHexString("0001");
        CompareArrays(b1, b2);

        b1 = new byte[] { 0x00, 0x01 };
        b2 = BitStuff.fromHexString("001");
        CompareArrays(b1, b2);

        b1 = new byte[] { (byte)0xFF, (byte)0xFE };
        b2 = BitStuff.fromHexString("FFFE");
        CompareArrays(b1, b2);

        b1 = new byte[] { 0x0F, 0x45 };
        b2 = BitStuff.fromHexString("F45");
        CompareArrays(b1, b2);

        b1 = new byte[] { 0x45, 0x20, 0x00, 0x01 };
        b2 = BitStuff.fromHexString("45020001");
        CompareArrays(b1, b2);
    }

    public void testToByteArray()
    {
        byte[] r1 = BitStuff.toByteArray((long) 1);
        Assert.assertTrue(CompareArrays(r1, new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01}));

        byte[] r2 = BitStuff.toByteArray((long) 0);
        Assert.assertTrue(CompareArrays(r2, new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00}));

        byte[] r3 = BitStuff.toByteArray((long) 256);
        Assert.assertTrue(CompareArrays(r3, new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00}));

        byte[] r4 = BitStuff.toByteArray(1);
        Assert.assertTrue(CompareArrays(r4, new byte[]{0x00, 0x00, 0x00, 0x01}));

        byte[] r5 = BitStuff.toByteArray(0);
        Assert.assertTrue(CompareArrays(r5, new byte[]{0x00, 0x00, 0x00, 0x00}));

        byte[] r6 = BitStuff.toByteArray(256);
        Assert.assertTrue(CompareArrays(r6, new byte[]{0x00, 0x00, 0x01, 0x00}));
    }

    public void testToInt() throws IOException {
        assertEquals(BitStuff.toInt(intToBytes(1), 0), 1);

        assertEquals(BitStuff.toInt(intToBytes(0), 0), 0);

        assertEquals(BitStuff.toInt(intToBytes(3684), 0), 3684);

        assertEquals(BitStuff.toInt(intToBytes(-3684), 0), -3684);

        assertEquals(BitStuff.toInt(intToBytes(8495637), 0), 8495637);

        assertEquals(BitStuff.toInt(intToBytes(-8495637), 0), -8495637);

        assertEquals(BitStuff.toInt(intToBytes(1, 5), 5), 1);

        assertEquals(BitStuff.toInt(intToBytes(0, 5), 5), 0);

        assertEquals(BitStuff.toInt(intToBytes(3684, 5), 5), 3684);

        assertEquals(BitStuff.toInt(intToBytes(-3684, 5), 5), -3684);

        assertEquals(BitStuff.toInt(intToBytes(8495637, 5), 5), 8495637);

        assertEquals(BitStuff.toInt(intToBytes(-8495637, 5), 5), -8495637);

        int i2 = BitStuff.toInt(new byte[]{0x00, 0x08, 0x00, 0x00}, 0);
        assertEquals(i2, 524288);

        int i4 = BitStuff.toInt(new byte[]{0x02, 0x00, 0x00, 0x00}, 0);
        assertEquals(i4, 33554432);

        int i5 = BitStuff.toInt(new byte[]{0x00, 0x00, 0x00, 0x01}, 0);
        assertEquals(i5, 1);

        int i6 = BitStuff.toInt(new byte[]{0x00, 0x00, 0x01, 0x00}, 0);
        assertEquals(i6, 256);

        int i7 = BitStuff.toInt(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00}, 1);
        assertEquals(i7, 0);

        int i8 = BitStuff.toInt(new byte[]{0x00, 0x00, 0x00, 0x00, 0x01}, 1);
        assertEquals(i8, 1);

        int i9 = BitStuff.toInt(new byte[]{0x00, 0x00, 0x00, 0x01, 0x00}, 1);
        assertEquals(i9, 256);
    }

    public void testCopy()
    {
        byte[] src = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A };

        byte[] dst1 = new byte[1];
        BitStuff.copy(src, 0, 1, dst1);
        Assert.assertTrue(CompareArrays(dst1, new byte[]{0x00}));

        byte[] dst2 = new byte[1];
        BitStuff.copy(src, 1, 1, dst2);
        Assert.assertTrue(CompareArrays(dst2, new byte[]{0x01}));

        byte[] dst3 = new byte[1];
        BitStuff.copy(src, 2, 1, dst3);
        Assert.assertTrue(CompareArrays(dst3, new byte[]{0x02}));

        byte[] dst4 = new byte[2];
        BitStuff.copy(src, 0, 2, dst4);
        Assert.assertTrue(CompareArrays(dst4, new byte[]{0x00, 0x01}));

        byte[] dst5 = new byte[2];
        BitStuff.copy(src, 1, 2, dst5);
        Assert.assertTrue(CompareArrays(dst5, new byte[]{0x01, 0x02}));

        byte[] dst6 = new byte[2];
        BitStuff.copy(src, 2, 2, dst6);
        Assert.assertTrue(CompareArrays(dst6, new byte[]{0x02, 0x03}));
    }

    public void testCompareArrays()
    {
        assertTrue(BitStuff.CompareArrays(2, new byte[]{0x00, 0x01}, 0, new byte[]{0x00, 0x01}, 0));
        assertTrue(BitStuff.CompareArrays(2, new byte[]{0x00, 0x01}, 0, new byte[]{0x00, 0x01, 0x02}, 0));
        assertTrue(BitStuff.CompareArrays(2, new byte[]{0x00, 0x01, 0x02}, 0, new byte[]{0x00, 0x01}, 0));
        assertTrue(BitStuff.CompareArrays(2, new byte[]{0x00, 0x01, 0x02}, 0, new byte[]{0x00, 0x01, 0x7F}, 0));
        assertTrue(BitStuff.CompareArrays(2, new byte[]{0x00, 0x01, 0x02}, 1, new byte[]{0x7f, 0x01, 0x02}, 1));
        assertTrue(BitStuff.CompareArrays(2, new byte[]{0x00, 0x01, 0x02}, 0, new byte[]{0x7f, 0x00, 0x01}, 1));
        assertTrue(BitStuff.CompareArrays(2, new byte[]{0x7F, 0x01, 0x02}, 1, new byte[]{0x01, 0x02, 0x7F}, 0));
        assertFalse(BitStuff.CompareArrays(2, new byte[]{0x7F, 0x01, 0x02}, 0, new byte[]{0x00, 0x01, 0x7F}, 0));
        assertFalse(BitStuff.CompareArrays(3, new byte[]{0x7F, 0x01, 0x02}, 0, new byte[]{0x00, 0x01, 0x7F}, 0));
        assertFalse(BitStuff.CompareArrays(4, new byte[]{0x7F, 0x01, 0x02}, 0, new byte[]{0x00, 0x01, 0x7F}, 0));
        assertFalse(BitStuff.CompareArrays(2, new byte[]{0x7F, 0x01, 0x02}, 0, new byte[]{0x00, 0x01, 0x7F}, 2));
    }

}
