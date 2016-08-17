package invalid.showme.model.db;


import android.provider.BaseColumns;

public abstract class SentTableContract implements BaseColumns
{
    protected SentTableContract() {}

    public static final String TABLE_NAME = "sent";
    public static final String COLUMN_NAME_MESSAGEID = "message_id";
    public static final String COLUMN_NAME_STATUS = "status";
    public static final String COLUMN_NAME_FRIENDID = "friend_id";
    public static final String COLUMN_NAME_SENT = "sent";
    public static final String COLUMN_NAME_MESSAGE = "message";
    public static final String COLUMN_NAME_PRIVATEPHOTO = "privatePhoto";
    public static final String COLUMN_NAME_PHOTOFILENAME = "photo_filename";
    public static final String COLUMN_NAME_THUMBNAILFILENAME = "thumbnail_filename";
    public static final String COLUMN_NAME_KEY = "key";
    public static final String COLUMN_NAME_PHOTOIV = "photo_iv";
    public static final String COLUMN_NAME_THUMBNAILIV = "thumbnail_iv";
    
    //Needed for SentSaveJob 
    public static final String COLUMN_NAME_PLAINTEXTPHOTOFILENAME = "plaintext_photo_filename";
    public static final String COLUMN_NAME_PLAINTEXTTHUMBNAILFILENAME = "plaintext_thumbnail_filename";
    public static final String COLUMN_NAME_DRAFTPHOTOID = "draft_photo_id";


    private static String[] projection = {
            _ID,
            COLUMN_NAME_MESSAGEID,
            COLUMN_NAME_STATUS,
            COLUMN_NAME_FRIENDID,
            COLUMN_NAME_SENT,
            COLUMN_NAME_MESSAGE,
            COLUMN_NAME_PRIVATEPHOTO,
            COLUMN_NAME_PHOTOFILENAME,
            COLUMN_NAME_THUMBNAILFILENAME,
            COLUMN_NAME_KEY,
            COLUMN_NAME_PHOTOIV,
            COLUMN_NAME_THUMBNAILIV,
            COLUMN_NAME_PLAINTEXTPHOTOFILENAME,
            COLUMN_NAME_PLAINTEXTTHUMBNAILFILENAME,
            COLUMN_NAME_DRAFTPHOTOID
    };
    public static String[] GetDBProjection()
    {
        return SentTableContract.projection;
    }
}
