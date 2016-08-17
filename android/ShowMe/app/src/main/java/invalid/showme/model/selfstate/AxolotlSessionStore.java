package invalid.showme.model.selfstate;

import android.database.Cursor;

import org.acra.ACRA;
import org.whispersystems.libaxolotl.AxolotlAddress;
import org.whispersystems.libaxolotl.state.SessionRecord;
import org.whispersystems.libaxolotl.state.SessionStore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import invalid.showme.exceptions.StrangeUsageException;
import invalid.showme.model.AxolotlSession;
import invalid.showme.model.UserProfile;
import invalid.showme.model.db.DBHelper;
import invalid.showme.model.db.SessionTableContract;

public class AxolotlSessionStore implements SessionStore {

    private UserProfile context;
    private HashSet<AxolotlAddress> presentSessions;

    public AxolotlSessionStore(UserProfile up)
    {
        this.context = up;
        this.presentSessions = new HashSet<>();

        Cursor c = AxolotlSession.getAllSessions(this.context);
        while(c.moveToNext()) {
            String name = c.getString(c.getColumnIndexOrThrow(SessionTableContract.COLUMN_NAME_NAME));
            Integer deviceID = c.getInt(c.getColumnIndexOrThrow(SessionTableContract.COLUMN_NAME_DEVICEID));
            AxolotlAddress a = new AxolotlAddress(name, deviceID);
            this.presentSessions.add(a);
        }
    }

    @Override
    public SessionRecord loadSession(AxolotlAddress axolotlAddress) {
        if(!presentSessions.contains(axolotlAddress))
            return new SessionRecord();
        /*
         * It important that implementations return a copy of current durable information.  The
         * returned SessionRecord may be modified, but those changes should not have effect on the
         * durable session state (what returned by subsequent calls to method) without the
         * store method being called here first.
         */
        SessionRecord rec = AxolotlSession.getSession(this.context, axolotlAddress);
        if(rec == null)
            ACRA.getErrorReporter().handleException(new StrangeUsageException("Somehow presentSessions got out of sync with database..."));
        return rec;
    }

    @Override
    public List<Integer> getSubDeviceSessions(String name) {
        List<Integer> ret = new ArrayList<>();
        for(AxolotlAddress a : this.presentSessions) {
            if(a.getName().equals(name)) {
                ret.add(a.getDeviceId());
            }
        }
        return ret;
    }

    @Override
    public void storeSession(AxolotlAddress axolotlAddress, SessionRecord sessionRecord) {
        if(this.presentSessions.contains(axolotlAddress)) {
            AxolotlSession.UpdateDatabase(this.context, axolotlAddress, sessionRecord);//Ignore any error
        } else {
            this.presentSessions.add(axolotlAddress);
            AxolotlSession.SaveToDatabase(this.context, axolotlAddress, sessionRecord);//Ignore any error
        }
    }

    @Override
    public boolean containsSession(AxolotlAddress axolotlAddress) {
        return this.presentSessions.contains(axolotlAddress);
    }

    @Override
    public void deleteSession(AxolotlAddress axolotlAddress) {
        AxolotlSession.DeleteFromDatabase(DBHelper.getInstance(this.context), axolotlAddress); //Ignore any error
        presentSessions.remove(axolotlAddress);
    }

    @Override
    public void deleteAllSessions(String name) {
        AxolotlSession.DeleteFromDatabase(DBHelper.getInstance(this.context), name); //Ignore any error
        for(AxolotlAddress a : this.presentSessions) {
            if(a.getName().equals(name)) {
                this.presentSessions.remove(a);
            }
        }
    }
}
