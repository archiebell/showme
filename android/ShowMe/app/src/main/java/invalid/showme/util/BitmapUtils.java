package invalid.showme.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.media.ThumbnailUtils;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.ImageView;

import org.acra.ACRA;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import invalid.showme.R;
import invalid.showme.exceptions.StrangeUsageException;

public class BitmapUtils
{
    private final static String TAG = "BitmapUtils";

    public final static int THUMBNAIL_SIZE = 200;

    public static Bitmap DecodeByteArrayForSize(byte[] bytes, int reqWidth, int reqHeight)
    {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);

        options.inJustDecodeBounds = false;
        options.inSampleSize = BitmapUtils.CalculateInSampleSize(options.outWidth, options.outHeight, reqWidth, reqHeight);

        //If have very large bitmap, have to get it under 4096 for processing.
        while(options.outWidth / options.inSampleSize > 4096 || options.outHeight / options.inSampleSize > 4096)
            options.inSampleSize++;

        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
    }
    public static Bitmap DecodeFileForSize(String inputFile, int reqWidth, int reqHeight)
    {
        try {
            FileInputStream fis1 = new FileInputStream(inputFile);
            FileInputStream fis2 = new FileInputStream(inputFile);
            return DecodeStreamForSize(fis1, fis2, reqWidth, reqHeight);
        } catch(IOException e) {
            ACRA.getErrorReporter().handleException(e);
        }
        return null;
    }
    public static Bitmap DecodeStreamForSize(InputStream stream1, InputStream stream2, int reqWidth, int reqHeight)
    {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(stream1, null, options);
            stream1.close();

            options.inJustDecodeBounds = false;
            options.inSampleSize = BitmapUtils.CalculateInSampleSize(options.outWidth, options.outHeight, reqWidth, reqHeight);

            //If have very large bitmap, have to get it under 4096 for processing.
            while(options.outWidth / options.inSampleSize > 4096 || options.outHeight / options.inSampleSize > 4096)
                options.inSampleSize++;

            Bitmap bitmap = BitmapFactory.decodeStream(stream2, null, options);
            stream2.close();
            return bitmap;
        } catch(IOException e) {
            ACRA.getErrorReporter().handleException(e);
        }
        return null;
    }
    private static int CalculateInSampleSize(int imageWidth, int imageHeight, int reqWidth, int reqHeight) {
        int inSampleSize = 1;

        if (imageHeight > reqHeight || imageWidth > reqWidth) {

            final int halfHeight = imageHeight / 2;
            final int halfWidth = imageWidth / 2;

            // Calculate largest inSampleSize value that power of 2 and keeps both
            // height and width larger than requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static Bitmap createThumbnail(Bitmap photo)
    {
        float photoW = photo.getWidth();
        float photoH = photo.getHeight();

        float scaleFactor = Math.min(THUMBNAIL_SIZE / photoW, THUMBNAIL_SIZE / photoH);

        return ThumbnailUtils.extractThumbnail(photo, (int) (scaleFactor * photoW), (int) (scaleFactor * photoH));
    }

    @Deprecated
    private static void scaleImageForView(ImageView mImageView, Bitmap image)
    {
        // Get dimensions View
        double targetW = mImageView.getWidth();
        double targetH = mImageView.getHeight();

        // Get dimensions bitmap
        double photoW = image.getWidth();
        double photoH = image.getHeight();

        // Determine how much scale down image
        double scaleFactor = Math.min(targetW / photoW, targetH / photoH);
        if(scaleFactor > 1) {
            String msg = "scaleImageForView scaled image " + scaleFactor;
            Log.w(TAG, msg);
            ACRA.getErrorReporter().handleException(new StrangeUsageException(msg));
        }

        Bitmap b = ThumbnailUtils.extractThumbnail(image, (int) (scaleFactor * photoW), (int) (scaleFactor * photoH));
        mImageView.setImageBitmap(b);
    }
    @Deprecated
    private static void scaleImageForView(ImageView mImageView, String photoLocation)
    {
        // Get dimensions View
        double targetW = mImageView.getWidth();
        double targetH = mImageView.getHeight();

        // Get dimensions bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(photoLocation, bmOptions);
        double photoW = bmOptions.outWidth;
        double photoH = bmOptions.outHeight;

        // Determine how much scale down image (reversed)
        double scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        // Decode image file into Bitmap sized for fill View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = (int)Math.ceil(scaleFactor);

        Bitmap bitmap = BitmapFactory.decodeFile(photoLocation, bmOptions);
        mImageView.setImageBitmap(bitmap);
    }

    public static void setImageForView(ImageView mImageView, File photoLocation)
    {
        setImageForView(mImageView, photoLocation.getAbsolutePath());
    }
    public static void setImageForView(ImageView mImageView, String photoLocation)
    {
        Bitmap bitmap = BitmapFactory.decodeFile(photoLocation);
        mImageView.setImageBitmap(bitmap);
    }
    public static void setImageForView(ImageView mImageView, Bitmap photo)
    {
        mImageView.setImageBitmap(photo);
    }

    private static Bitmap sunglasses;
    public static Bitmap getSunglassesBitmap(Context context) {
        if(sunglasses == null)
            sunglasses = BitmapFactory.decodeResource(context.getResources(), R.drawable.sunglasses);
        return sunglasses;
    }

    private static Bitmap brokenThumbnail;
    public static Bitmap getBrokenThumbnailResource(Context context) {
        if(brokenThumbnail == null)
            brokenThumbnail = BitmapFactory.decodeResource(context.getResources(), R.drawable.brokenthumbnail);
        return brokenThumbnail;
    }

    private static Bitmap privacyPlease;
    public static Bitmap getPrivacyPleaseResource(Context context) {
        if(privacyPlease == null)
            privacyPlease = BitmapFactory.decodeResource(context.getResources(), R.drawable.privacyplease);
        return privacyPlease;
    }

    private static int screenWidth = 0;
    private static int screenHeight = 0;
    public static int GetScreenWidth(WindowManager wm) {
        if(screenWidth == 0 || screenHeight == 0) {
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            screenWidth = size.x;
            screenHeight = size.y;
        }
        return screenWidth;
    }

    public static int GetScreenHeight(WindowManager wm) {
        if(screenWidth == 0 || screenHeight == 0) {
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            screenWidth = size.x;
            screenHeight = size.y;
        }
        return screenHeight;
    }
}
