package invalid.showme.model.db;

import android.provider.BaseColumns;

public class SessionTableContract implements BaseColumns {
    protected SessionTableContract() {}

    public static final String TABLE_NAME = "sessions";
    public static final String COLUMN_NAME_NAME = "name";
    public static final String COLUMN_NAME_DEVICEID = "deviceid";
    public static final String COLUMN_NAME_SESSION = "session";

    private static String[] projection = {
            _ID,
            COLUMN_NAME_NAME,
            COLUMN_NAME_DEVICEID,
            COLUMN_NAME_SESSION
    };
    public static String[] GetDBProjection()
    {
        return SessionTableContract.projection;
    }
}
