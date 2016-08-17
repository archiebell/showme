package invalid.showme.services;


import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;
import android.util.Log;

import org.acra.ACRA;

import java.util.HashMap;

import invalid.showme.R;
import invalid.showme.activities.friendslist.FriendsListActivity;
import invalid.showme.activities.viewfriend.ViewFriend;
import invalid.showme.exceptions.StrangeUsageException;

public class NotificationWrangler
{
    private static final String TAG = "NotificationManager";
    private static HashMap<Long, String> receivedMessageSenders = new HashMap<>();
    private static int receivedMessages = 0;
    private static Boolean hasPrivatePhotos = false;

    public static void AddSender(Long id, String name, Boolean privatePhoto)
    {
        //TODO: Confirm doesn't crash on two inserts
        NotificationWrangler.receivedMessageSenders.put(id, name);
        NotificationWrangler.receivedMessages++;
        NotificationWrangler.hasPrivatePhotos |= privatePhoto;
    }

    private static final int notificationID = 1337;
    private static final Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    private static NotificationCompat.Builder notificationBuilder = null;
    private static NotificationManager notificationManager = null;

    private static void InitializeNotificationBuilder(Context context)
    {
        if(NotificationWrangler.notificationBuilder == null) {
            NotificationWrangler.notificationBuilder = new NotificationCompat.Builder(context)
                    .setAutoCancel(true)
                    .setSound(NotificationWrangler.defaultSoundUri);
        }
        if(NotificationWrangler.notificationManager == null) {
            NotificationWrangler.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        }
    }

    public static void ViewedPhoto(long id, Context context)
    {
        InitializeNotificationBuilder(context);
        if(NotificationWrangler.receivedMessageSenders.containsKey(id)) {
            NotificationWrangler.receivedMessageSenders.remove(id);
            NotificationWrangler.receivedMessages--;
        }

        if(NotificationWrangler.receivedMessageSenders.size() == 0) {
            NotificationWrangler.receivedMessages = 0;
            NotificationWrangler.hasPrivatePhotos = false;
            NotificationWrangler.notificationManager.cancel(notificationID);
        } else {
            NotificationWrangler.ShowOrUpdateNotification(context);
        }
    }

    public static void ResetNotification(Context context)
    {
        InitializeNotificationBuilder(context);
        NotificationWrangler.receivedMessages = 0;
        NotificationWrangler.receivedMessageSenders.clear();
        NotificationWrangler.hasPrivatePhotos = false;
        NotificationWrangler.notificationManager.cancel(notificationID);
    }

    public static void ShowOrUpdateNotification(Context context)
    {
        InitializeNotificationBuilder(context);

        if(NotificationWrangler.receivedMessageSenders.size() == 0) {
            String msg = "Strange, called ShowOrUpdateNotification without any received message senders! ";
            Log.w(TAG, msg);
            ACRA.getErrorReporter().handleException(new StrangeUsageException(msg));
            return;
        }

        Intent intent;
        PendingIntent pendingIntent;
        if(NotificationWrangler.receivedMessageSenders.size() > 1) {
            intent = new Intent(context, FriendsListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            pendingIntent = PendingIntent.getActivity(context, 0, intent,
                    PendingIntent.FLAG_ONE_SHOT);
        }
        else {
            //TODO: Why doesn't work?
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

            stackBuilder.addParentStack(ViewFriend.class);

            intent = new Intent(context, ViewFriend.class);
            intent.putExtra("friendToView", (Long) NotificationWrangler.receivedMessageSenders.keySet().toArray()[0]);
            intent.putExtra("fromNotification", true);
            stackBuilder.addNextIntent(intent);

            pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        notificationBuilder
                .setContentTitle("New Message" + (NotificationWrangler.receivedMessages > 1 ? "s" : ""))
                .setContentText(TextUtils.join(", ", NotificationWrangler.receivedMessageSenders.values()))
                .setNumber(NotificationWrangler.receivedMessages)
                .setContentIntent(pendingIntent)
                .setSmallIcon(NotificationWrangler.hasPrivatePhotos ? R.drawable.sunglasses_outline : R.drawable.ic_camera_alt_white_48dp);

        notificationManager.notify(notificationID, notificationBuilder.build());
    }
}
