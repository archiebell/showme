package invalid.showme.model.photo;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.util.Log;

import org.acra.ACRA;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;

import javax.crypto.CipherOutputStream;

import invalid.showme.exceptions.DatabaseException;
import invalid.showme.model.UserProfile;
import invalid.showme.model.db.DBHelper;
import invalid.showme.model.db.PhotoTableContract;
import invalid.showme.model.photo.getthumbnail.PhotoGetThumbnailJob;
import invalid.showme.util.BitmapUtils;
import invalid.showme.util.CryptoUtil;
import invalid.showme.util.PhotoFileManager;
import invalid.showme.util.RandomUtil;
import invalid.showme.util.TimeUtil;

public class ReceivedPhoto extends Photo implements Serializable
{
    final private static String TAG = "ReceivedPhoto";

    public Boolean Seen;
    public String MessageID;
    public long FriendID;
    public long Received;
    private long FirstViewed;

    private ReceivedPhoto(long id, long friendID, String messageID, long received, long firstViewed, String message, Boolean privatePhoto, Boolean seen, String photoFilename, String thumbFilename, byte[] key, byte[] photoIv, byte[] thumbIv) throws FileNotFoundException {
        super(id, message, privatePhoto, photoFilename, thumbFilename, key, photoIv, thumbIv);
        this.MessageID = messageID;
        this.FriendID = friendID;
        this.Received = received;
        this.FirstViewed = firstViewed;
        this.Seen = seen;
    }
    public ReceivedPhoto(Context context, String messageID, long friendID, String message, Boolean privatePhoto, Boolean seen, long received, long firstViewed, byte[] photoBytes) {
        this.MessageID = messageID;
        this.FriendID = friendID;
        this.Received = received;
        this.FirstViewed = firstViewed;
        this.Message = message;
        this.PrivatePhoto = privatePhoto;
        this.Seen = seen;

        byte[] key = new byte[16];
        RandomUtil.getBytes(key);
        byte[] photoIv = new byte[16];
        RandomUtil.getBytes(photoIv);
        byte[] thumbnailIv = new byte[16];
        RandomUtil.getBytes(thumbnailIv);

        this.photoFile = CryptoUtil.EncryptBytesToFile(context, photoBytes, key, photoIv);
        this.realThumbnail = BitmapUtils.DecodeByteArrayForSize(photoBytes, BitmapUtils.THUMBNAIL_SIZE, BitmapUtils.THUMBNAIL_SIZE);

        //don't have plaintext file on disk, so jump through hoop rather than call createThumbnailFromFileToFile
        this.thumbnailFile = PhotoFileManager.GetThumbnailFile(this.photoFile);
        try {
            FileOutputStream outputStream = new FileOutputStream(this.thumbnailFile);
            CipherOutputStream ciphertextStream = CryptoUtil.wrapinCipherOutputStream(outputStream, key, thumbnailIv);

            this.realThumbnail.compress(Bitmap.CompressFormat.JPEG, 95, ciphertextStream);
            ciphertextStream.flush();
            ciphertextStream.close();
        } catch(IOException e) {
            Log.e(TAG, "Caught IOException trying to write encrypted thumbnail file.");
            ACRA.getErrorReporter().handleException(e);
        }

        this.key = key;
        this.photoIv = photoIv;
        this.thumbnailIv = thumbnailIv;
    }

    public boolean ShouldBeDeleted() {
        if(!this.PrivatePhoto) return false;
        if(this.FirstViewed == 0) return false;
        return TimeUtil.GetNow() - this.FirstViewed > 60*60*4;
    }

    public void SetAsSeen(DBHelper dbHelper) {
        if(!this.Seen) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            try {
                ContentValues args = new ContentValues();
                args.put(PhotoTableContract.COLUMN_NAME_SEEN, 1);
                if(this.FirstViewed == 0) {
                    this.FirstViewed = TimeUtil.GetNow();
                    args.put(PhotoTableContract.COLUMN_NAME_FIRSTVIEWED, this.FirstViewed);
                }
                db.beginTransaction();
                if(1 == db.update(PhotoTableContract.TABLE_NAME, args, PhotoTableContract._ID + "=" + this.getID(), null)) {
                    db.setTransactionSuccessful();
                    this.Seen = true;
                } else {
                    String msg = "Couldn't set photo ID " + this.getID() + " as seen.";
                    Log.e(TAG, msg);
                    ACRA.getErrorReporter().handleException(new DatabaseException(msg));
                }
            }
            finally {
                db.endTransaction();
            }
        }
    }

    public Boolean FilesIntact()
    {
        if(!this.photoFile.exists() || !this.photoFile.canRead())
            return false;
        if(!this.thumbnailFile.exists() || !this.thumbnailFile.canRead())
            return false;
        return true;
    }

    public static boolean DeleteFromDatabase(DBHelper dbHelper, long id)
    {
        boolean success = false;
        String selection = PhotoTableContract._ID + " LIKE ?";
        String[] selectionArgs = { String.valueOf(id) };
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            db.beginTransaction();
            if (1 == db.delete(PhotoTableContract.TABLE_NAME, selection, selectionArgs)) {
                success = true;
                db.setTransactionSuccessful();
            }
        } finally {
            db.endTransaction();
        }
        return success;
    }
    public static ReceivedPhoto FromDatabase(UserProfile up, Cursor c, Boolean fetchThumbnail) throws FileNotFoundException {
        long id  = c.getLong(c.getColumnIndexOrThrow(PhotoTableContract._ID));
        long friendId = c.getLong(c.getColumnIndexOrThrow(PhotoTableContract.COLUMN_NAME_FRIENDID));
        String messageId = c.getString(c.getColumnIndexOrThrow(PhotoTableContract.COLUMN_NAME_MESSAGEID));
        long received = c.getLong(c.getColumnIndexOrThrow(PhotoTableContract.COLUMN_NAME_RECEIVED));
        long firstviewed = c.getLong(c.getColumnIndexOrThrow(PhotoTableContract.COLUMN_NAME_FIRSTVIEWED));
        String message = c.getString(c.getColumnIndexOrThrow(PhotoTableContract.COLUMN_NAME_MESSAGE));
        Boolean privatePhoto = 0 != c.getLong(c.getColumnIndexOrThrow(PhotoTableContract.COLUMN_NAME_PRIVATEPHOTO));
        Boolean seen = 0 != c.getLong(c.getColumnIndexOrThrow(PhotoTableContract.COLUMN_NAME_SEEN));
        String photoFilename = c.getString(c.getColumnIndexOrThrow(PhotoTableContract.COLUMN_NAME_PHOTOFILENAME));
        String thumbFilename = c.getString(c.getColumnIndexOrThrow(PhotoTableContract.COLUMN_NAME_THUMBNAILFILENAME));
        byte[] key = c.getBlob(c.getColumnIndexOrThrow(PhotoTableContract.COLUMN_NAME_KEY));
        byte[] photoIv = c.getBlob(c.getColumnIndexOrThrow(PhotoTableContract.COLUMN_NAME_PHOTOIV));
        byte[] thumbIv = c.getBlob(c.getColumnIndexOrThrow(PhotoTableContract.COLUMN_NAME_THUMBNAILIV));

        ReceivedPhoto d = new ReceivedPhoto(id, friendId, messageId, received, firstviewed, message, privatePhoto, seen, photoFilename, thumbFilename, key, photoIv, thumbIv);
        if(fetchThumbnail)
            up.getJobManager().addJob(new PhotoGetThumbnailJob(d));
        return d;
    }

    public boolean saveToDatabase(DBHelper dbHelper)
    {
        if(this.id != -1) {
            String msg = "Tried to enter photo into database that already has ID:" + this.id;
            Log.e(TAG, msg);
            ACRA.getErrorReporter().handleException(new DatabaseException(msg));
            return true;
        }
        if(this.photoFile == null) {
            String msg = "Tried to enter photo into database that doesn't have file";
            Log.e(TAG, msg);
            ACRA.getErrorReporter().handleException(new DatabaseException(msg));
            return false;
        }

        ContentValues values = new ContentValues();
        values.put(PhotoTableContract.COLUMN_NAME_FRIENDID, this.FriendID);
        values.put(PhotoTableContract.COLUMN_NAME_MESSAGEID, this.MessageID);
        values.put(PhotoTableContract.COLUMN_NAME_RECEIVED, this.Received);
        values.put(PhotoTableContract.COLUMN_NAME_FIRSTVIEWED, this.FirstViewed);
        values.put(PhotoTableContract.COLUMN_NAME_MESSAGE, this.Message);
        values.put(PhotoTableContract.COLUMN_NAME_PRIVATEPHOTO, this.PrivatePhoto? 1 : 0);
        values.put(PhotoTableContract.COLUMN_NAME_SEEN, this.Seen? 1 : 0);
        values.put(PhotoTableContract.COLUMN_NAME_PHOTOFILENAME, this.photoFile.getAbsolutePath());
        values.put(PhotoTableContract.COLUMN_NAME_THUMBNAILFILENAME, this.thumbnailFile.getAbsolutePath());
        values.put(PhotoTableContract.COLUMN_NAME_KEY, this.key);
        values.put(PhotoTableContract.COLUMN_NAME_PHOTOIV, this.photoIv);
        values.put(PhotoTableContract.COLUMN_NAME_THUMBNAILIV, this.thumbnailIv);

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try
        {
            db.beginTransaction();
            long id = db.insert(PhotoTableContract.TABLE_NAME,
                    "null",
                    values);
            if(id < 0) {
                String msg = "Could not insert ReceivedPhoto into database...";
                Log.e(TAG, msg);
                ACRA.getErrorReporter().handleException(new DatabaseException(msg));
            }
            else
                db.setTransactionSuccessful();
            this.id = id;
        } finally {
            db.endTransaction();
        }
        return this.id > 0;
    }
}
