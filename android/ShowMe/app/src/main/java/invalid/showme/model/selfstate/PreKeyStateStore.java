package invalid.showme.model.selfstate;

import android.database.Cursor;
import android.util.Log;
import android.widget.Toast;

import org.acra.ACRA;
import org.whispersystems.libaxolotl.IdentityKeyPair;
import org.whispersystems.libaxolotl.InvalidKeyException;
import org.whispersystems.libaxolotl.InvalidKeyIdException;
import org.whispersystems.libaxolotl.state.PreKeyRecord;
import org.whispersystems.libaxolotl.state.PreKeyStore;
import org.whispersystems.libaxolotl.state.SignedPreKeyRecord;
import org.whispersystems.libaxolotl.state.SignedPreKeyStore;
import org.whispersystems.libaxolotl.util.KeyHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import invalid.showme.exceptions.DatabaseException;
import invalid.showme.exceptions.StrangeUsageException;
import invalid.showme.model.PreKey;
import invalid.showme.model.UserProfile;
import invalid.showme.model.db.DBHelper;

public class PreKeyStateStore implements PreKeyStore, SignedPreKeyStore {
    private final String TAG = "PreKeyStateStore";

    public int PreKeyCounter;
    public int SignedPreKeyCounter;

    private List<PreKey> preKeys;
    private UserProfile context;

    public PreKeyStateStore(UserProfile up, int pkC, int spkC)
    {
        this.context = up;
        this.PreKeyCounter = pkC;
        this.SignedPreKeyCounter = spkC;
        this.preKeys = new ArrayList<>();

        Cursor prekeyRows = DBHelper.getPreKeys(context);
        while(prekeyRows.moveToNext()) {
            try {
                PreKey p = PreKey.FromDatabase(prekeyRows);
                this.preKeys.add(p);
            } catch (IOException e) {
                String msg = "caught IO Exception while loading PreKey from database.";
                Log.e(TAG, msg);
                ACRA.getErrorReporter().handleException(new DatabaseException(msg));
            }
        }
    }

    //====================================================================================
    // Unsigned PreKey

    @Override
    public PreKeyRecord loadPreKey(int i) throws InvalidKeyIdException {
        for(PreKey pk : this.preKeys)
            if(!pk.IsSigned && pk.Record.getId() == i)
                return pk.Record;
        throw new InvalidKeyIdException("PreKey ID " + i + " not found");
    }

    @Override
    public void storePreKey(int i, PreKeyRecord preKeyRecord) {
        if(i != preKeyRecord.getId()) {
            ACRA.getErrorReporter().handleException(new StrangeUsageException("What does it mean that ID in PreKeyRecord doesn't match id in function?"));
        }
        PreKey myPreKey = new PreKey(preKeyRecord);

        if (!myPreKey.saveToDatabase(DBHelper.getInstance(context))) {
            Toast.makeText(context, "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
        }
        PreKeyCounter++;
        if(!context.updatePreKeyCounters()) {
            Toast.makeText(context, "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
        }
        this.preKeys.add(myPreKey);
    }

    @Override
    public boolean containsPreKey(int i) {
        for(PreKey pk : this.preKeys)
            if(!pk.IsSigned && pk.Record.getId() == i)
                return true;
        return false;
    }

    @Override
    public void removePreKey(int i) {
        PreKey target = null;
        for(PreKey pk : this.preKeys)
            if(!pk.IsSigned && pk.Record.getId() == i)
                target = pk;
        if(target != null) {
            if(!PreKey.DeleteFromDatabase(DBHelper.getInstance(context), target))
                ACRA.getErrorReporter().handleException(new DatabaseException("Could not delete PreKey"));
            this.preKeys.remove(target);
        }
    }

    public PreKeyRecord getNewPreKey() {
        PreKeyRecord key = KeyHelper.generatePreKeys(PreKeyCounter, 1).get(0);
        PreKey myPreKey = new PreKey(key);

        if (!myPreKey.saveToDatabase(DBHelper.getInstance(context))) {
            Toast.makeText(context, "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
        }
        PreKeyCounter++;
        if(!context.updatePreKeyCounters()) {
            Toast.makeText(context, "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
        }
        this.preKeys.add(myPreKey);

        return key;
    }

    //====================================================================================
    //Signed PreKey

    @Override
    public List<SignedPreKeyRecord> loadSignedPreKeys() {
        List<SignedPreKeyRecord> ret = new ArrayList<>();
        for(PreKey pk : this.preKeys) {
            if(pk.IsSigned)
                ret.add(pk.SignedRecord);
        }
        return ret;
    }

    @Override
    public SignedPreKeyRecord loadSignedPreKey(int i) throws InvalidKeyIdException {
        for(PreKey pk : this.preKeys)
            if(pk.IsSigned && pk.SignedRecord.getId() == i)
                return pk.SignedRecord;
        throw new InvalidKeyIdException("SignedPreKey ID " + i + " not found");
    }

    @Override
    public void storeSignedPreKey(int i, SignedPreKeyRecord signedPreKeyRecord) {
        if(i != signedPreKeyRecord.getId()) {
            ACRA.getErrorReporter().handleException(new StrangeUsageException("What does it mean that ID in PreKeyRecord doesn't match id in function?"));
        }
        PreKey myPreKey = new PreKey(signedPreKeyRecord);

        if (!myPreKey.saveToDatabase(DBHelper.getInstance(context))) {
            Toast.makeText(context, "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
        }
        SignedPreKeyCounter++;
        if(!context.updatePreKeyCounters()) {
            Toast.makeText(context, "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
        }
        this.preKeys.add(myPreKey);
    }

    @Override
    public boolean containsSignedPreKey(int i) {
        for(PreKey pk : this.preKeys)
            if(pk.IsSigned && pk.SignedRecord.getId() == i)
                return true;
        return false;
    }

    @Override
    public void removeSignedPreKey(int i) {
        PreKey target = null;
        for(PreKey pk : this.preKeys)
            if(pk.IsSigned && pk.SignedRecord.getId() == i)
                target = pk;
        if(target != null) {
            if(!PreKey.DeleteFromDatabase(DBHelper.getInstance(context), target))
                ACRA.getErrorReporter().handleException(new DatabaseException("Could not delete PreKey"));
            this.preKeys.remove(target);
        }
    }

    public SignedPreKeyRecord getSignedPreKey(IdentityKeyPair idkp) throws InvalidKeyException {
        SignedPreKeyRecord rec = null;
        for(PreKey pk : this.preKeys) {
            if(pk.IsSigned)
                rec = pk.SignedRecord;
        }

        if(rec == null) {
            rec = KeyHelper.generateSignedPreKey(idkp, SignedPreKeyCounter);
            PreKey myPreKey = new PreKey(rec);

            if (!myPreKey.saveToDatabase(DBHelper.getInstance(context))) {
                Toast.makeText(context, "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
            }
            SignedPreKeyCounter++;
            if (!context.updatePreKeyCounters()) {
                Toast.makeText(context, "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
            }
            this.preKeys.add(myPreKey);
        }

        return rec;
    }
}
