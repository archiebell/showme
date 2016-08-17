package invalid.showme.model;

import android.content.Context;

import org.whispersystems.libaxolotl.ecc.ECKeyPair;
import org.whispersystems.libaxolotl.state.AxolotlStore;

import java.util.List;

import invalid.showme.model.photo.DraftPhoto;

public interface IMe extends AxolotlStore
{
    String getDisplayName();
    ECKeyPair getKeyPair();
    KeyFingerprint getFingerprint();

    void addFriend(IFriend f);
    List<IFriend> getFriends();
    IFriend findFriend(long id);

    List<DraftPhoto> getDrafts();


    boolean saveToDatabase(Context context);
}
