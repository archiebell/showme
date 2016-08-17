package invalid.showme.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.acra.ACRA;
import org.whispersystems.libaxolotl.AxolotlAddress;
import org.whispersystems.libaxolotl.state.SessionRecord;

import java.io.IOException;

import invalid.showme.exceptions.DatabaseException;
import invalid.showme.model.db.DBHelper;
import invalid.showme.model.db.SessionTableContract;

public class AxolotlSession
{
    private static final String TAG = "AxolotlSession";

    public static Cursor getAllSessions(UserProfile context) {
        DBHelper helper = DBHelper.getInstance(context);
        SQLiteDatabase db = helper.getReadableDatabase();

        return db.query(SessionTableContract.TABLE_NAME,
                SessionTableContract.GetDBProjection(),
                null,
                null,
                null,
                null,
                null);
    }

    public static SessionRecord getSession(Context context, AxolotlAddress a)
    {
        DBHelper helper = DBHelper.getInstance(context);
        SQLiteDatabase db = helper.getReadableDatabase();

        String selection = SessionTableContract.COLUMN_NAME_NAME + " = ? AND " + SessionTableContract.COLUMN_NAME_DEVICEID + " = ?";
        String[] selectionArgs = { String.valueOf(a.getName()), Integer.valueOf(a.getDeviceId()).toString() };

        Cursor c = db.query(SessionTableContract.TABLE_NAME,
                SessionTableContract.GetDBProjection(),
                selection,
                selectionArgs,
                null,
                null,
                null);

        while(c.moveToNext()) {
            try {
                byte[] serializedSession = c.getBlob(c.getColumnIndexOrThrow(SessionTableContract.COLUMN_NAME_SESSION));
                return new SessionRecord(serializedSession);
            } catch (IOException e) {
                ACRA.getErrorReporter().handleException(new DatabaseException("Could not deserialized session."));
            }
        }
        c.close();
        return null;
    }

    public static boolean SaveToDatabase(Context context, AxolotlAddress axolotlAddress, SessionRecord rec)
    {
        long id = -1;
        SQLiteDatabase db = DBHelper.getInstance(context).getWritableDatabase();
        try {
            db.beginTransaction();
            ContentValues values = new ContentValues();
            values.put(SessionTableContract.COLUMN_NAME_NAME, axolotlAddress.getName());
            values.put(SessionTableContract.COLUMN_NAME_DEVICEID, axolotlAddress.getDeviceId());
            values.put(SessionTableContract.COLUMN_NAME_SESSION, rec.serialize());

            id = db.insert(SessionTableContract.TABLE_NAME,
                    "null",
                    values);
            if(id < 0) {
                String msg = "Could not insert session into database...";
                Log.e(TAG, msg);
                ACRA.getErrorReporter().handleException(new DatabaseException(msg));
            }
            else
                db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }
        return id > 0;
    }

    public static boolean UpdateDatabase(Context context, AxolotlAddress axolotlAddress, SessionRecord rec)
    {
        SQLiteDatabase db = DBHelper.getInstance(context).getWritableDatabase();
        String selection = SessionTableContract.COLUMN_NAME_NAME + " = ? AND " + SessionTableContract.COLUMN_NAME_DEVICEID + " = ?";
        String[] selectionArgs = { String.valueOf(axolotlAddress.getName()), Integer.valueOf(axolotlAddress.getDeviceId()).toString() };

        ContentValues values = new ContentValues();
        values.put(SessionTableContract.COLUMN_NAME_SESSION, rec.serialize());

        int rowsAffected;
        try {
            db.beginTransaction();
            rowsAffected = db.update(SessionTableContract.TABLE_NAME, values, selection, selectionArgs);
            if (rowsAffected != 1) {
                String msg = "Could not update session in database...";
                Log.e(TAG, msg);
                ACRA.getErrorReporter().handleException(new DatabaseException(msg));
            } else
                db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return rowsAffected == 1;
    }

    public static boolean DeleteFromDatabase(DBHelper dbHelper, AxolotlAddress a)
    {
        Boolean success = false;
        String selection = SessionTableContract.COLUMN_NAME_NAME + " = ? AND " + SessionTableContract.COLUMN_NAME_DEVICEID + " = ?";
        String[] selectionArgs = { String.valueOf(a.getName()), Integer.valueOf(a.getDeviceId()).toString() };
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            db.beginTransaction();
            if (1 == db.delete(SessionTableContract.TABLE_NAME, selection, selectionArgs)) {
                success = true;
                db.setTransactionSuccessful();
            } else {
                String msg = "Could not delete session from database...";
                Log.e(TAG, msg);
                ACRA.getErrorReporter().handleException(new DatabaseException(msg));
            }
        } finally {
            db.endTransaction();
        }
        return success;
    }

    public static boolean DeleteFromDatabase(DBHelper dbHelper, String name)
    {
        Boolean success = false;
        String selection = SessionTableContract.COLUMN_NAME_NAME + " = ?";
        String[] selectionArgs = { String.valueOf(name) };
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            db.beginTransaction();
            if (db.delete(SessionTableContract.TABLE_NAME, selection, selectionArgs) >= 1) {
                success = true;
                db.setTransactionSuccessful();
            } else {
                String msg = "Could not delete sessions from database...";
                Log.e(TAG, msg);
                ACRA.getErrorReporter().handleException(new DatabaseException(msg));
            }
        } finally {
            db.endTransaction();
        }
        return success;
    }

}
