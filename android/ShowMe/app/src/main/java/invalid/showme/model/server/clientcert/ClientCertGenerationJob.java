package invalid.showme.model.server.clientcert;

import android.util.Log;

import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;
import com.path.android.jobqueue.RetryConstraint;

import org.acra.ACRA;

import invalid.showme.model.UserProfile;
import invalid.showme.util.JobPriorityUtil;

class ClientCertGenerationJob extends Job
{
    final static private String TAG = "ClientCertGenerationJob";

    private ClientCertificateStore certStore;

    public ClientCertGenerationJob(ClientCertificateStore certStore) {
        super(new Params(JobPriorityUtil.JobTypeToPriority(JobPriorityUtil.JobType.ClientCertGeneration)));
        this.certStore = certStore;
    }

    @Override
    public void onRun() throws Throwable
    {
        for(int i=0; i<10; i++) {
            certStore.generateNow();
        }
        certStore.saveToDatabase((UserProfile)getApplicationContext());
        certStore.jobDone();
    }

    @Override
    public void onAdded() { Log.d(TAG, "Added client certificate generation job"); }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(Throwable throwable, int runCount, int maxRunCount) {
        Log.e(TAG, "Caught failure while trying to generate client certificates. " + throwable.toString());
        ACRA.getErrorReporter().handleException(throwable);
        if(runCount < 3){
            return RetryConstraint.createExponentialBackoff(runCount, 1000);
        } else {
            return RetryConstraint.CANCEL;
        }
    }

    @Override
    protected void onCancel() { }
}
