package invalid.showme.tests;

import invalid.showme.util.PhotoFileManager;

public class PhotoFileManagerTest extends TestCaseWithUtils
{
    public void testThumbnailNamer()
    {
        String expected1 = "/abcde/fghei/pqrst_tb.jpg";
        String actual1 = PhotoFileManager.GetThumbnailPath("/abcde/fghei/pqrst.jpg");
        assertEquals(expected1, actual1);

        String expected2 = "/abcde/fghei/pqrst.jpg_tb.jpg";
        String actual2 = PhotoFileManager.GetThumbnailPath("/abcde/fghei/pqrst.jpg.jpg");
        assertEquals(expected2, actual2);

        String expected3 = "/abcde/fghei/klmni/pqrst_tb.JPEG";
        String actual3 = PhotoFileManager.GetThumbnailPath("/abcde/fghei//klmni//pqrst.JPEG");
        assertEquals(expected3, actual3);

        boolean nope = false;
        try {
            String actual4 = PhotoFileManager.GetThumbnailPath("/abcde/fghei/pqrst");
            nope = true;
        }
        catch(Exception ex){

        }
        assertFalse(nope);
    }
}
