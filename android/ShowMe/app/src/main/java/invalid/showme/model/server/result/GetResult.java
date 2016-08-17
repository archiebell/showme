package invalid.showme.model.server.result;

import android.util.Base64;

import java.io.UnsupportedEncodingException;

import invalid.showme.exceptions.InvalidMessageException;
import invalid.showme.exceptions.NewMessageVersionException;
import invalid.showme.exceptions.OldMessageVersionException;
import invalid.showme.model.IMe;
import invalid.showme.model.message.IncomingMessage;
import invalid.showme.util.JobPriorityUtil;

public class GetResult extends ServerResultEvent
{
    public String ID;
    private byte[] data;

    public GetResult(boolean suc, int returnCode, String ID, byte[] d)
    {
        super(JobPriorityUtil.JobType.GetJob, suc, returnCode);
        this.ID = ID;
        this.data= d;
    }

    public IncomingMessage toMessage(IMe me) throws NewMessageVersionException, InvalidMessageException, OldMessageVersionException {
        return IncomingMessage.Decode(me, this.ID, this.data);
    }

    @Override
    public String getResponseAsString()
    {
        try {
            return new String(this.data, "UTF-8");
        } catch(UnsupportedEncodingException e) {
            return "ERROR: Could not decode string. Base64: " + Base64.encodeToString(this.data, 0);
        }
    }
}
