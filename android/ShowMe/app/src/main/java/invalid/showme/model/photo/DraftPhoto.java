package invalid.showme.model.photo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.acra.ACRA;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import invalid.showme.exceptions.DatabaseException;
import invalid.showme.exceptions.StrangeUsageException;
import invalid.showme.model.UserProfile;
import invalid.showme.model.db.DBHelper;
import invalid.showme.model.db.DraftTableContract;
import invalid.showme.model.photo.getthumbnail.PhotoGetThumbnailJob;
import invalid.showme.util.CryptoUtil;
import invalid.showme.util.RandomUtil;

public class DraftPhoto extends Photo implements Serializable
{
    final private static String TAG = "DraftPhoto";

    //temporary until can encrypt and process draft
    private String plaintextPhotoFilename;
    private String plaintextThumbnailFilename;

    public DraftPhoto(String message, Boolean privatePhoto, String plaintextPhotoFilename, String plaintextThumbnailFilename) {
        this(message, privatePhoto, plaintextPhotoFilename, plaintextThumbnailFilename, BitmapFactory.decodeFile(plaintextThumbnailFilename));
    }
    private DraftPhoto(String message, Boolean privatePhoto, String plaintextFullSizeFile, String plaintextThumbnailFile, Bitmap thumbnail)
    {
        super();

        this.id = RandomUtil.getRandomNegativeNumber();

        this.Message = message;
        this.PrivatePhoto = privatePhoto;
        this.plaintextPhotoFilename = plaintextFullSizeFile;
        this.plaintextThumbnailFilename = plaintextThumbnailFile;
        this.realThumbnail = thumbnail;
    }


    private DraftPhoto(long id, String message, Boolean privatePhoto, String photoFilename, String thumbnailFilename, byte[] key, byte[] photoIv, byte[] thumbnailIv) throws FileNotFoundException {
        super(id, message, privatePhoto, photoFilename, thumbnailFilename, key, photoIv, thumbnailIv);
    }


    @Override
    public boolean equals(Object o) {
        if(o instanceof DraftPhoto)
        {
            DraftPhoto d = (DraftPhoto)o;
            if(d.id < 0 && this.id < 0) {
                if (d.plaintextPhotoFilename != null && this.plaintextPhotoFilename != null)
                    return this.plaintextPhotoFilename.equals(d.plaintextPhotoFilename);
                else
                    throw new RuntimeException("Tried to compare DraftPhotos where only one has plaintext");
            }
            else if (d.id > 0 && this.id > 0)
                return d.id == this.id;
            else
                throw new RuntimeException("Tried to compare DraftPhotos where only one has been saved.");
        }
        return false;
    }

    public void EncryptAndSaveFiles(Context context) throws IOException
    {
        byte[] key = new byte[16];
        RandomUtil.getBytes(key);
        byte[] photoIv = new byte[16];
        RandomUtil.getBytes(photoIv);
        byte[] thumbnailIv = new byte[16];
        RandomUtil.getBytes(thumbnailIv);

        this.photoFile = CryptoUtil.EncryptFileToFile(context, new File(this.plaintextPhotoFilename), key, photoIv);
        Log.d(TAG, "Finished encrypting fullsize " + this.plaintextPhotoFilename);

        this.thumbnailFile = CryptoUtil.EncryptFileToFile(context, new File(this.plaintextThumbnailFilename), key, thumbnailIv);
        Log.d(TAG, "Finished encrypting thumb " + this.plaintextThumbnailFilename);

        this.plaintextPhotoFilename = "";
        this.plaintextThumbnailFilename = "";
        this.key = key;
        this.photoIv = photoIv;
        this.thumbnailIv = thumbnailIv;
    }

    @Override
    public InputStream getDecryptedPhotoStream()
    {
        if(this.plaintextPhotoFilename != null && this.key == null) {
            //unusual case, someone trying to send draft that hasn't been encrypted
            //  maaaay have race condition on plaintext photo file.
            String errMsg = "trying to send draft that hasn't been encrypted..." + plaintextPhotoFilename;
            Log.w(TAG, errMsg);
            ACRA.getErrorReporter().handleException(new StrangeUsageException(errMsg));

            File plaintextPhoto = new File(this.plaintextPhotoFilename);
            if(plaintextPhoto.exists() && plaintextPhoto.canRead())
                try {
                    return new FileInputStream(plaintextPhoto);
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "Caught FileNotFoundException on plaintext photo " + plaintextPhotoFilename + " after thought it present.");
                    ACRA.getErrorReporter().handleException(e);
                }
            else {
                String msg = "Could not find plaintext photo " + plaintextPhotoFilename;
                Log.e(TAG, msg);
                ACRA.getErrorReporter().handleException(new DatabaseException(msg));
                return null;
            }
        }
        else {
            //normal case
            return super.getDecryptedPhotoStream();
        }
        return null;
    }



    public static boolean DeleteFromDatabase(DBHelper dbHelper, long id)
    {
        boolean success = false;
        String selection = DraftTableContract._ID + " LIKE ?";
        String[] selectionArgs = { String.valueOf(id) };
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            db.beginTransaction();
            if (1 == db.delete(DraftTableContract.TABLE_NAME, selection, selectionArgs)) {
                success = true;
                db.setTransactionSuccessful();
            } else {
                String msg = "Could not delete DraftPhoto from database...";
                Log.e(TAG, msg);
                ACRA.getErrorReporter().handleException(new DatabaseException(msg));
            }
        }finally {
            db.endTransaction();
        }
        return success;
    }
    public static DraftPhoto FromDatabase(UserProfile up, Cursor c, Boolean loadThumbnail) throws FileNotFoundException
    {
        long id  = c.getLong(c.getColumnIndexOrThrow(DraftTableContract._ID));
        String photoFilename = c.getString(c.getColumnIndexOrThrow(DraftTableContract.COLUMN_NAME_PHOTOFILENAME));
        String thumbFilename = c.getString(c.getColumnIndexOrThrow(DraftTableContract.COLUMN_NAME_THUMBNAILFILENAME));
        String message = c.getString(c.getColumnIndexOrThrow(DraftTableContract.COLUMN_NAME_MESSAGE));
        Boolean privatePhoto = 0 != c.getLong(c.getColumnIndexOrThrow(DraftTableContract.COLUMN_NAME_PRIVATEPHOTO));
        byte[] key = c.getBlob(c.getColumnIndexOrThrow(DraftTableContract.COLUMN_NAME_KEY));
        byte[] photoIv = c.getBlob(c.getColumnIndexOrThrow(DraftTableContract.COLUMN_NAME_PHOTOIV));
        byte[] thumbIv = c.getBlob(c.getColumnIndexOrThrow(DraftTableContract.COLUMN_NAME_THUMBNAILIV));

        DraftPhoto d = new DraftPhoto(id, message, privatePhoto, photoFilename, thumbFilename, key, photoIv, thumbIv);
        if(loadThumbnail)
            up.getJobManager().addJob(new PhotoGetThumbnailJob(d));
        return d;
    }

    public boolean saveToDatabase(DBHelper dbHelper)
    {
        if(this.id >= 0) throw new RuntimeException("Do not re-enter already-entered values in database");
        if(this.photoFile == null) throw new RuntimeException("Cannot enter uninitialized photo into database");

        ContentValues values = new ContentValues();
        values.put(DraftTableContract.COLUMN_NAME_PHOTOFILENAME, this.photoFile.getAbsolutePath());
        values.put(DraftTableContract.COLUMN_NAME_THUMBNAILFILENAME, this.thumbnailFile.getAbsolutePath());
        values.put(DraftTableContract.COLUMN_NAME_MESSAGE, this.Message);
        values.put(DraftTableContract.COLUMN_NAME_PRIVATEPHOTO, this.PrivatePhoto ? 1 : 0);
        values.put(DraftTableContract.COLUMN_NAME_KEY, this.key);
        values.put(DraftTableContract.COLUMN_NAME_PHOTOIV, this.photoIv);
        values.put(DraftTableContract.COLUMN_NAME_THUMBNAILIV, this.thumbnailIv);

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            db.beginTransaction();
            long id = db.insert(DraftTableContract.TABLE_NAME,
                    "null",
                    values);
            if (id < 0) {
                String msg = "Could not insert DraftPhoto into database...";
                Log.e(TAG, msg);
                ACRA.getErrorReporter().handleException(new DatabaseException(msg));
            }
            else
                db.setTransactionSuccessful();
            this.id = id;
        }
        finally {
            db.endTransaction();
        }
        return this.id > 0;
    }
}
