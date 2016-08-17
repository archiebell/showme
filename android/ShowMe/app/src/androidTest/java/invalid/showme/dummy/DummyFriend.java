package invalid.showme.dummy;

import android.content.Context;

import org.whispersystems.libaxolotl.AxolotlAddress;
import org.whispersystems.libaxolotl.ecc.ECPublicKey;
import org.whispersystems.libaxolotl.state.PreKeyBundle;

import java.util.ArrayList;
import java.util.List;

import invalid.showme.model.IFriend;
import invalid.showme.model.KeyFingerprint;
import invalid.showme.model.db.DBHelper;
import invalid.showme.model.photo.ReceivedPhoto;

public class DummyFriend implements IFriend
{
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

    public DummyFriend(String s, PreKeyBundle bundle) {
        this.displayName = s;
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

    public long getID() { return 1; }
    public String getDisplayName() { return this.displayName; }

    public PreKeyBundle getPreKeyBundle() { return this.preKeyBundle; }

    public ECPublicKey getPublicKey() { return this.publicKey; }
    public KeyFingerprint getFingerprint() { return this.fingerprint; }

    public AxolotlAddress getAxolotlAddress() { return new AxolotlAddress(this.fingerprint.toString(), this.deviceID); }

    @Override
    public void addPhoto(ReceivedPhoto p) { this.photos.add(p); }
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
    public boolean hasUnseenPhotos() { return false; }
    @Override
    public List<ReceivedPhoto> getPhotos()
    {
        return this.photos;
    }

    @Override
    public boolean saveToDatabase(Context context) { return true; }

    @Override
    public boolean DeleteFromDatabase(DBHelper dbHelper) { return true; }
}
