package invalid.showme.dummy;

import org.whispersystems.libaxolotl.AxolotlAddress;
import org.whispersystems.libaxolotl.state.SessionRecord;
import org.whispersystems.libaxolotl.state.SessionStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DummySessionStore implements SessionStore {
    private HashMap<AxolotlAddress, SessionRecord> sessions;

    public DummySessionStore(){
        this.sessions = new HashMap<>();
    }

    @Override
    public SessionRecord loadSession(AxolotlAddress axolotlAddress) {
        SessionRecord s1 = this.sessions.get(axolotlAddress);
        if(s1 == null) return new SessionRecord();

        byte b[] = s1.serialize();
        try {
            return new SessionRecord(b);
        } catch(IOException e) {}
        return null;
    }

    @Override
    public List<Integer> getSubDeviceSessions(String s) {
        ArrayList<Integer> ret = new ArrayList<>();
        for(AxolotlAddress a : this.sessions.keySet()) {
            if(s.equals(a.getName()))
                ret.add(a.getDeviceId());
        }
        return ret;
    }

    @Override
    public void storeSession(AxolotlAddress axolotlAddress, SessionRecord sessionRecord) {
        this.sessions.put(axolotlAddress, sessionRecord);
    }

    @Override
    public boolean containsSession(AxolotlAddress axolotlAddress) {
        return this.sessions.containsKey(axolotlAddress);
    }

    @Override
    public void deleteSession(AxolotlAddress axolotlAddress) {
        this.sessions.remove(axolotlAddress);
    }

    @Override
    public void deleteAllSessions(String s) {
        for(AxolotlAddress a : this.sessions.keySet()) {
            if(s.equals(a.getName()))
                this.sessions.remove(s);
        }
    }
}
