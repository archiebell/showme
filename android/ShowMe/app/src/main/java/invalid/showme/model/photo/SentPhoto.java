package invalid.showme.model.photo;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.acra.ACRA;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;

import invalid.showme.exceptions.DatabaseException;
import invalid.showme.exceptions.StrangeUsageException;
import invalid.showme.model.UserProfile;
import invalid.showme.model.db.DBHelper;
import invalid.showme.model.db.SentTableContract;
import invalid.showme.model.photo.getthumbnail.PhotoGetThumbnailJob;
import invalid.showme.util.CryptoUtil;
import invalid.showme.util.PhotoFileManager;
import invalid.showme.util.RandomUtil;
import invalid.showme.util.TimeUtil;

public class SentPhoto extends Photo implements Serializable
{
    public static class SentPhotoStatus
    {
        public static int Encrypting = 1;
        public static int Queued = 2;
        public static int Sent = 3;
        public static int Received = 4;
        public static int Error = 5;
    }

    final private static String TAG = "SentPhoto";

    public int Status;
    public String MessageID;
    public long FriendID;
    public long Sent;

    //temporary until encrypt and save sent photo
    public String plaintextPhotoFilename;
    public String plaintextThumbnailFilename;
    private DraftPhoto draftPhotoSource;

    private SentPhoto(long id, int status, long friendID, String messageID, long sent, String message, Boolean privatePhoto, String photoFilename, String thumbnailFilename,
                      byte[] key, byte[] photoIv, byte[] thumbnailIv, String plaintextPhotoFilename, String plaintextThumbnailFilename, DraftPhoto d) throws FileNotFoundException {
        super(id, message, privatePhoto, photoFilename, thumbnailFilename, key, photoIv, thumbnailIv);
        this.MessageID = messageID;
        this.Status = status;
        this.FriendID = friendID;
        this.Sent = sent;

        this.draftPhotoSource = d;
        this.plaintextPhotoFilename = plaintextPhotoFilename;
        this.plaintextThumbnailFilename = plaintextThumbnailFilename;
    }

    public SentPhoto(int status, String messageID, long friendID, String message, Boolean privatePhoto, long sent, DraftPhoto d) {
        this.MessageID = messageID;
        this.Status = status;
        this.FriendID = friendID;
        this.Sent = sent;
        this.Message = message;
        this.PrivatePhoto = privatePhoto;

        this.draftPhotoSource = d;
        this.plaintextPhotoFilename = null;
        this.plaintextThumbnailFilename = null;
    }

    public SentPhoto(int status, String messageID, long friendID, String message, Boolean privatePhoto, long sent, String plaintextPhotoFilename, String plaintextThumbnailFilename) {
        this.MessageID = messageID;
        this.Status = status;
        this.FriendID = friendID;
        this.Sent = sent;
        this.Message = message;
        this.PrivatePhoto = privatePhoto;

        this.draftPhotoSource = null;
        this.plaintextPhotoFilename = plaintextPhotoFilename;
        this.plaintextThumbnailFilename = plaintextThumbnailFilename;
    }

    public void EncryptAndSaveFiles(Context context) throws IOException {
        if(this.draftPhotoSource == null && this.plaintextPhotoFilename != null && this.plaintextThumbnailFilename != null) {
            //SEC: Random key and IVs
            byte[] key = new byte[16];
            RandomUtil.getBytes(key);
            byte[] photoIv = new byte[16];
            RandomUtil.getBytes(photoIv);
            byte[] thumbnailIv = new byte[16];
            RandomUtil.getBytes(thumbnailIv);

            //Photo --------------------------------------------------------------
            File plaintextImgFile = new File(this.plaintextPhotoFilename);
            this.photoFile = CryptoUtil.EncryptFileToFile(context, plaintextImgFile, key, photoIv);
            Log.d(TAG, "Finished encrypting fullsize " + this.plaintextPhotoFilename);
            plaintextImgFile.delete();

            //Thumbnail --------------------------------------------------------------
            File plaintextThumbnailFile = new File(this.plaintextThumbnailFilename);
            this.thumbnailFile = CryptoUtil.EncryptFileToFile(context, plaintextThumbnailFile, key, thumbnailIv);
            Log.d(TAG, "Finished encrypting thumb " + this.plaintextThumbnailFilename);
            plaintextThumbnailFile.delete();

            this.key = key;
            this.photoIv = photoIv;
            this.thumbnailIv = thumbnailIv;
        } else if(this.draftPhotoSource != null && this.plaintextPhotoFilename == null && this.plaintextThumbnailFilename == null) {
            //Just copy files to new filenames
            this.key = draftPhotoSource.key;
            this.photoIv = draftPhotoSource.photoIv;
            this.thumbnailIv = draftPhotoSource.thumbnailIv;

            this.photoFile = PhotoFileManager.createPermanentImageFile(context);
            PhotoFileManager.CopyFile(draftPhotoSource.photoFile, this.photoFile);

            this.thumbnailFile = PhotoFileManager.GetThumbnailFile(this.photoFile);
            PhotoFileManager.CopyFile(draftPhotoSource.thumbnailFile, this.thumbnailFile);
        } else {
            ACRA.getErrorReporter().handleException(new StrangeUsageException("Trying to save sent photo, but draft photo/plaintext photo sources are incorrect."));
        }

        //-----------------------------------------------------------------
        this.plaintextPhotoFilename = null;
        this.plaintextThumbnailFilename = null;
        this.draftPhotoSource = null;
    }

    public boolean ShouldBeDeleted() {
        if(!this.PrivatePhoto) return false;
        if(this.Status == SentPhotoStatus.Received && TimeUtil.GetNow() - (60 * 60 * 5) > this.Sent)
            return true;
        return false;
    }

    public Boolean FilesIntact()
    {
        if(this.photoFile != null && (!this.photoFile.exists() || !this.photoFile.canRead()))
            return false;
        if(this.thumbnailFile != null && (!this.thumbnailFile.exists() || !this.thumbnailFile.canRead()))
            return false;
        return true;
    }

    public static boolean DeleteFromDatabase(DBHelper dbHelper, long id)
    {
        boolean success = false;
        String selection = SentTableContract._ID + " LIKE ?";
        String[] selectionArgs = { String.valueOf(id) };
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            db.beginTransaction();
            if (1 == db.delete(SentTableContract.TABLE_NAME, selection, selectionArgs)) {
                success = true;
                db.setTransactionSuccessful();
            }
        } finally {
            db.endTransaction();
        }
        return success;
    }
    public static SentPhoto FromDatabase(UserProfile up, Cursor c, Boolean fetchThumbnail) throws FileNotFoundException {
        long id  = c.getLong(c.getColumnIndexOrThrow(SentTableContract._ID));
        long friendId = c.getLong(c.getColumnIndexOrThrow(SentTableContract.COLUMN_NAME_FRIENDID));
        String messageId = c.getString(c.getColumnIndexOrThrow(SentTableContract.COLUMN_NAME_MESSAGEID));
        long sent = c.getLong(c.getColumnIndexOrThrow(SentTableContract.COLUMN_NAME_SENT));
        String message = c.getString(c.getColumnIndexOrThrow(SentTableContract.COLUMN_NAME_MESSAGE));
        Boolean privatePhoto = 0 != c.getLong(c.getColumnIndexOrThrow(SentTableContract.COLUMN_NAME_PRIVATEPHOTO));
        int status = c.getInt(c.getColumnIndexOrThrow(SentTableContract.COLUMN_NAME_STATUS));
        String photoFilename = c.getString(c.getColumnIndexOrThrow(SentTableContract.COLUMN_NAME_PHOTOFILENAME));
        String thumbFilename = c.getString(c.getColumnIndexOrThrow(SentTableContract.COLUMN_NAME_THUMBNAILFILENAME));
        byte[] key = c.getBlob(c.getColumnIndexOrThrow(SentTableContract.COLUMN_NAME_KEY));
        byte[] photoIv = c.getBlob(c.getColumnIndexOrThrow(SentTableContract.COLUMN_NAME_PHOTOIV));
        byte[] thumbIv = c.getBlob(c.getColumnIndexOrThrow(SentTableContract.COLUMN_NAME_THUMBNAILIV));
        String plaintextPhotoFilename = c.getString(c.getColumnIndexOrThrow(SentTableContract.COLUMN_NAME_PLAINTEXTPHOTOFILENAME));
        String plaintextThumbnailFilename = c.getString(c.getColumnIndexOrThrow(SentTableContract.COLUMN_NAME_PLAINTEXTPHOTOFILENAME));
        long draftPhotoId = c.getLong(c.getColumnIndexOrThrow(SentTableContract.COLUMN_NAME_DRAFTPHOTOID));

        DraftPhoto draftPhoto = draftPhotoId == -1 ? null : up.findDraft(draftPhotoId);

        SentPhoto s = new SentPhoto(id, status, friendId, messageId, sent, message, privatePhoto,
                photoFilename, thumbFilename, key, photoIv, thumbIv, plaintextPhotoFilename, plaintextThumbnailFilename, draftPhoto);
        if(fetchThumbnail)
            up.getJobManager().addJob(new PhotoGetThumbnailJob(s));
        return s;
    }

    public boolean saveToDatabase(DBHelper dbHelper)
    {
        ContentValues values = new ContentValues();
        values.put(SentTableContract.COLUMN_NAME_FRIENDID, this.FriendID);
        values.put(SentTableContract.COLUMN_NAME_MESSAGEID, this.MessageID);
        values.put(SentTableContract.COLUMN_NAME_STATUS, this.Status);
        values.put(SentTableContract.COLUMN_NAME_MESSAGE, this.Message);
        values.put(SentTableContract.COLUMN_NAME_PRIVATEPHOTO, this.PrivatePhoto ? 1 : 0);
        values.put(SentTableContract.COLUMN_NAME_SENT, this.Sent);
        values.put(SentTableContract.COLUMN_NAME_PHOTOFILENAME, this.photoFile != null ? this.photoFile.getAbsolutePath() : "");
        values.put(SentTableContract.COLUMN_NAME_THUMBNAILFILENAME, this.thumbnailFile != null ? this.thumbnailFile.getAbsolutePath() : "");
        values.put(SentTableContract.COLUMN_NAME_KEY, this.key);
        values.put(SentTableContract.COLUMN_NAME_PHOTOIV, this.photoIv);
        values.put(SentTableContract.COLUMN_NAME_THUMBNAILIV, this.thumbnailIv);
        values.put(SentTableContract.COLUMN_NAME_PLAINTEXTPHOTOFILENAME, this.plaintextPhotoFilename);
        values.put(SentTableContract.COLUMN_NAME_PLAINTEXTTHUMBNAILFILENAME, this.plaintextThumbnailFilename);
        values.put(SentTableContract.COLUMN_NAME_DRAFTPHOTOID, this.draftPhotoSource != null ? this.draftPhotoSource.getID() : -1);

        if(this.id == -1) { //Inserting
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            try {
                db.beginTransaction();
                long id = db.insert(SentTableContract.TABLE_NAME,
                        "null",
                        values);
                if (id < 0) {
                    String msg = "Could not insert SentPhoto into database...";
                    Log.e(TAG, msg);
                    ACRA.getErrorReporter().handleException(new DatabaseException(msg));
                } else
                    db.setTransactionSuccessful();
                this.id = id;
            } finally {
                db.endTransaction();
            }
            return this.id > 0;
        } else { //Updating
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            String selection = SentTableContract._ID + " LIKE ?";
            String[] selectionArgs = { String.valueOf(id) };
            int rowsAffected;
            try {
                db.beginTransaction();
                rowsAffected = db.update(SentTableContract.TABLE_NAME, values, selection, selectionArgs);
                if (rowsAffected != 1) {
                    String msg = "Could not update SentPhoto in database...";
                    Log.e(TAG, msg);
                    ACRA.getErrorReporter().handleException(new DatabaseException(msg));
                } else
                    db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            return rowsAffected == 1;
        }
    }
}
