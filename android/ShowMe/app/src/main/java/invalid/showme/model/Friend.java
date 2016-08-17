package invalid.showme.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.acra.ACRA;
import org.whispersystems.libaxolotl.AxolotlAddress;
import org.whispersystems.libaxolotl.IdentityKey;
import org.whispersystems.libaxolotl.InvalidKeyException;
import org.whispersystems.libaxolotl.ecc.Curve;
import org.whispersystems.libaxolotl.ecc.ECPublicKey;
import org.whispersystems.libaxolotl.state.PreKeyBundle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import invalid.showme.exceptions.DatabaseException;
import invalid.showme.model.db.DBHelper;
import invalid.showme.model.db.FriendTableContract;
import invalid.showme.model.photo.ReceivedPhoto;

public class Friend implements IFriend, Serializable {
    final private static String TAG = "Friend";

    private long id;
    private String displayName;

    private PreKeyBundle preKeyBundle;
    private int registrationID;
    private int deviceID;
    private ECPublicKey publicKey;
    private int preKeyID;
    private ECPublicKey preKey;
    private int signedPreKeyID;
    private ECPublicKey signedPreKey;
    private byte[] signedPreKeySignature;

    private KeyFingerprint fingerprint;
    private List<ReceivedPhoto> photos;

    public Friend(String s) {
        this.displayName = s;
        this.fingerprint = null;
        this.photos = new ArrayList<>();
    }
    public Friend(String s, PreKeyBundle bundle) {
        this(-1, s, bundle);
    }
    private Friend(long id, String dn, PreKeyBundle bundle)
    {
        this.id = id;
        this.displayName = dn;
        this.photos = new ArrayList<>();

        this.preKeyBundle = bundle;
        this.publicKey = bundle.getIdentityKey().getPublicKey();
        this.fingerprint = new KeyFingerprint(this.publicKey);

        this.deviceID = bundle.getDeviceId();
        this.registrationID = bundle.getRegistrationId();

        this.preKeyID = bundle.getPreKeyId();
        this.preKey = bundle.getPreKey();

        this.signedPreKeyID = bundle.getSignedPreKeyId();
        this.signedPreKey = bundle.getSignedPreKey();
        this.signedPreKeySignature = bundle.getSignedPreKeySignature();
    }

    public long getID() { return this.id; }
    public String getDisplayName() { return this.displayName; }

    @Override
    public PreKeyBundle getPreKeyBundle() { return this.preKeyBundle; }

    public ECPublicKey getPublicKey() { return this.publicKey; }


    public KeyFingerprint getFingerprint() { return this.fingerprint; }

    public AxolotlAddress getAxolotlAddress() { return new AxolotlAddress(this.fingerprint.toString(), this.deviceID); }


    @Override
    public void addPhoto(ReceivedPhoto p) { this.photos.add(0, p); }
    @Override
    public void deletePhoto(String messageID) {
        for(ReceivedPhoto p : this.photos)
            if(p.MessageID.equals(messageID)) {
                this.photos.remove(p);
                break;
            }
    }
    @Override
    public boolean hasPhoto(String messageID) {
        for(ReceivedPhoto p : this.photos)
            if(p.MessageID.equals(messageID))
                return true;
        return false;
    }
    @Override
    public List<ReceivedPhoto> getPhotos()
    {
        return this.photos;
    }

    @Override
    public boolean hasUnseenPhotos() {
        for(ReceivedPhoto p : this.photos)
            if(!p.Seen) return true;
        return false;
    }

    @Override
    public boolean saveToDatabase(Context context)
    {
        SQLiteDatabase db = DBHelper.getInstance(context).getWritableDatabase();
        try {
            db.beginTransaction();
            ContentValues values = new ContentValues();
            values.put(FriendTableContract.COLUMN_NAME_DISPLAYNAME, this.displayName);
            values.put(FriendTableContract.COLUMN_NAME_REGISTRATIONID, this.registrationID);
            values.put(FriendTableContract.COLUMN_NAME_DEVICEID, this.deviceID);
            values.put(FriendTableContract.COLUMN_NAME_PREKEYID, this.preKeyID);
            values.put(FriendTableContract.COLUMN_NAME_PREKEY, this.preKey.serialize());
            values.put(FriendTableContract.COLUMN_NAME_SIGNEDPREKEYID, this.signedPreKeyID);
            values.put(FriendTableContract.COLUMN_NAME_SIGNEDPREKEY, this.signedPreKey.serialize());
            values.put(FriendTableContract.COLUMN_NAME_SIGNEDPREKEYSIGNATURE, this.signedPreKeySignature);
            values.put(FriendTableContract.COLUMN_NAME_IDENTITYKEY, this.publicKey.serialize());

            long id = db.insert(FriendTableContract.TABLE_NAME,
                    "null",
                    values);
            if(id < 0) {
                String msg = "Could not insert Friend into database...";
                Log.e(TAG, msg);
                ACRA.getErrorReporter().handleException(new DatabaseException(msg));
            }
            else
                db.setTransactionSuccessful();
            this.id = id;
        }
        finally {
            db.endTransaction();
        }
        return this.id > 0;
    }
    @Override
    public boolean DeleteFromDatabase(DBHelper dbHelper)
    {
        Boolean success = false;
        String selection = FriendTableContract._ID + " LIKE ?";
        String[] selectionArgs = { String.valueOf(id) };
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            db.beginTransaction();
            if (1 == db.delete(FriendTableContract.TABLE_NAME, selection, selectionArgs)) {
                success = true;
                db.setTransactionSuccessful();
            } else {
                String msg = "Could not delete Friend from database...";
                Log.e(TAG, msg);
                ACRA.getErrorReporter().handleException(new DatabaseException(msg));
            }
        } finally {
            db.endTransaction();
        }
        return success;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof Friend)
        {
            Friend f = (Friend)o;
            return this.publicKey.equals(f.publicKey);
        }
        return false;
    }

    public static IFriend FromDatabase(Cursor friends) throws InvalidKeyException
    {
        long id  = friends.getLong(friends.getColumnIndexOrThrow(FriendTableContract._ID));
        String displayName = friends.getString(friends.getColumnIndexOrThrow(FriendTableContract.COLUMN_NAME_DISPLAYNAME));
        int regid = friends.getInt(friends.getColumnIndexOrThrow(FriendTableContract.COLUMN_NAME_REGISTRATIONID));
        int devid = friends.getInt(friends.getColumnIndexOrThrow(FriendTableContract.COLUMN_NAME_DEVICEID));
        int pkid = friends.getInt(friends.getColumnIndexOrThrow(FriendTableContract.COLUMN_NAME_PREKEYID));
        byte[] preKey = friends.getBlob(friends.getColumnIndexOrThrow(FriendTableContract.COLUMN_NAME_PREKEY));
        int skid = friends.getInt(friends.getColumnIndexOrThrow(FriendTableContract.COLUMN_NAME_SIGNEDPREKEYID));
        byte[] signedPreKey = friends.getBlob(friends.getColumnIndexOrThrow(FriendTableContract.COLUMN_NAME_SIGNEDPREKEY));
        byte[] sksig = friends.getBlob(friends.getColumnIndexOrThrow(FriendTableContract.COLUMN_NAME_SIGNEDPREKEYSIGNATURE));
        byte[] pubKey = friends.getBlob(friends.getColumnIndexOrThrow(FriendTableContract.COLUMN_NAME_IDENTITYKEY));

        PreKeyBundle bundle = new PreKeyBundle(regid, devid, pkid, Curve.decodePoint(preKey, 0), skid, Curve.decodePoint(signedPreKey, 0), sksig, new IdentityKey(Curve.decodePoint(pubKey, 0)));
        return new Friend(id, displayName, bundle);
    }
}
