package invalid.showme.dummy;

import android.content.Context;

import org.acra.ACRA;
import org.whispersystems.libaxolotl.AxolotlAddress;
import org.whispersystems.libaxolotl.IdentityKey;
import org.whispersystems.libaxolotl.IdentityKeyPair;
import org.whispersystems.libaxolotl.InvalidKeyException;
import org.whispersystems.libaxolotl.InvalidKeyIdException;
import org.whispersystems.libaxolotl.ecc.ECKeyPair;
import org.whispersystems.libaxolotl.state.AxolotlStore;
import org.whispersystems.libaxolotl.state.PreKeyRecord;
import org.whispersystems.libaxolotl.state.SessionRecord;
import org.whispersystems.libaxolotl.state.SignedPreKeyRecord;
import org.whispersystems.libaxolotl.util.KeyHelper;

import java.util.ArrayList;
import java.util.List;

import invalid.showme.exceptions.StrangeUsageException;
import invalid.showme.model.IFriend;
import invalid.showme.model.IMe;
import invalid.showme.model.KeyFingerprint;
import invalid.showme.model.photo.DraftPhoto;

public class DummyMe implements IMe, AxolotlStore
{
    private String displayName;
    private ECKeyPair identityKey;
    private KeyFingerprint keyFP;
    private List<IFriend> friends;
    private List<DraftPhoto> drafts;

    public DummyMe(String dn, ECKeyPair kp)
    {
        this.setDisplayName(dn);
        this.setKeyPair(kp);
        this.initializeFriends();

        this.signedPreKeyStore = new DummySignedPreKeyStore();
        this.preKeyStore = new DummyPreKeyStore();
        this.sessionStore = new DummySessionStore();
    }

    private void setDisplayName(String dn) { this.displayName = dn; }
    public String getDisplayName() { return this.displayName; }

    private void setKeyPair(ECKeyPair kp)
    {
        this.identityKey = kp;
        this.keyFP = new KeyFingerprint(this.identityKey.getPublicKey());
    }
    public ECKeyPair getKeyPair() { return this.identityKey; }

    public KeyFingerprint getFingerprint() { return keyFP; }

    @Override
    public boolean saveToDatabase(Context context) { return true; }

    private void initializeFriends() { this.friends = new ArrayList<>(); }
    public void addFriend(IFriend f) {
        this.friends.add(f);
        this.saveIdentity(f.getAxolotlAddress().getName(), new IdentityKey(f.getPublicKey()));
    }
    public List<IFriend> getFriends() { return this.friends; }
    @Override
    public IFriend findFriend(long id) {
        for(int i=0; i<this.friends.size(); i++)
            if(this.friends.get(i).getID() == id)
                return this.friends.get(i);
        return null;
    }

    public void initializeDrafts() { this.drafts = new ArrayList<>(); }
    public List<DraftPhoto> getDrafts() { return this.drafts; }

    private DummySignedPreKeyStore signedPreKeyStore;
    private DummyPreKeyStore preKeyStore;
    private DummySessionStore sessionStore;
    
    //========================================================================================
    //Axolotl Store Interface
    public PreKeyRecord getNewPreKey() throws InvalidKeyException {
        return this.preKeyStore.getNewPreKey();
    }
    public SignedPreKeyRecord getSignedPrekey()
    {
        try {
            return this.signedPreKeyStore.getSignedPreKey(this.getIdentityKeyPair());
        } catch(InvalidKeyException e) {
            ACRA.getErrorReporter().handleException(new StrangeUsageException("Somehow own identity key invalid?", e));
        }
        return null;
    }

    @Override
    public IdentityKeyPair getIdentityKeyPair() {
        return new IdentityKeyPair(new IdentityKey(this.getKeyPair().getPublicKey()), this.getKeyPair().getPrivateKey());
    }

    private int localRegistrationId = KeyHelper.generateRegistrationId(false);
    @Override
    public int getLocalRegistrationId() {
        return this.localRegistrationId;
    }

    public int getLocalDeviceId() {
        //TODO maybe someday might support multiple devices.
        return 1;
    }

    @Override
    public void saveIdentity(String s, IdentityKey identityKey) {
        for(IFriend f: this.getFriends()) {
            if(f.getAxolotlAddress().getName().equals(s) && f.getPublicKey().equals(identityKey.getPublicKey()))
                return;
        }
        throw new RuntimeException("Somehow asked to save Identity did not already know about!");
    }

    @Override
    public boolean isTrustedIdentity(String s, IdentityKey identityKey) {
        for(IFriend f : this.getFriends()) {
            if(f.getAxolotlAddress().getName().equals(s) && f.getPublicKey().equals(identityKey.getPublicKey()))
                return true;
        }
        throw new RuntimeException("Somehow got untrusted identity!");
    }

    @Override
    public PreKeyRecord loadPreKey(int i) throws InvalidKeyIdException {
        return this.preKeyStore.loadPreKey(i);
    }

    @Override
    public void storePreKey(int i, PreKeyRecord preKeyRecord) {
        this.preKeyStore.storePreKey(i, preKeyRecord);
    }

    @Override
    public boolean containsPreKey(int i) {
        return this.preKeyStore.containsPreKey(i);
    }

    @Override
    public void removePreKey(int i) {
        this.preKeyStore.removePreKey(i);
    }

    @Override
    public SessionRecord loadSession(AxolotlAddress axolotlAddress) {
        return this.sessionStore.loadSession(axolotlAddress);
    }

    @Override
    public List<Integer> getSubDeviceSessions(String s) {
        return this.sessionStore.getSubDeviceSessions(s);
    }

    @Override
    public void storeSession(AxolotlAddress axolotlAddress, SessionRecord sessionRecord) {
        this.sessionStore.storeSession(axolotlAddress, sessionRecord);
    }

    @Override
    public boolean containsSession(AxolotlAddress axolotlAddress) {
        return this.sessionStore.containsSession(axolotlAddress);
    }

    @Override
    public void deleteSession(AxolotlAddress axolotlAddress) {
        this.sessionStore.deleteSession(axolotlAddress);
    }

    @Override
    public void deleteAllSessions(String s) {
        this.sessionStore.deleteAllSessions(s);
    }

    @Override
    public SignedPreKeyRecord loadSignedPreKey(int i) throws InvalidKeyIdException {
        return this.signedPreKeyStore.loadSignedPreKey(i);
    }

    @Override
    public List<SignedPreKeyRecord> loadSignedPreKeys() {
        return this.signedPreKeyStore.loadSignedPreKeys();
    }

    @Override
    public void storeSignedPreKey(int i, SignedPreKeyRecord signedPreKeyRecord) {
        this.signedPreKeyStore.storeSignedPreKey(i, signedPreKeyRecord);
    }

    @Override
    public boolean containsSignedPreKey(int i) {
        return this.signedPreKeyStore.containsSignedPreKey(i);
    }

    @Override
    public void removeSignedPreKey(int i) {
        this.signedPreKeyStore.removeSignedPreKey(i);
    }
}
