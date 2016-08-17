package invalid.showme.model.db;

import android.provider.BaseColumns;

public abstract class IdentityTableContract implements BaseColumns
{
    protected IdentityTableContract() {}

    public static final String TABLE_NAME = "identities";
    public static final String COLUMN_NAME_DISPLAYNAME = "displayname";
    public static final String COLUMN_NAME_PUBKEY = "key_public";
    public static final String COLUMN_NAME_PRIVKEY = "key_private";
    public static final String COLUMN_NAME_PREKEYCOUNTER = "prekeycounter";
    public static final String COLUMN_NAME_SIGNEDPREKEYCOUNTER = "signedprekeycounter";
    public static final String COLUMN_NAME_REGISTRATIONID = "registrationid";

    private static String[] projection = {
            IdentityTableContract._ID,
            IdentityTableContract.COLUMN_NAME_DISPLAYNAME,
            IdentityTableContract.COLUMN_NAME_PUBKEY,
            IdentityTableContract.COLUMN_NAME_PRIVKEY,
            IdentityTableContract.COLUMN_NAME_PREKEYCOUNTER,
            IdentityTableContract.COLUMN_NAME_SIGNEDPREKEYCOUNTER,
            IdentityTableContract.COLUMN_NAME_REGISTRATIONID
    };
    public static String[] GetDBProjection()
    {
        return IdentityTableContract.projection;
    }
}
