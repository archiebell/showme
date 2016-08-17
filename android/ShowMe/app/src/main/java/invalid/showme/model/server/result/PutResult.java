package invalid.showme.model.server.result;

import android.util.Base64;

import java.io.UnsupportedEncodingException;

import invalid.showme.util.JobPriorityUtil;

public class PutResult extends ServerResultEvent
{
    private byte[] response;
    public String MessageIDCalculatedLocally;
    public String MessageIDCalculatedRemotely;
    public long SentPhotoRecordID;

    public PutResult(boolean suc, int returnCode, byte[] response)
    {
        super(JobPriorityUtil.JobType.PutJob, suc, returnCode);
        this.response = response;
    }
    public PutResult(boolean suc, int returnCode, String messageID)
    {
        super(JobPriorityUtil.JobType.PutJob, suc, returnCode);
        this.MessageIDCalculatedRemotely = messageID;
    }

    @Override
    public String getResponseAsString()
    {
        try {
            if(this.response == null) return "";
            return new String(this.response, "UTF-8");
        } catch(UnsupportedEncodingException e) {
            return "ERROR: Could not decode string. Base64: " + Base64.encodeToString(this.response, 0);
        }
    }
}
