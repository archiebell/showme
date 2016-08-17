package invalid.showme.dummy;

import org.whispersystems.libaxolotl.InvalidKeyException;
import org.whispersystems.libaxolotl.InvalidKeyIdException;
import org.whispersystems.libaxolotl.state.PreKeyRecord;
import org.whispersystems.libaxolotl.state.PreKeyStore;
import org.whispersystems.libaxolotl.util.KeyHelper;

import java.util.HashMap;

public class DummyPreKeyStore implements PreKeyStore {
    private HashMap<Integer, PreKeyRecord> preKeys;

    private int PreKeyCounter;

    public DummyPreKeyStore() {
        this.preKeys = new HashMap<>();
        this.PreKeyCounter = 1;
    }

    public PreKeyRecord getNewPreKey() throws InvalidKeyException {
        PreKeyRecord key = KeyHelper.generatePreKeys(PreKeyCounter, 1).get(0);

        if(this.preKeys.containsKey(key.getId()))
            throw new InvalidKeyException("Somehow trying to insert key already present!");
        this.preKeys.put(key.getId(), key);
        PreKeyCounter++;

        return key;
    }

    @Override
    public PreKeyRecord loadPreKey(int i) throws InvalidKeyIdException {
        if(this.preKeys.containsKey(i)) {
            return this.preKeys.get(i);
        }
        throw new InvalidKeyIdException("Invalid Key ID");
    }

    @Override
    public void storePreKey(int i, PreKeyRecord preKeyRecord) {
        this.preKeys.put(i, preKeyRecord);
    }

    @Override
    public boolean containsPreKey(int i) {
        return this.preKeys.containsKey(i);
    }

    @Override
    public void removePreKey(int i) {
        this.preKeys.remove(i);
    }
}
