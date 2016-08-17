package invalid.showme.model.photo.savejob;

import android.util.Log;

import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;
import com.path.android.jobqueue.RetryConstraint;

import org.acra.ACRA;

import java.io.IOException;

import de.greenrobot.event.EventBus;
import invalid.showme.activities.draftslist.DraftSaveJobEvent;
import invalid.showme.exceptions.StrangeUsageException;
import invalid.showme.model.UserProfile;
import invalid.showme.model.db.DBHelper;
import invalid.showme.model.photo.DraftPhoto;
import invalid.showme.util.JobPriorityUtil;

public class DraftSaveJob extends Job
{
    final private static String TAG = "DraftSaveJob";
    private DraftPhoto serializedDraft;
    private long id;

    public DraftSaveJob(long id, DraftPhoto draft) {
        super(new Params(JobPriorityUtil.JobTypeToPriority(JobPriorityUtil.JobType.DraftSaveJob)).persist());
        this.id = id;
        this.serializedDraft = draft;
    }

    @Override
    public void onRun() throws Throwable {
        UserProfile up = (UserProfile)getApplicationContext();
        DraftPhoto draft = up.findDraft(this.id);
        Boolean shouldReloadDrafts = false;
        if(draft == null) {
            //Prefer to get id from profile, so when assign it id, are editing actual object reference
            //When use serialized version, discovered id not updated in origin object
            //So if do use id, have to tell profile to reload drafts from database
            //TODO WHY?! PhotoGetThumbnailJob loads thumbnail on real object. Complex vs Primitive type issue?
            ACRA.getErrorReporter().handleException(new StrangeUsageException("Could not find draft id in profile, resorting to serialized version."));
            draft = serializedDraft;
            shouldReloadDrafts = true;
        }

        try {
            draft.EncryptAndSaveFiles(getApplicationContext());
            if(!draft.saveToDatabase(DBHelper.getInstance(getApplicationContext()))) {
                throw new Exception("Could not save draft to database.");
            }
        } catch (IOException e) {
            Log.e(TAG, "Caught IOException in onRun(): " + e.getMessage());
            ACRA.getErrorReporter().handleException(e);
        }
        EventBus.getDefault().post(new DraftSaveJobEvent());
        if(shouldReloadDrafts)
            up.initializeDrafts();
    }

    @Override
    public void onAdded()
    {
        Log.d(TAG, "Added job");
    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(Throwable throwable, int runCount, int maxRunCount) {
        Log.e(TAG, "Caught failure while trying to save draft. " + throwable.toString());
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
