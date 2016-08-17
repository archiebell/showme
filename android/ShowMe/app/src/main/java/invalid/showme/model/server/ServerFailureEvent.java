package invalid.showme.model.server;

import invalid.showme.util.JobPriorityUtil;

public class ServerFailureEvent
{
    public JobPriorityUtil.JobType Type;
    public String Message;


    public ServerFailureEvent(JobPriorityUtil.JobType type, String msg)
    {
        this.Type = type;
        this.Message = msg;
    }
}
