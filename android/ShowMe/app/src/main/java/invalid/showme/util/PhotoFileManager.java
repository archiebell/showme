package invalid.showme.util;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.acra.ACRA;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PhotoFileManager
{
    private final static String TAG = "PhotoFileManager";

    public static String GetThumbnailPath(String photoLocation)
    {
        return GetThumbnailFile(photoLocation).getAbsolutePath();
    }
    public static String GetThumbnailPath(File photoLocation)
    {
        return GetThumbnailFile(photoLocation).getAbsolutePath();
    }
    private static File GetThumbnailFile(String photoLocation)
    {
        File f = new File(photoLocation);
        return GetThumbnailFile(f);
    }
    public static File GetThumbnailFile(File photoLocation)
    {
        String name = photoLocation.getName();
        if(!name.contains(".")) {
            Log.e(TAG, "Passed file " + name + " without file extension.");
            throw new RuntimeException("Passed file without extension in PhotoFileManager.GetThumbnailPath");
        }
        String withoutExtension = name.substring(0, name.lastIndexOf('.'));
        String extension = name.substring(name.lastIndexOf('.'));

        return new File(photoLocation.getParent() + "/" + withoutExtension + "_tb" + extension);
    }

    public static File createTempImageFile(Context context, Bitmap b) throws IOException
    {
        File output = createTempImageFile(context);
        createTempImageFile(output, b);
        return output;
    }
    public static File createTempImageFile(Context context, byte[] b) throws IOException
    {
        File output = createTempImageFile(context);
        createTempImageFile(output, b);
        return output;
    }
    public static void createTempImageFile(File output, Bitmap b) throws IOException
    {
        FileOutputStream outStream = new FileOutputStream(output);
        b.compress(Bitmap.CompressFormat.JPEG, 95, outStream);
        outStream.close();
    }
    public static void createTempImageFile(File output, byte[] b) throws IOException
    {
        FileOutputStream outStream = new FileOutputStream(output);
        outStream.write(b, 0, b.length);
        outStream.close();
    }
    public static File createTempImageFile(Context context) throws IOException
    {
        File storageDir = context.getFilesDir();

        //TODO: Refactor so don't store it straight to disk, use IOCipher
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "TMPIMG_" + timeStamp + "_";
        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
    }

    public static File createPermanentImageFile(Context context) throws IOException
    {
        File storageDir = context.getFilesDir();

        String imageFileName = "ENC";
        return File.createTempFile(
                imageFileName,  /* prefix */
                ".bin",         /* suffix */
                storageDir      /* directory */
        );
    }

    //TODO: Verify works as expected and doesn't inadvertently open up holes
    public static File CreateReadablePhoto(Context context, InputStream photoStream) throws IOException {
        File out = PhotoFileManager.createPublicImageFile(context);
        FileOutputStream outStream = new FileOutputStream(out);
        IOUtil.streamStreams(photoStream, outStream);
        return out;
    }
    private static File createPublicImageFile(Context context) throws IOException
    {
        String sharingDirectory = context.getFilesDir() + "/forsharing/";
        File shareDir = new File(sharingDirectory);
        shareDir.mkdirs();

        String imageFileName = "PUB_";
        return File.createTempFile(
                imageFileName + TimeUtil.GetNow() + "_",  /* prefix */
                ".jpg",         /* suffix */
                shareDir      /* directory */
        );
    }

    public static File createThumbnailFromFullSizedFileToFile(File photoLocation)
    {
        return createThumbnailFromFullSizedFileToFile(photoLocation.getAbsolutePath());
    }
    private static File createThumbnailFromFullSizedFileToFile(String photoLocation)
    {
        File thumbnailLocation = PhotoFileManager.GetThumbnailFile(photoLocation);
        Bitmap thumbnail = BitmapUtils.DecodeFileForSize(photoLocation, BitmapUtils.THUMBNAIL_SIZE, BitmapUtils.THUMBNAIL_SIZE);
        PhotoFileManager.createThumbnailFromThumbnailBitmapToFile(thumbnail, thumbnailLocation);
        return thumbnailLocation;
    }
    public static void createThumbnailFromBytesToFile(byte[] bytes, File thumbnailFile)
    {
        Bitmap thumbnail = BitmapUtils.DecodeByteArrayForSize(bytes, BitmapUtils.THUMBNAIL_SIZE, BitmapUtils.THUMBNAIL_SIZE);
        PhotoFileManager.createThumbnailFromThumbnailBitmapToFile(thumbnail, thumbnailFile);
    }
    public static void createThumbnailFromThumbnailBitmapToFile(Bitmap thumbnail, File thumbnailFile)
    {
        FileOutputStream thumbStream = null;
        try {
            thumbStream = new FileOutputStream(thumbnailFile);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "In createThumbnailFromThumbnailBitmapToFile() caught FileNotFoundException on " + thumbnailFile.toString());
            ACRA.getErrorReporter().handleException(e);
        }
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 95, thumbStream);
        thumbnail.recycle();
    }

    //TODO: Can be made more efficient?
    public static void CopyFile(String sourceFile, File destFile) throws IOException {
        CopyFile(new File(sourceFile), destFile);
    }
    public static void CopyFile(File sourceFile, File destFile) throws IOException {
        if(!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        }
        finally {
            if(source != null) {
                source.close();
            }
            if(destination != null) {
                destination.close();
            }
        }
    }
}
