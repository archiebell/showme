package invalid.showme.model.db;


import android.provider.BaseColumns;

public abstract class FriendTableContract implements BaseColumns
{
    protected FriendTableContract() {}

    public static final String TABLE_NAME = "friends";
    public static final String COLUMN_NAME_DISPLAYNAME = "displayname";
    public static final String COLUMN_NAME_REGISTRATIONID = "regid";
    public static final String COLUMN_NAME_DEVICEID = "devid";
    public static final String COLUMN_NAME_PREKEYID = "pkid";
    public static final String COLUMN_NAME_PREKEY = "prekey";
    public static final String COLUMN_NAME_SIGNEDPREKEYID = "skid";
    public static final String COLUMN_NAME_SIGNEDPREKEY = "signedprekey";
    public static final String COLUMN_NAME_SIGNEDPREKEYSIGNATURE = "sksig";
    public static final String COLUMN_NAME_IDENTITYKEY = "pubkey";

    private static String[] projection = {
            FriendTableContract._ID,
            FriendTableContract.COLUMN_NAME_DISPLAYNAME,
            FriendTableContract.COLUMN_NAME_REGISTRATIONID,
            FriendTableContract.COLUMN_NAME_DEVICEID,
            FriendTableContract.COLUMN_NAME_PREKEYID,
            FriendTableContract.COLUMN_NAME_PREKEY,
            FriendTableContract.COLUMN_NAME_SIGNEDPREKEYID,
            FriendTableContract.COLUMN_NAME_SIGNEDPREKEY,
            FriendTableContract.COLUMN_NAME_SIGNEDPREKEYSIGNATURE,
            FriendTableContract.COLUMN_NAME_IDENTITYKEY
    };
    public static String[] GetDBProjection()
    {
        return FriendTableContract.projection;
    }
}
