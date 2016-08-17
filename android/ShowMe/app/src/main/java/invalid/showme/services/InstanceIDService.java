package invalid.showme.services;


import android.content.Context;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.google.android.gms.iid.InstanceIDListenerService;

import org.acra.ACRA;

import java.io.IOException;

import invalid.showme.model.UserProfile;
import invalid.showme.model.server.ServerConfiguration;
import invalid.showme.model.server.ServerRequestJob;
import invalid.showme.util.JobPriorityUtil;

public class InstanceIDService  extends InstanceIDListenerService {
    private static final String TAG = "InstanceIDService";

    public void onTokenRefresh() {
        Log.d(TAG, "Got TokenRefresh hit");
        UserProfile profile = (UserProfile)getApplication();

        profile.getJobManager().addJob(new ServerRequestJob(JobPriorityUtil.JobType.TokenJob));
    }

    public static String getToken(Context context) {
        String token = null;
        try {
            InstanceID instanceID = InstanceID.getInstance(context);
            String authorizedEntity = ServerConfiguration.GOOGLE_PROJECT_ID;
            token = instanceID.getToken(authorizedEntity, GoogleCloudMessaging.INSTANCE_ID_SCOPE);
        } catch(IOException e) {
            Log.e(TAG, "Caught IOException while trying to retrieve token.");
            ACRA.getErrorReporter().handleException(e);
        }
        return token;
    }
}
