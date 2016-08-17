package invalid.showme.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


import invalid.showme.model.UserProfile;

public class NetworkChangeDetector extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        UserProfile up = (UserProfile)context.getApplicationContext();
        up.maybeSetNewClientCertificate();
    }
}
