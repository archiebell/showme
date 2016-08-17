package invalid.showme.model.server;

import android.util.Log;

import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;
import com.path.android.jobqueue.RetryConstraint;

import org.acra.ACRA;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import invalid.showme.exceptions.ServerException;
import invalid.showme.exceptions.StrangeUsageException;
import invalid.showme.model.UserProfile;
import invalid.showme.model.message.OutgoingMessage;
import invalid.showme.model.photo.SentPhoto;
import invalid.showme.model.server.result.PutResult;
import invalid.showme.model.server.result.ServerResultEvent;
import invalid.showme.services.InstanceIDService;
import invalid.showme.util.BitStuff;
import invalid.showme.util.IOUtil;
import invalid.showme.util.JobPriorityUtil;

public class ServerRequestJob extends Job
{
    private final static String TAG = "ServerRequestJob";

    private JobPriorityUtil.JobType type;
    private List<String> ids;
    private OutgoingMessage msg;
    private int RetryNumber;

    public ServerRequestJob(JobPriorityUtil.JobType type) {
        this(type, "", null);
    }
    public ServerRequestJob(JobPriorityUtil.JobType type, OutgoingMessage m) { this(type, "", m); }
    public ServerRequestJob(JobPriorityUtil.JobType type, String id) {
        this(type, id, null);
    }
    private ServerRequestJob(JobPriorityUtil.JobType type, String id, OutgoingMessage m) {
        super(new Params(JobPriorityUtil.JobTypeToPriority(type)).requireNetwork().persist());
        this.type = type;
        this.ids = new ArrayList<>();
        if(id != null && !id.isEmpty())
            this.ids.add(id);
        this.msg = m;
        this.RetryNumber = 0;
    }

    @Override
    public void onRun() throws Throwable {
        //TODO: Check that have all necessary to run job
        //  not waiting on any other job, like RegisterJob or DraftSaveJob

        ServerResponse response = null;
        ServerResultEvent result = null;

        Exception e = null;
        String errMsg = null;

        UserProfile me = (UserProfile)getApplicationContext();
        if(msg != null) msg.Reconstitute(me);
        ServerInterface.setContext(me);
        switch(this.type)
        {
            case RegisterJob:
                response = ServerInterface.Register();
                result = new ServerResultEvent(JobPriorityUtil.JobType.RegisterJob, response.code == 200 || response.code == 201, response.code);
                break;
            case TokenJob:
                String token = InstanceIDService.getToken(getApplicationContext());
                response = ServerInterface.Token(token);
                result = new ServerResultEvent(JobPriorityUtil.JobType.TokenJob, response.code == 200, response.code);
                break;
            case DeregisterJob:
                response = ServerInterface.Deregister();
                result = new ServerResultEvent(JobPriorityUtil.JobType.DeregisterJob, response.code == 200 || response.code == 204, response.code);
                break;
            case GetJob:
                result = ServerInterface.Get(this.ids.get(0));
                break;
            case PollJob:
                result = ServerInterface.Poll();
                break;
            case DeleteJob:
                response = ServerInterface.Delete(this.ids.get(0));
                result = new ServerResultEvent(JobPriorityUtil.JobType.DeleteJob, response.code == 204, response.code);
                break;
            case StatusJob:
                List<SentPhoto> list = me.getSent();
                List<String> ids = new ArrayList<>();
                for(SentPhoto p : list)
                    if(p.Status != SentPhoto.SentPhotoStatus.Received && p.MessageID != null)
                        ids.add(p.MessageID);
                this.ids = ids;
                //TODO: If have no IDs, extra network request for no reason
                result = ServerInterface.Status(this.ids);
                break;
            case PutJob:
                byte[] data;
                data = msg.EncodeForSending(me);
                result = ServerInterface.Put(data);
                ((PutResult)result).MessageIDCalculatedLocally = BitStuff.toHexString(msg.MessageID);
                ((PutResult)result).SentPhotoRecordID = msg.sentPhotoRecordID;
                break;
            default:
                errMsg = "Received unexpected JobType." + this.type.toString();
                result = null;
        }
        if(result != null) {
            result.RetryNumber = this.RetryNumber;
            EventBus.getDefault().post(result);

            if(!result.Success) {
                errMsg = "Got unexpected error code or other error during " + this.type + ". Response Code: " + result.ReturnCode;
                if(this.type == JobPriorityUtil.JobType.DeleteJob ||
                    this.type == JobPriorityUtil.JobType.StatusJob ||
                    this.type == JobPriorityUtil.JobType.GetJob)
                    errMsg += " IDs: " + IOUtil.join(this.ids);

                if(response != null)
                    errMsg += " Response: " + response.getResponseAsString();
                else if(result != null)
                    errMsg += " Response: " + result.getResponseAsString();

                Log.e(TAG, errMsg);
                e = new ServerException(errMsg);
                ACRA.getErrorReporter().handleException(e);
                throw e;
            }
        }
        else
            ACRA.getErrorReporter().handleException(new StrangeUsageException(errMsg));
    }

    @Override
    public void onAdded() { }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(Throwable throwable, int runCount, int maxRunCount) {
        RetryNumber = runCount;
        //TODO: Raise Recipient not found error to User
        if(runCount > 8 || throwable instanceof FileNotFoundException)
            return RetryConstraint.CANCEL;
        else
            return RetryConstraint.createExponentialBackoff(runCount, 1000);
    }

    @Override
    protected void onCancel() {
        ACRA.getErrorReporter().handleException(new StrangeUsageException("Cancelled server job: " + this.type));
    }
}
