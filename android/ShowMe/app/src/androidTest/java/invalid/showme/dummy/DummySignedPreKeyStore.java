package invalid.showme.dummy;

import org.whispersystems.libaxolotl.IdentityKeyPair;
import org.whispersystems.libaxolotl.InvalidKeyException;
import org.whispersystems.libaxolotl.InvalidKeyIdException;
import org.whispersystems.libaxolotl.state.SignedPreKeyRecord;
import org.whispersystems.libaxolotl.state.SignedPreKeyStore;
import org.whispersystems.libaxolotl.util.KeyHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DummySignedPreKeyStore implements SignedPreKeyStore {
    private HashMap<Integer, SignedPreKeyRecord> signedPreKeys;
    private int SignedPreKeyCounter;

    public DummySignedPreKeyStore() {
        this.signedPreKeys = new HashMap<>();
        this.SignedPreKeyCounter = 1;
    }

    public SignedPreKeyRecord getSignedPreKey(IdentityKeyPair idkp) throws InvalidKeyException {
        if(this.signedPreKeys.size() > 0) {
            return this.signedPreKeys.values().iterator().next();
        }

        SignedPreKeyRecord rec = null;
        rec = KeyHelper.generateSignedPreKey(idkp, SignedPreKeyCounter);
        SignedPreKeyCounter++;
        if(this.signedPreKeys.containsKey(rec.getId()))
            throw new InvalidKeyException("Somehow trying to insert key already present!");
        this.signedPreKeys.put(rec.getId(), rec);

        return rec;
    }

    @Override
    public SignedPreKeyRecord loadSignedPreKey(int i) throws InvalidKeyIdException {
        return this.signedPreKeys.get(i);
    }

    @Override
    public List<SignedPreKeyRecord> loadSignedPreKeys() {
        List<SignedPreKeyRecord> ret = new ArrayList<>();
        ret.addAll(this.signedPreKeys.values());
        return ret;
    }

    @Override
    public void storeSignedPreKey(int i, SignedPreKeyRecord signedPreKeyRecord) {
        this.signedPreKeys.put(i, signedPreKeyRecord);
    }

    @Override
    public boolean containsSignedPreKey(int i) {
        return this.signedPreKeys.containsKey(i);
    }

    @Override
    public void removeSignedPreKey(int i) {
        this.signedPreKeys.remove(i);
    }
}
