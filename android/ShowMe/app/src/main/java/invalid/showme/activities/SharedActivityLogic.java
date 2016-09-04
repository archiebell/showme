package invalid.showme.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import org.acra.ACRA;

import info.guardianproject.netcipher.proxy.OrbotHelper;
import invalid.showme.R;
import invalid.showme.activities.camera.TakePhoto;
import invalid.showme.activities.chooserecipient.ChooseRecipient;
import invalid.showme.exceptions.ExceptionWrapper;
import invalid.showme.exceptions.NoCameraException;
import invalid.showme.exceptions.StrangeUsageException;
import invalid.showme.model.UserProfile;
import invalid.showme.model.server.ServerConfiguration;
import invalid.showme.model.server.result.PollResult;
import invalid.showme.model.server.result.ServerResultEvent;
import invalid.showme.services.PhotoReceiver;
import invalid.showme.util.IOUtil;
import invalid.showme.util.PhotoFileManager;

public class SharedActivityLogic
{
    public static final int PERMISSIONS_REQUEST_CAMERA = 1;

    public static void ConfirmInitialization(Context context, boolean isThisUnusual)
    {
        final UserProfile up = (UserProfile)context;
        if(!up.Initialized())
        {
            if(isThisUnusual)
                ACRA.getErrorReporter().handleException(new StrangeUsageException("hadn't initalized profile yet..."));
            if(!up.Initialize())
                ACRA.getErrorReporter().handleException(new StrangeUsageException("Profile initialization failed!"));
        }
    }



    //https://github.com/guardianproject/NetCipher/blob/a80e21f8fe334516d399c32f081197e73bc7f051/sample-chboye/src/sample/netcipher/NetCipherSampleActivity.java#L334
    public static void PromptForOrbot(final Context context) {
        String message = "You must have Orbot installed to use Tor. ";
        if(ServerConfiguration.RequiresTor())
            message += "Tor is required to use the server that is configured in this version of ShowMe. ";

        final Intent intent = OrbotHelper.getOrbotInstallIntent(context);
        if (intent.getPackage() == null) {
            message += "Would you like to download it from f-droid.org?";
        } else if(intent.getPackage().equals("com.android.vending")) {
            message += "Would you like to install it using Google Play?";
        } else {
            message += "Would you like to install it using F-Droid?";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Install Orbot?");
        builder.setMessage(message);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                context.startActivity(intent);
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        builder.show();
    }

    public static void TakePhoto(String TAG, Activity activity, int ACTIVITY_REQUEST_IMAGE_CAPTURE)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.CAMERA},
                        SharedActivityLogic.PERMISSIONS_REQUEST_CAMERA);
                return;
            }
        }

        Intent takePictureIntent;
        Boolean useBuiltinCamera = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext()).getBoolean("pref_usebuiltincamera", true);
        if(useBuiltinCamera) {
            takePictureIntent = new Intent(activity, TakePhoto.class);
            activity.startActivityForResult(takePictureIntent, ACTIVITY_REQUEST_IMAGE_CAPTURE);
        } else {
            takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
                try {
                    Uri uri = Uri.parse("content://invalid.showme.photowriter/brandnew");
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                    takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    activity.startActivityForResult(takePictureIntent, ACTIVITY_REQUEST_IMAGE_CAPTURE);
                } catch (Exception e) {
                    String msg = "Caught Exception when trying to set location for photo capture.";
                    Log.e(TAG, msg);
                    ACRA.getErrorReporter().handleException(new ExceptionWrapper(msg, e));
                    Toast.makeText(activity.getApplicationContext(), R.string.takephoto_error_nodir, Toast.LENGTH_LONG).show();
                }
            } else {
                Log.e(TAG, "No camera found?");
                ACRA.getErrorReporter().handleException(new NoCameraException());
                Toast.makeText(activity.getApplicationContext(), R.string.takephoto_error_nocamera, Toast.LENGTH_LONG).show();
            }
        }
    }
    public static Intent HandleImageCapture(Activity activity, int resultCode, Intent resultIntent) {
        if (resultCode == Activity.RESULT_OK) {
            Intent intent = new Intent(activity, ChooseRecipient.class);
            Boolean useBuiltinCamera = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext()).getBoolean("pref_usebuiltincamera", true);
            if (!useBuiltinCamera) {
                if (PhotoReceiver.lastPhoto != null && PhotoReceiver.lastPhoto.exists()) {
                    intent.putExtra("photoPath", PhotoReceiver.lastPhoto.getAbsolutePath());
                    intent.putExtra("thumbnailPath", PhotoFileManager.createThumbnailFromFullSizedFileToFile(PhotoReceiver.lastPhoto).getAbsolutePath());
                    PhotoReceiver.lastPhoto = null;
                    return intent;
                } else {
                    ACRA.getErrorReporter().handleException(new StrangeUsageException("Somehow didn't use built-in camera but still failed to get lastPhoto"));
                }
            } else {
                if (resultIntent.getStringExtra("photoPath") != null && resultIntent.getStringExtra("photoPath").length() != 0 &&
                        resultIntent.getStringExtra("thumbnailPath") != null && resultIntent.getStringExtra("thumbnailPath").length() != 0) {
                    intent.putExtra("photoPath", resultIntent.getStringExtra("photoPath"));
                    intent.putExtra("thumbnailPath", resultIntent.getStringExtra("thumbnailPath"));
                    return intent;
                } else {
                    ACRA.getErrorReporter().putCustomData("intentData", IOUtil.intentToString(resultIntent));
                    ACRA.getErrorReporter().handleException(new StrangeUsageException("Somehow used built-in camera but didn't get files!"));
                    ACRA.getErrorReporter().removeCustomData("intentData");
                }
            }
        }
        return null;
    }



    public static void HandleServerResponse(final String TAG, final Activity activity, final ServerResultEvent event, final ArrayAdapter<?> dataAdaptor, final SwipeRefreshLayout swipeContainer)
    {
        String toastMsg = "";
        Boolean shouldStopRefreshing = false;

        switch(event.Type)
        {
            case RegisterJob:
                shouldStopRefreshing = true;
                if(event.Success)
                    toastMsg = "Server Registration Succeeded.";
                else if(event.RetryNumber == 0)
                    toastMsg = "Server Registration Failed, will try again later.";
                break;
            case PollJob:
                PollResult pollResult = (PollResult)event;
                shouldStopRefreshing = true;
                if(!event.Success && event.RetryNumber == 0)
                    toastMsg = "Error checking messages, will retry...";
                else if(event.Success && pollResult.IDs.size() == 0)
                    toastMsg = "No new messages.";
                else if(event.Success)
                    toastMsg = "Downloading " + pollResult.IDs.size() + " messages...";
                break;
            case StatusJob:
                shouldStopRefreshing = true;
                if(!event.Success && event.RetryNumber == 0)
                    toastMsg = "Error checking status, will retry...";
                break;
            case GetJob:
                shouldStopRefreshing = true;
                if(!event.Success && event.RetryNumber == 0)
                    toastMsg = "Error downloading message, will retry...";
        }
        final String toastMsgF = toastMsg;
        final Boolean shouldStopRefreshingF = shouldStopRefreshing;
        if(toastMsg.length() != 0 || shouldStopRefreshingF)
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dataAdaptor.notifyDataSetChanged();
                    if (shouldStopRefreshingF)
                        swipeContainer.setRefreshing(false);
                    if (toastMsgF.length() > 0)
                        Toast.makeText(activity.getApplicationContext(), toastMsgF, Toast.LENGTH_LONG).show();
                }
            });
    }

}
