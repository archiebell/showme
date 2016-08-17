package invalid.showme.model.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper
{
    // adb exec-out run-as invalid.showme cat /data/data/invalid.showme/databases/ShowMe.db > data.db
    final private static String TAG = "DBHelper";

    private static  DBHelper instance = null;
    public static DBHelper getInstance(Context context)
    {
        if(DBHelper.instance == null) DBHelper.instance = new DBHelper(context);
        return DBHelper.instance;
    }

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "ShowMe.db";
    private DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    //----------------------------------------------------------------------------------------------
    @Override
    public void onCreate(SQLiteDatabase db) {
        createEverything(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "are upgrading from " + oldVersion + " to " + newVersion);

        dropEverything(db);
        createEverything(db);
    }

    public static void wipeEverything(SQLiteDatabase db)
    {
        dropEverything(db);
        createEverything(db);
    }

    private static void dropEverything(SQLiteDatabase db)
    {
        db.execSQL("DROP TABLE IF EXISTS " + IdentityTableContract.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + PreKeyTableContract.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + SessionTableContract.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + FriendTableContract.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + DraftTableContract.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + PhotoTableContract.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + SentTableContract.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + CertificateTableContract.TABLE_NAME);
    }

    private static void createEverything(SQLiteDatabase db)
    {
        db.execSQL("CREATE TABLE " + IdentityTableContract.TABLE_NAME + " (" +
                        IdentityTableContract._ID + " INTEGER PRIMARY KEY, " +
                        IdentityTableContract.COLUMN_NAME_DISPLAYNAME+ " TEXT, " +
                        IdentityTableContract.COLUMN_NAME_PUBKEY + " BLOB, " +
                        IdentityTableContract.COLUMN_NAME_PRIVKEY + " BLOB, " +
                        IdentityTableContract.COLUMN_NAME_PREKEYCOUNTER + " INTEGER, " +
                        IdentityTableContract.COLUMN_NAME_SIGNEDPREKEYCOUNTER + " INTEGER, " +
                        IdentityTableContract.COLUMN_NAME_REGISTRATIONID + " INTEGER " +
                        ") "
        );
        db.execSQL("CREATE TABLE " + PreKeyTableContract.TABLE_NAME + " (" +
                        PreKeyTableContract._ID + " INTEGER PRIMARY KEY, " +
                        PreKeyTableContract.COLUMN_NAME_KEY + " BLOB, " +
                        PreKeyTableContract.COLUMN_NAME_TYPE + " INTEGER " +
                        ") "
        );
        db.execSQL("CREATE TABLE " + SessionTableContract.TABLE_NAME + " (" +
                        SessionTableContract._ID + " INTEGER PRIMARY KEY, " +
                        SessionTableContract.COLUMN_NAME_NAME + " TEXT, " +
                        SessionTableContract.COLUMN_NAME_DEVICEID + " INTEGER, " +
                        SessionTableContract.COLUMN_NAME_SESSION + " BLOG " +
                        ") "
        );
        db.execSQL("CREATE TABLE " + CertificateTableContract.TABLE_NAME + " (" +
                        CertificateTableContract._ID + " INTEGER PRIMARY KEY, " +
                        CertificateTableContract.COLUMN_NAME_CERTIFICATE + " BLOB, " +
                        CertificateTableContract.COLUMN_NAME_PUBKEY + " BLOB, " +
                        CertificateTableContract.COLUMN_NAME_PRIVKEY + " BLOB " +
                        ") "
        );
        db.execSQL("CREATE TABLE " + FriendTableContract.TABLE_NAME + " (" +
                        FriendTableContract._ID + " INTEGER PRIMARY KEY, " +
                        FriendTableContract.COLUMN_NAME_DISPLAYNAME + " TEXT, " +
                        FriendTableContract.COLUMN_NAME_REGISTRATIONID + " INTEGER, " +
                        FriendTableContract.COLUMN_NAME_DEVICEID + " INTEGER, " +
                        FriendTableContract.COLUMN_NAME_PREKEYID + " INTEGER, " +
                        FriendTableContract.COLUMN_NAME_PREKEY + " BLOB, " +
                        FriendTableContract.COLUMN_NAME_SIGNEDPREKEYID + " INTEGER, " +
                        FriendTableContract.COLUMN_NAME_SIGNEDPREKEY + " BLOB, " +
                        FriendTableContract.COLUMN_NAME_SIGNEDPREKEYSIGNATURE + " BLOB, " +
                        FriendTableContract.COLUMN_NAME_IDENTITYKEY + " BLOB " +
                        ") "
        );
        db.execSQL("CREATE TABLE " + DraftTableContract.TABLE_NAME + " (" +
                        DraftTableContract._ID + " INTEGER PRIMARY KEY, " +
                        DraftTableContract.COLUMN_NAME_PHOTOFILENAME + " TEXT, " +
                        DraftTableContract.COLUMN_NAME_THUMBNAILFILENAME + " TEXT, " +
                        DraftTableContract.COLUMN_NAME_MESSAGE + " TEXT, " +
                        DraftTableContract.COLUMN_NAME_PRIVATEPHOTO + " INTEGER, " +
                        DraftTableContract.COLUMN_NAME_KEY + " BLOB, " +
                        DraftTableContract.COLUMN_NAME_PHOTOIV + " BLOB, " +
                        DraftTableContract.COLUMN_NAME_THUMBNAILIV + " BLOB " +
                        ") "
        );
        db.execSQL("CREATE TABLE " + SentTableContract.TABLE_NAME + " (" +
                        SentTableContract._ID + " INTEGER PRIMARY KEY, " +
                        SentTableContract.COLUMN_NAME_MESSAGEID + " TEXT, " +
                        SentTableContract.COLUMN_NAME_STATUS + " INTEGER, " +
                        SentTableContract.COLUMN_NAME_FRIENDID + " INTEGER, " +
                        SentTableContract.COLUMN_NAME_SENT + " INTEGER, " +
                        SentTableContract.COLUMN_NAME_MESSAGE + " TEXT, " +
                        SentTableContract.COLUMN_NAME_PRIVATEPHOTO + " INTEGER, " +
                        SentTableContract.COLUMN_NAME_PHOTOFILENAME + " TEXT, " +
                        SentTableContract.COLUMN_NAME_THUMBNAILFILENAME + " TEXT, " +
                        SentTableContract.COLUMN_NAME_KEY + " BLOB, " +
                        SentTableContract.COLUMN_NAME_PHOTOIV + " BLOB, " +
                        SentTableContract.COLUMN_NAME_THUMBNAILIV + " BLOB, " +
                        SentTableContract.COLUMN_NAME_PLAINTEXTPHOTOFILENAME + " TEXT, " +
                        SentTableContract.COLUMN_NAME_PLAINTEXTTHUMBNAILFILENAME + " TEXT, " +
                        SentTableContract.COLUMN_NAME_DRAFTPHOTOID + " INTEGER " +
                        ") "
        );
        db.execSQL("CREATE TABLE " + PhotoTableContract.TABLE_NAME + " (" +
                        PhotoTableContract._ID + " INTEGER PRIMARY KEY, " +
                        PhotoTableContract.COLUMN_NAME_MESSAGEID + " TEXT, " +
                        PhotoTableContract.COLUMN_NAME_FRIENDID + " INTEGER, " +
                        PhotoTableContract.COLUMN_NAME_RECEIVED + " INTEGER, " +
                        PhotoTableContract.COLUMN_NAME_FIRSTVIEWED + " INTEGER, " +
                        PhotoTableContract.COLUMN_NAME_MESSAGE + " TEXT, " +
                        PhotoTableContract.COLUMN_NAME_PRIVATEPHOTO + " INTEGER, " +
                        PhotoTableContract.COLUMN_NAME_SEEN + " INTEGER, " +
                        PhotoTableContract.COLUMN_NAME_PHOTOFILENAME + " TEXT, " +
                        PhotoTableContract.COLUMN_NAME_THUMBNAILFILENAME + " TEXT, " +
                        PhotoTableContract.COLUMN_NAME_KEY + " BLOB, " +
                        PhotoTableContract.COLUMN_NAME_PHOTOIV + " BLOB, " +
                        PhotoTableContract.COLUMN_NAME_THUMBNAILIV + " BLOB " +
                        ") "
        );
    }

    //----------------------------------------------------------------------------------------------

    public static Cursor getIdentities(Context context)
    {
        DBHelper helper = DBHelper.getInstance(context);
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(IdentityTableContract.TABLE_NAME,
                IdentityTableContract.GetDBProjection(),
                null,
                null,
                null,
                null,
                null);
        c.moveToFirst();
        return c;
    }

    public static Cursor getPreKeys(Context context)
    {
        DBHelper helper = DBHelper.getInstance(context);
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(PreKeyTableContract.TABLE_NAME,
                PreKeyTableContract.GetDBProjection(),
                null,
                null,
                null,
                null,
                null);
        c.moveToFirst();
        return c;
    }

    public static Cursor getCertificates(Context context) {
        DBHelper helper = DBHelper.getInstance(context);
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(CertificateTableContract.TABLE_NAME,
                CertificateTableContract.GetDBProjection(),
                null,
                null,
                null,
                null,
                null);
        c.moveToFirst();
        return c;
    }

    public static Cursor getDrafts(Context context)
    {
        DBHelper helper = DBHelper.getInstance(context);
        SQLiteDatabase db = helper.getReadableDatabase();
        return db.query(DraftTableContract.TABLE_NAME,
                DraftTableContract.GetDBProjection(),
                null,
                null,
                null,
                null,
                null);
    }

    public static Cursor getSent(Context context)
    {
        DBHelper helper = DBHelper.getInstance(context);
        SQLiteDatabase db = helper.getReadableDatabase();
        return db.query(SentTableContract.TABLE_NAME,
                SentTableContract.GetDBProjection(),
                null,
                null,
                null,
                null,
                null);
    }

    public static Cursor getFriends(Context context)
    {
        DBHelper helper = DBHelper.getInstance(context);
        SQLiteDatabase db = helper.getReadableDatabase();
        return db.query(FriendTableContract.TABLE_NAME,
                FriendTableContract.GetDBProjection(),
                null,
                null,
                null,
                null,
                null);
    }

    public static Cursor getAllPhotos(Context context)
    {
        DBHelper helper = DBHelper.getInstance(context);
        SQLiteDatabase db = helper.getReadableDatabase();
        return db.query(PhotoTableContract.TABLE_NAME,
                PhotoTableContract.GetDBProjection(),
                null,
                null,
                null,
                null,
                null);
    }
    public static Cursor getPhotosForFriend(Context context, long id)
    {
        DBHelper helper = DBHelper.getInstance(context);
        SQLiteDatabase db = helper.getReadableDatabase();

        String selection = PhotoTableContract.COLUMN_NAME_FRIENDID + " LIKE ?";
        String[] selectionArgs = { String.valueOf(id) };

        return db.query(PhotoTableContract.TABLE_NAME,
                PhotoTableContract.GetDBProjection(),
                selection,
                selectionArgs,
                null,
                null,
                null);
    }
}
