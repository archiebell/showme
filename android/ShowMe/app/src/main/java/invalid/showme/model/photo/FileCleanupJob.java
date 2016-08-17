package invalid.showme.model.photo;

import android.util.Log;

import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;
import com.path.android.jobqueue.RetryConstraint;

import org.acra.ACRA;

import java.io.File;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Objects;

import invalid.showme.model.IFriend;
import invalid.showme.model.UserProfile;
import invalid.showme.model.db.JobDBPeeker;
import invalid.showme.util.JobPriorityUtil;
import invalid.showme.util.TimeUtil;

public class FileCleanupJob extends Job
{
    final static private String TAG = "FileCleanupJob";

    public FileCleanupJob() {
        super(new Params(JobPriorityUtil.JobTypeToPriority(JobPriorityUtil.JobType.FileCleanupJob)));
    }

    private class DelayMePleaseException extends Exception {}

    @Override
    public void onRun() throws Throwable
    {
        UserProfile me = (UserProfile)getApplicationContext();

        if(JobDBPeeker.queuedJobs(me)) {
            throw new DelayMePleaseException();
        }

        File storageDir = me.getFilesDir();

        Boolean hasSharingDir = false;
        Dictionary<String, Boolean> files = new Hashtable<>();
        Log.d(TAG, "Iterating through files directory.");
        for(File f : storageDir.listFiles()) {
            if(f.isDirectory() && !Objects.equals(f.getName(), "forsharing"))
                Log.d(TAG, "Found directory didn't expect: " + f.getName());
            else if(f.isDirectory() && Objects.equals(f.getName(), "forsharing"))
                hasSharingDir = true;
            else if(Objects.equals(f.getName(), "ACRA-INSTALLATION"))
                continue;
            else
                files.put(f.getName(), false);
        }

        for(DraftPhoto d : me.getDrafts()) {
            if(files.get(d.photoFile.getName()) == null)
                Log.d(TAG, "Draft id " + d.getID() + " missing photo file.");
            else
                files.put(d.photoFile.getName(), true);
            if(files.get(d.thumbnailFile.getName()) == null)
                Log.d(TAG, "Draft id " + d.getID() + " missing thumbnail file.");
            else
                files.put(d.thumbnailFile.getName(), true);
        }

        for(SentPhoto d : me.getSent()) {
            if(d.photoFile != null && files.get(d.photoFile.getName()) == null)
                Log.d(TAG, "Sent id " + d.getID() + " missing photo file.");
            else if(d.photoFile != null)
                files.put(d.photoFile.getName(), true);
            if(d.thumbnailFile != null && files.get(d.thumbnailFile.getName()) == null)
                Log.d(TAG, "Sent id " + d.getID() + " missing thumbnail file.");
            else if (d.thumbnailFile != null)
                files.put(d.thumbnailFile.getName(), true);

            if(d.plaintextPhotoFilename != null && !d.plaintextPhotoFilename.isEmpty() &&
                    files.get(d.plaintextPhotoFilename) == null)
                Log.d(TAG, "Sent id " + d.getID() + " missing plaintext photo file.");
            else if(d.plaintextPhotoFilename != null && !d.plaintextPhotoFilename.isEmpty())
                files.put(d.plaintextPhotoFilename, true);
            if(d.plaintextThumbnailFilename != null && !d.plaintextThumbnailFilename.isEmpty() &&
                    files.get(d.plaintextThumbnailFilename) == null)
                Log.d(TAG, "Sent id " + d.getID() + " missing plaintext thumbnail file.");
            else if(d.plaintextThumbnailFilename != null && !d.plaintextThumbnailFilename.isEmpty())
                files.put(d.plaintextThumbnailFilename, true);
        }

        for(IFriend f : me.getFriends()) {
            for (ReceivedPhoto p : f.getPhotos()) {
                if (files.get(p.photoFile.getName()) == null)
                    Log.d(TAG, "Photo id " + p.getID() + " missing photo file.");
                else
                    files.put(p.photoFile.getName(), true);
                if (files.get(p.thumbnailFile.getName()) == null)
                    Log.d(TAG, "Photo id " + p.getID() + " missing thumbnail file.");
                else
                    files.put(p.thumbnailFile.getName(), true);
            }
        }

        if(!JobDBPeeker.queuedJobs(me)) {
            for (Enumeration<String> e = files.keys(); e.hasMoreElements(); ) {
                String f = e.nextElement();
                if (!files.get(f)) {
                    Log.d(TAG, "Removing orphaned file: " + f);
                    File fi = new File(me.getFilesDir(), f);
                    if (!fi.delete())
                        Log.d(TAG, "   could not remove " + f);
                }
            }
        }

        if(hasSharingDir) {
            Log.d(TAG, "Iterating through sharing directory.");
            File shareDir = new File(me.getFilesDir() + "/forsharing/");
            for(File f : shareDir.listFiles()) {
                if(f.isDirectory())
                    Log.d(TAG, "Found directory didn't expect in sharing directory: " + f.getName());
                else {
                    String strStamp = f.getName().substring(4, f.getName().indexOf('_', 5));
                    long timestamp = Long.parseLong(strStamp);
                    if(!TimeUtil.MoreThanOneHourAgo(timestamp)) {
                        Log.d(TAG, "Removing temporary shared photo: " + f.getName());
                        f.delete();
                    } else {
                        Log.d(TAG, "Leaving temporary shared photo: " + f.getName());
                    }
                }
            }
        }
    }

    @Override
    public void onAdded() { Log.d(TAG, "Added file cleanup job"); }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(Throwable throwable, int runCount, int maxRunCount) {
        if(!(throwable instanceof DelayMePleaseException)) {
            Log.e(TAG, "Caught failure while trying to clean up files. " + throwable.toString());
            ACRA.getErrorReporter().handleException(throwable);
            return RetryConstraint.CANCEL;
        } else if(runCount < 4){
            return RetryConstraint.createExponentialBackoff(runCount, 5000);
        } else {
            return RetryConstraint.CANCEL;
        }
    }

    @Override
    protected void onCancel() { }
}

