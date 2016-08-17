package invalid.showme.util;


import android.util.Log;

import org.acra.ACRA;

public class JobPriorityUtil
{
    private final static String TAG = "JobPriorityUtil";

    public enum JobType
    {
        RegisterJob,
        TokenJob,
        DeregisterJob,
        PollJob,
        GetJob,
        DeleteJob,
        PutJob,
        StatusJob,

        ClientCertGeneration,

        DraftGetThumbnailJob,
        DraftSaveJob,

        SentSaveJob,

        FileCleanupJob
    }

    public static int JobTypeToPriority(JobType jt) {
        switch(jt)
        {
            case RegisterJob:
                return 100;
            case TokenJob:
            case DraftGetThumbnailJob:
                return 99;
            case ClientCertGeneration:
                return 76;
            case DraftSaveJob:
            case SentSaveJob:
            case PutJob:
            case GetJob:
                return 75;
            case PollJob:
            case StatusJob:
                return 25;
            case DeleteJob:
                return 10;
            case FileCleanupJob:
            case DeregisterJob:
                return 1;
            default:
                String msg = "In JobTypeToPriority, given JobType didn't know: " + jt.toString();
                Log.e(TAG, msg);
                ACRA.getErrorReporter().handleException(new RuntimeException());
                break;
        }
        return 0;
    }
}
