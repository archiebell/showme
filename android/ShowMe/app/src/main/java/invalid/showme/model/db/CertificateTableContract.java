package invalid.showme.model.db;

import android.provider.BaseColumns;

public abstract class CertificateTableContract implements BaseColumns
{
    protected CertificateTableContract() {}

    public static final String TABLE_NAME = "certificates";
    public static final String COLUMN_NAME_CERTIFICATE = "certificate";
    public static final String COLUMN_NAME_PUBKEY = "key_public";
    public static final String COLUMN_NAME_PRIVKEY = "key_private";

    private static String[] projection = {
            CertificateTableContract._ID,
            CertificateTableContract.COLUMN_NAME_CERTIFICATE,
            CertificateTableContract.COLUMN_NAME_PUBKEY,
            CertificateTableContract.COLUMN_NAME_PRIVKEY,
    };
    public static String[] GetDBProjection()
    {
        return CertificateTableContract.projection;
    }
}