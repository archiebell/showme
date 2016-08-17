package invalid.showme.model.db;

import android.provider.BaseColumns;

public abstract class PreKeyTableContract implements BaseColumns
{
    protected PreKeyTableContract() {}

    public static final String TABLE_NAME = "prekeys";
    public static final String COLUMN_NAME_KEY = "key";
    public static final String COLUMN_NAME_TYPE = "keytype";


    private static String[] projection = {
            PreKeyTableContract._ID,
            PreKeyTableContract.COLUMN_NAME_KEY,
            PreKeyTableContract.COLUMN_NAME_TYPE,
    };
    public static String[] GetDBProjection()
    {
        return PreKeyTableContract.projection;
    }
}
