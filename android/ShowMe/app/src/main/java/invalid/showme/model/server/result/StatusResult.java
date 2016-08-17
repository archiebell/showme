package invalid.showme.model.server.result;

import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.util.Hashtable;
import java.util.Map;

import invalid.showme.util.JobPriorityUtil;

public class StatusResult extends ServerResultEvent
{
    public Map<String, String> Statuses;
    private byte[] data;

    public StatusResult(boolean suc, int returnCode, Map<String, String> ids)
    {
        super(JobPriorityUtil.JobType.StatusJob, suc, returnCode);
        this.Statuses = ids;
        this.data = null;
    }
    public StatusResult(boolean suc, int returnCode, byte[] d)
    {
        super(JobPriorityUtil.JobType.StatusJob, suc, returnCode);
        this.Statuses = new Hashtable<>();
        this.data = d;
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
