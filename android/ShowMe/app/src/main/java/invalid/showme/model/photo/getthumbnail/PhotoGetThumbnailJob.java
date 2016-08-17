package invalid.showme.model.photo.getthumbnail;

import android.util.Log;

import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;
import com.path.android.jobqueue.RetryConstraint;

import org.acra.ACRA;

import de.greenrobot.event.EventBus;
import invalid.showme.model.photo.DraftPhoto;
import invalid.showme.model.photo.Photo;
import invalid.showme.model.photo.ReceivedPhoto;
import invalid.showme.model.photo.SentPhoto;
import invalid.showme.util.JobPriorityUtil;

public class PhotoGetThumbnailJob extends Job
{
    final static private String TAG = "PhotoGetThumbnailJob";

    private Photo photo;

    public PhotoGetThumbnailJob(Photo draft) {
        super(new Params(JobPriorityUtil.JobTypeToPriority(JobPriorityUtil.JobType.DraftGetThumbnailJob)));
        this.photo = draft;
    }

    @Override
    public void onRun() throws Throwable
    {
        Log.d(TAG, "Starting load of thumbnail for " + photo.getClass().getName() + " id# " + photo.getID());
        photo.LoadThumbnail(getApplicationContext());
        Log.d(TAG, "Completed loading thumbnail for " + photo.getClass().getName() + " id# " + photo.getID());

        if(photo instanceof DraftPhoto)
            EventBus.getDefault().post(new DraftThumbnailLoadEvent(photo.getID()));
        else if(photo instanceof ReceivedPhoto)
            EventBus.getDefault().post(new ReceivedPhotoThumbnailLoadEvent(photo.getID()));
        else if(photo instanceof SentPhoto)
            EventBus.getDefault().post(new SentPhotoThumbnailLoadEvent(photo.getID()));
    }

    @Override
    public void onAdded() { Log.d(TAG, "Added job for " + photo.getClass().getName() + " id# " + photo.getID()); }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(Throwable throwable, int runCount, int maxRunCount) {
        Log.e(TAG, "Caught failure while trying to background-load thumbnail. " + throwable.toString());
        ACRA.getErrorReporter().handleException(throwable);
        return RetryConstraint.CANCEL;
    }

    @Override
    protected void onCancel() { }
}
