package invalid.showme.model.server.clientcert;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.acra.ACRA;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

import invalid.showme.exceptions.DatabaseException;
import invalid.showme.model.db.CertificateTableContract;
import invalid.showme.model.db.DBHelper;
import invalid.showme.util.KeyUtil;

public class ClientCertificate
{
    private static final String TAG = "ClientCertificate";

    private long id;
    public KeyPair keyPair;
    public X509Certificate cert;

    private ClientCertificate(long id, KeyPair kp, X509Certificate cert) {
        this.id = id;
        this.keyPair = kp;
        this.cert = cert;
    }
    public ClientCertificate(KeyPair kp, X509Certificate cert) {
        this.id = -1;
        this.keyPair = kp;
        this.cert = cert;
    }

    public Boolean SaveToDatabase(Context context) {
        SQLiteDatabase db = DBHelper.getInstance(context).getWritableDatabase();
        try {
            db.beginTransaction();
            ContentValues values = new ContentValues();
            values.put(CertificateTableContract.COLUMN_NAME_PRIVKEY, this.keyPair.getPrivate().getEncoded());
            values.put(CertificateTableContract.COLUMN_NAME_PUBKEY, this.keyPair.getPublic().getEncoded());
            values.put(CertificateTableContract.COLUMN_NAME_CERTIFICATE, this.cert.getEncoded());

            long id = db.insert(CertificateTableContract.TABLE_NAME,
                    "null",
                    values);
            if(id < 0) {
                String msg = "Could not insert certificate into database...";
                Log.e(TAG, msg);
                ACRA.getErrorReporter().handleException(new DatabaseException(msg));
            }
            else
                db.setTransactionSuccessful();
            this.id = id;
        } catch(Exception e) {
            ACRA.getErrorReporter().handleException(e);
        }
        finally {
            db.endTransaction();
        }
        return this.id > 0;
    }

    public boolean DeleteFromDatabase(DBHelper dbHelper)
    {
        if(this.id < 0) return true;
        Boolean success = false;
        String selection = CertificateTableContract._ID + " LIKE ?";
        String[] selectionArgs = { String.valueOf(id) };
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            db.beginTransaction();
            if (1 == db.delete(CertificateTableContract.TABLE_NAME, selection, selectionArgs)) {
                success = true;
                db.setTransactionSuccessful();
            } else {
                String msg = "Could not delete certificate from database...";
                Log.e(TAG, msg);
                ACRA.getErrorReporter().handleException(new DatabaseException(msg));
            }
        } finally {
            db.endTransaction();
        }
        return success;
    }
    public static ClientCertificate FromDatabase(Cursor certCursor) {
        long id  = certCursor.getLong(certCursor.getColumnIndexOrThrow(CertificateTableContract._ID));
        byte[] privKey = certCursor.getBlob(certCursor.getColumnIndexOrThrow(CertificateTableContract.COLUMN_NAME_PRIVKEY));
        byte[] pubKey = certCursor.getBlob(certCursor.getColumnIndexOrThrow(CertificateTableContract.COLUMN_NAME_PUBKEY));
        byte[] cert = certCursor.getBlob(certCursor.getColumnIndexOrThrow(CertificateTableContract.COLUMN_NAME_CERTIFICATE));

        KeyPair kp = new KeyPair(KeyUtil.BytesToRSAPublicKey(pubKey), KeyUtil.BytesToRSAPrivateKey(privKey));
        X509Certificate c = KeyUtil.BytesToX509Cert(cert);

        return new ClientCertificate(id, kp, c);
    }
}
