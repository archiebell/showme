package invalid.showme.model.photo.savejob;

import android.util.Log;

import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;
import com.path.android.jobqueue.RetryConstraint;

import org.acra.ACRA;

import java.io.IOException;

import invalid.showme.exceptions.StrangeUsageException;
import invalid.showme.model.UserProfile;
import invalid.showme.model.db.DBHelper;
import invalid.showme.model.photo.SentPhoto;
import invalid.showme.util.JobPriorityUtil;

public class SentSaveJob extends Job
{
    final private static String TAG = "SentSaveJob";
    private long id;

    public SentSaveJob(long id) {
        super(new Params(JobPriorityUtil.JobTypeToPriority(JobPriorityUtil.JobType.SentSaveJob)).persist());
        this.id = id;
    }

    @Override
    public void onRun() throws Throwable {
        UserProfile up = (UserProfile)getApplicationContext();
        SentPhoto sent = up.findSent(this.id);
        if(sent == null) {
            ACRA.getErrorReporter().handleException(new StrangeUsageException("Could not find sent id in profile!"));
            throw new RuntimeException("Could not find sent id in profile!");
        }

        try {
            sent.EncryptAndSaveFiles(getApplicationContext());
            if(!sent.saveToDatabase(DBHelper.getInstance(getApplicationContext()))) {
                throw new Exception("Could not save sent photo to database.");
            }
        } catch (IOException e) {
            Log.e(TAG, "Caught IOException in onRun(): " + e.getMessage());
            ACRA.getErrorReporter().handleException(e);
        }
    }

    @Override
    public void onAdded()
    {
        Log.d(TAG, "Added job");
    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(Throwable throwable, int runCount, int maxRunCount) {
        Log.e(TAG, "Caught failure while trying to save sent photo. " + throwable.toString());
        ACRA.getErrorReporter().handleException(throwable);
        if(runCount < 8){
            return RetryConstraint.createExponentialBackoff(runCount, 1000);
        } else {
            return RetryConstraint.CANCEL;
        }
    }

    @Override
    protected void onCancel() { }
}
