package invalid.showme.model;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.acra.ACRA;
import org.whispersystems.libaxolotl.state.PreKeyRecord;
import org.whispersystems.libaxolotl.state.SignedPreKeyRecord;

import java.io.IOException;
import java.io.Serializable;

import invalid.showme.exceptions.DatabaseException;
import invalid.showme.model.db.DBHelper;
import invalid.showme.model.db.PreKeyTableContract;

public class PreKey implements Serializable
{
    private final String TAG = "PreKey";

    private long DatabaseID;
    public boolean IsSigned;

    public PreKeyRecord Record;
    public SignedPreKeyRecord SignedRecord;

    public PreKey(PreKeyRecord rec) {
        this.DatabaseID = -1;
        this.Record = rec;
        this.IsSigned = false;
    }

    public PreKey(SignedPreKeyRecord rec) {
        this.DatabaseID = -1;
        this.SignedRecord = rec;
        this.IsSigned = true;
    }

    private PreKey(long id, int type, byte[] key) throws IOException {
        this.DatabaseID = id;
        if(type == 0) {
            this.Record = new PreKeyRecord(key);
            this.IsSigned = false;
        } else {
            this.SignedRecord = new SignedPreKeyRecord(key);
            this.IsSigned = true;
        }
    }

    public static boolean DeleteFromDatabase(DBHelper dbHelper, PreKey pk)
    {
        boolean success = false;
        String selection = PreKeyTableContract._ID + " LIKE ?";
        String[] selectionArgs = { String.valueOf(pk.DatabaseID) };
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            db.beginTransaction();
            if (1 == db.delete(PreKeyTableContract.TABLE_NAME, selection, selectionArgs)) {
                success = true;
                db.setTransactionSuccessful();
            }
        } finally {
            db.endTransaction();
        }
        return success;
    }

    public static PreKey FromDatabase(Cursor c) throws IOException {
        long id  = c.getLong(c.getColumnIndexOrThrow(PreKeyTableContract._ID));
        byte[] key = c.getBlob(c.getColumnIndexOrThrow(PreKeyTableContract.COLUMN_NAME_KEY));
        int type = c.getInt(c.getColumnIndexOrThrow(PreKeyTableContract.COLUMN_NAME_TYPE));

        return new PreKey(id, type, key);
    }

    public boolean saveToDatabase(DBHelper dbHelper)
    {
        if(this.DatabaseID != -1) {
            String msg = "Tried to enter prekey into database that already has ID:" + this.DatabaseID;
            Log.e(TAG, msg);
            ACRA.getErrorReporter().handleException(new DatabaseException(msg));
            return true;
        }

        ContentValues values = new ContentValues();
        if(this.IsSigned) {
            values.put(PreKeyTableContract.COLUMN_NAME_KEY, this.SignedRecord.serialize());
            values.put(PreKeyTableContract.COLUMN_NAME_TYPE, 1);
        } else {
            values.put(PreKeyTableContract.COLUMN_NAME_KEY, this.Record.serialize());
            values.put(PreKeyTableContract.COLUMN_NAME_TYPE, 0);
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try
        {
            db.beginTransaction();
            long id = db.insert(PreKeyTableContract.TABLE_NAME,
                    "null",
                    values);
            if(id < 0) {
                String msg = "Could not insert prekey into database...";
                Log.e(TAG, msg);
                ACRA.getErrorReporter().handleException(new DatabaseException(msg));
            }
            else
                db.setTransactionSuccessful();
            this.DatabaseID = id;
        } finally {
            db.endTransaction();
        }
        return this.DatabaseID > 0;
    }
}
