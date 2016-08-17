package invalid.showme.services;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import org.acra.ACRA;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import invalid.showme.exceptions.StrangeUsageException;
import invalid.showme.util.PhotoFileManager;

public class PhotoReceiver extends ContentProvider
{
    private final static String TAG = "PhotoReceiver";

    public static File lastPhoto;

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] strings, String s, String[] strings1, String s1) {
        Log.e(TAG, "Somehow wound up in query???");
        ACRA.getErrorReporter().handleException(new StrangeUsageException());
        return null;
    }

    @Override
    public String getType(Uri uri) {
        Log.e(TAG, "Somehow wound up in getType???");
        ACRA.getErrorReporter().handleException(new StrangeUsageException());
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        Log.e(TAG, "Somehow wound up in insert???");
        ACRA.getErrorReporter().handleException(new StrangeUsageException());
        return null;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        try {
            lastPhoto = PhotoFileManager.createTempImageFile(getContext());
            return (ParcelFileDescriptor.open(lastPhoto,
                    ParcelFileDescriptor.MODE_READ_WRITE));
        } catch (IOException e) {
            Log.e(TAG, "Could not create temp file for writing.");
            ACRA.getErrorReporter().handleException(e);
        }
        return null;
    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        Log.e(TAG, "Somehow wound up in delete???");
        ACRA.getErrorReporter().handleException(new StrangeUsageException());
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        Log.e(TAG, "Somehow wound up in update???");
        ACRA.getErrorReporter().handleException(new StrangeUsageException());
        return 0;
    }
}
