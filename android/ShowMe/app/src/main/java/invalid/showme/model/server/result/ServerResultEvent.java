package invalid.showme.model.server.result;

import invalid.showme.util.JobPriorityUtil;

public class ServerResultEvent
{
    public int ReturnCode;
    public JobPriorityUtil.JobType Type;
    public boolean Success;
    public int RetryNumber;


    public ServerResultEvent(JobPriorityUtil.JobType type, boolean success, int returnCode)
    {
        this.Type = type;
        this.Success = success;
        this.ReturnCode = returnCode;
        this.RetryNumber = 0;
    }

    public String getResponseAsString() {
        return "<not implemented for ServerResultEvent>";
    }
}
