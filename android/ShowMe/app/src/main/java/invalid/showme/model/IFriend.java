package invalid.showme.model;

import android.content.Context;

import org.whispersystems.libaxolotl.AxolotlAddress;
import org.whispersystems.libaxolotl.ecc.ECPublicKey;
import org.whispersystems.libaxolotl.state.PreKeyBundle;

import java.util.List;

import invalid.showme.model.db.DBHelper;
import invalid.showme.model.photo.ReceivedPhoto;

public interface IFriend {
    long getID();
    String getDisplayName();

    ECPublicKey getPublicKey();

    PreKeyBundle getPreKeyBundle();
    KeyFingerprint getFingerprint();
    AxolotlAddress getAxolotlAddress();

    void addPhoto(ReceivedPhoto p);
    void deletePhoto(String messageID);
    boolean hasPhoto(String messageID);
    List<ReceivedPhoto> getPhotos();
    boolean hasUnseenPhotos();

    boolean saveToDatabase(Context context);
    boolean DeleteFromDatabase(DBHelper dbHelper);

}
