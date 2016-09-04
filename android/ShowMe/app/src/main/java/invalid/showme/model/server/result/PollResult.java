package invalid.showme.model.server.result;

import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import invalid.showme.util.JobPriorityUtil;

public class PollResult extends ServerResultEvent
{
    public List<String> IDs;
    private byte[] data;

    public PollResult(boolean suc, int returnCode, List<String> ids)
    {
        super(JobPriorityUtil.JobType.PollJob, suc, returnCode);
        this.IDs = ids;
        this.data = null;
    }
    public PollResult(boolean suc, int returnCode, byte[] d)
    {
        super(JobPriorityUtil.JobType.PollJob, suc, returnCode);
        this.IDs = new ArrayList<>();
        this.data = d;
    }

    @Override
    public String getResponseAsString()
    {
        try {
            if(this.data == null)
                return "";
            return new String(this.data, "UTF-8");
        } catch(UnsupportedEncodingException e) {
            return "ERROR: Could not decode string. Base64: " + Base64.encodeToString(this.data, 0);
        }
    }
}
