package invalid.showme.activities.viewfriend;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.Toast;

import org.acra.ACRA;

import java.util.List;

import de.greenrobot.event.EventBus;
import invalid.showme.R;
import invalid.showme.activities.SharedActivityLogic;
import invalid.showme.activities.draftslist.ChooseDraft;
import invalid.showme.activities.settings.Settings;
import invalid.showme.activities.viewphoto.ViewPhotoNormal;
import invalid.showme.activities.viewphoto.ViewPhotoTransient;
import invalid.showme.exceptions.ActivityResultException;
import invalid.showme.exceptions.IntentDataException;
import invalid.showme.layoutobjects.DataAdaptorRefreshEvent;
import invalid.showme.model.IFriend;
import invalid.showme.model.UserProfile;
import invalid.showme.model.db.DBHelper;
import invalid.showme.model.photo.ReceivedPhoto;
import invalid.showme.model.photo.getthumbnail.ReceivedPhotoThumbnailLoadEvent;
import invalid.showme.model.server.ServerRequestJob;
import invalid.showme.model.server.result.ServerResultEvent;
import invalid.showme.services.NotificationWrangler;
import invalid.showme.util.IOUtil;
import invalid.showme.util.JobPriorityUtil;

public class ViewFriend extends ActionBarActivity implements SwipeRefreshLayout.OnRefreshListener, SharedPreferences.OnSharedPreferenceChangeListener, ActivityCompat.OnRequestPermissionsResultCallback {
    private final static String TAG = "ViewFriend";

    private IFriend thisFriend;

    private FriendsPhotoAdaptor photosAdaptor;
    private GridView gv;
    private SwipeRefreshLayout swipeContainer;

    private AlertDialog deletePhotoDialog;
    private ReceivedPhoto selectedPhotoForDeletion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedActivityLogic.ConfirmInitialization(getApplicationContext(), false);
        setContentView(R.layout.activity_view_friend);

        UserProfile up = (UserProfile)getApplication();
        EventBus.getDefault().register(this);

        Intent intent = getIntent();
        if(intent.getBooleanExtra("fromNotification", false))
            NotificationWrangler.ResetNotification(getApplicationContext());

        long friendId = intent.getLongExtra("friendToView", -1);
        this.thisFriend = up.findFriend(friendId);
        if(this.thisFriend == null) {
            String msg = "Could not find friend with ID " + friendId;
            Log.e(TAG, msg);
            ACRA.getErrorReporter().putCustomData("intentData", IOUtil.intentToString(getIntent()));
            ACRA.getErrorReporter().handleException(new IntentDataException(msg));
            ACRA.getErrorReporter().removeCustomData("intentData");
            Toast.makeText(getApplicationContext(), "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
        }
        final ViewFriend viewFriendActivity = this;



        //TODO: What happens if someone's name superlong?
        setTitle(this.thisFriend.getDisplayName());



        List<ReceivedPhoto> photos = this.thisFriend.getPhotos();
        for(ReceivedPhoto p : photos)
        {
            if(p.ShouldBeDeleted()) {
                this.thisFriend.deletePhoto(p.MessageID);
                photos.remove(p);
                p.DeleteFiles();
                if(!ReceivedPhoto.DeleteFromDatabase(DBHelper.getInstance(this), p.getID())) {
                    Toast.makeText(getApplicationContext(), "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
                }
            }
        }

        gv = (GridView) findViewById(R.id.viewfriend_list_photos);
        photosAdaptor = new FriendsPhotoAdaptor(this, gv, up, photos);
        gv.setAdapter(photosAdaptor);
        gv.setEmptyView(findViewById(R.id.viewfriend_empty));
        gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                view.setBackgroundResource(R.drawable.noglow);

                ReceivedPhoto photo = (ReceivedPhoto) gv.getItemAtPosition(position);
                NotificationWrangler.ViewedPhoto(photo.FriendID, getApplicationContext());

                Intent intent1;
                if (photo.PrivatePhoto) {
                    intent1 = new Intent(viewFriendActivity, ViewPhotoTransient.class);
                } else {
                    intent1 = new Intent(viewFriendActivity, ViewPhotoNormal.class);
                }
                intent1.putExtra("photoToView", photo.getID());

                startActivity(intent1);
            }
        });
        gv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                selectedPhotoForDeletion = (ReceivedPhoto) gv.getItemAtPosition(position);

                AlertDialog.Builder builder = new AlertDialog.Builder(viewFriendActivity);

                builder.setTitle(R.string.action_delete_photo);
                builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (!ReceivedPhoto.DeleteFromDatabase(DBHelper.getInstance(getApplicationContext()), selectedPhotoForDeletion.getID())) {
                            Toast.makeText(getApplicationContext(), "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
                        } else {
                            thisFriend.deletePhoto(selectedPhotoForDeletion.MessageID);
                            photosAdaptor.notifyDataSetChanged();
                        }
                        selectedPhotoForDeletion = null;
                        deletePhotoDialog.dismiss();
                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        selectedPhotoForDeletion = null;
                        dialog.dismiss();
                    }
                });

                deletePhotoDialog = builder.create();
                deletePhotoDialog.show();

                return true;
            }
        });



        ImageButton draftsButtom = (ImageButton)findViewById(R.id.viewfriend_senddraft);
        if(up.getDrafts().size() == 0)
            draftsButtom.setVisibility(View.GONE);
        else
            draftsButtom.setVisibility(View.VISIBLE);



        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.viewfriend_swipe_container);
        swipeContainer.setOnRefreshListener(this);
        swipeContainer.setColorScheme(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
    }

    public void onEvent(ReceivedPhotoThumbnailLoadEvent event) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                gv.invalidateViews();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        //causes redraw so if you had changed preferences it will update
        photosAdaptor.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_view_friend, menu);
        return true;
    }



    // Photo Operations

    private static final int ACTIVITY_REQUEST_IMAGE_CAPTURE = 1;
    private String photoTaken;
    public void sendDraft(View view)
    {
        ViewFriend viewFriendActivity = this;
        Intent intent = new Intent(viewFriendActivity, ChooseDraft.class);
        intent.putExtra("friendToSendTo", this.thisFriend.getID());

        startActivity(intent);
    }

    public void takePhoto(View view)
    {
        SharedActivityLogic.TakePhoto(TAG, this, ACTIVITY_REQUEST_IMAGE_CAPTURE);
    }



    // Activity Result

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case SharedActivityLogic.PERMISSIONS_REQUEST_CAMERA: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    SharedActivityLogic.TakePhoto(TAG, this, ACTIVITY_REQUEST_IMAGE_CAPTURE);
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, Settings.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultIntent)
    {
        switch (requestCode) {
            case ACTIVITY_REQUEST_IMAGE_CAPTURE:
                Intent intent = SharedActivityLogic.HandleImageCapture(this, resultCode, resultIntent);
                if(intent != null) {
                    intent.putExtra("limitRecipients", thisFriend.getID());
                    startActivity(intent);
                }
                break;
            default:
                String msg = "Got requestCode wasn't expecting:" + requestCode;
                Log.e(TAG, msg);
                ACRA.getErrorReporter().handleException(new ActivityResultException(msg));
                Toast.makeText(getApplicationContext(), "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
        }
    }



    // Gestures

    @Override
    public void onRefresh() {
        swipeContainer.setRefreshing(true);
        UserProfile up = (UserProfile)getApplication();
        up.getJobManager().addJob(new ServerRequestJob(JobPriorityUtil.JobType.PollJob));
    }

    public void onEvent(ServerResultEvent event)
    {
        SharedActivityLogic.HandleServerResponse(TAG, this, event, photosAdaptor, swipeContainer);
    }
    public void onEvent(DataAdaptorRefreshEvent event)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                photosAdaptor.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        photosAdaptor.notifyDataSetChanged();
    }
}
