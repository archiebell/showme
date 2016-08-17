package invalid.showme.model.db;


import android.provider.BaseColumns;

public abstract class PhotoTableContract implements BaseColumns
{
    protected PhotoTableContract() {}

    public static final String TABLE_NAME = "photos";
    public static final String COLUMN_NAME_MESSAGEID = "message_id";
    public static final String COLUMN_NAME_FRIENDID = "friend_id";
    public static final String COLUMN_NAME_RECEIVED = "received";
    public static final String COLUMN_NAME_FIRSTVIEWED  = "firstviewed";
    public static final String COLUMN_NAME_MESSAGE = "message";
    public static final String COLUMN_NAME_PRIVATEPHOTO = "privatePhoto";
    public static final String COLUMN_NAME_SEEN = "seen";
    public static final String COLUMN_NAME_PHOTOFILENAME = "photo_filename";
    public static final String COLUMN_NAME_THUMBNAILFILENAME = "thumbnail_filename";
    public static final String COLUMN_NAME_KEY = "key";
    public static final String COLUMN_NAME_PHOTOIV = "photo_iv";
    public static final String COLUMN_NAME_THUMBNAILIV = "thumbnail_iv";

    private static String[] projection = {
            _ID,
            COLUMN_NAME_MESSAGEID,
            COLUMN_NAME_FRIENDID,
            COLUMN_NAME_RECEIVED,
            COLUMN_NAME_FIRSTVIEWED ,
            COLUMN_NAME_MESSAGE,
            COLUMN_NAME_PRIVATEPHOTO,
            COLUMN_NAME_SEEN,
            COLUMN_NAME_PHOTOFILENAME,
            COLUMN_NAME_THUMBNAILFILENAME,
            COLUMN_NAME_KEY,
            COLUMN_NAME_PHOTOIV,
            COLUMN_NAME_THUMBNAILIV
    };
    public static String[] GetDBProjection()
    {
        return PhotoTableContract.projection;
    }
}
