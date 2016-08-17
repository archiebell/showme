package invalid.showme.services;

import android.os.Bundle;

import com.google.android.gms.gcm.GcmListenerService;

import invalid.showme.model.UserProfile;
import invalid.showme.model.server.ServerRequestJob;
import invalid.showme.util.JobPriorityUtil;

public class CloudReceiverService extends GcmListenerService
{
    private static final String TAG = "CloudReceiverService";

    @Override
    public void onMessageReceived(String from, Bundle data)
    {
        UserProfile up = (UserProfile)getApplication();
        up.getJobManager().addJob(new ServerRequestJob(JobPriorityUtil.JobType.PollJob));
    }
}
