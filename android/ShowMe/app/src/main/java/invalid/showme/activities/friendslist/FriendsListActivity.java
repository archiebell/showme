package invalid.showme.activities.friendslist;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.acra.ACRA;
import org.whispersystems.libaxolotl.InvalidKeyException;
import org.whispersystems.libaxolotl.state.PreKeyBundle;
import org.whispersystems.libaxolotl.state.PreKeyRecord;
import org.whispersystems.libaxolotl.state.SignedPreKeyRecord;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidParameterException;
import java.util.Comparator;
import java.util.List;

import javax.crypto.CipherOutputStream;

import de.greenrobot.event.EventBus;
import invalid.showme.R;
import invalid.showme.activities.SharedActivityLogic;
import invalid.showme.activities.draftslist.ChooseDraft;
import invalid.showme.activities.sentmessages.SentMessages;
import invalid.showme.activities.settings.Settings;
import invalid.showme.activities.setup.Setup;
import invalid.showme.activities.viewfriend.ViewFriend;
import invalid.showme.exceptions.ActivityResultException;
import invalid.showme.exceptions.ExceptionWrapper;
import invalid.showme.exceptions.StrangeUsageException;
import invalid.showme.layoutobjects.DataAdaptorRefreshEvent;
import invalid.showme.model.IFriend;
import invalid.showme.model.UserProfile;
import invalid.showme.model.db.DBHelper;
import invalid.showme.model.photo.DraftPhoto;
import invalid.showme.model.photo.FileCleanupJob;
import invalid.showme.model.photo.ReceivedPhoto;
import invalid.showme.model.server.ServerRequestJob;
import invalid.showme.model.server.result.ServerResultEvent;
import invalid.showme.services.NotificationWrangler;
import invalid.showme.util.BitmapUtils;
import invalid.showme.util.CryptoUtil;
import invalid.showme.util.JobPriorityUtil;
import invalid.showme.util.ProfileURIBuilder;


public class FriendsListActivity extends ActionBarActivity implements SwipeRefreshLayout.OnRefreshListener, ActivityCompat.OnRequestPermissionsResultCallback {
    private final String TAG = "FriendsListActivity";

    private AlertDialog scanFriendDialog;
    private FriendsListArrayAdaptor friendsAdaptor;
    private SwipeRefreshLayout swipeContainer;

    private IFriend selectedFriendForDeletion = null;
    private AlertDialog deleteFriendDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UserProfile up = (UserProfile)getApplication();
        if(!up.Initialized())
        {
            if(!up.Initialize())
            {
                Intent intent = new Intent(this, Setup.class);
                startActivity(intent);
                finish();//So don't leave on activity stack
                return;
            }
        }

        setTitle(R.string.title_activity_friends_list);

        Intent intent = getIntent();
        if(intent.getBooleanExtra("fromNotification", false))
            NotificationWrangler.ResetNotification(getApplicationContext());



        setContentView(R.layout.activity_friends_list);
        final FriendsListActivity friendsListActivity = this;

        List<IFriend> friends = up.getFriends();
        //Sort by most recent message received from contact
        java.util.Collections.sort(friends, new Comparator<IFriend>() {
            @Override
            public int compare(IFriend t1, IFriend t2) {
                if (t1.getPhotos().size() == 0 && t2.getPhotos().size() == 0)
                    return t1.getDisplayName().charAt(0) - t2.getDisplayName().charAt(0);
                else if (t1.getPhotos().size() == 0)
                    return 1;
                else if (t2.getPhotos().size() == 0)
                    return -1;
                else
                    return (int) t1.getPhotos().get(0).Received - (int) t2.getPhotos().get(0).Received;
            }
        });
        friendsAdaptor = new FriendsListArrayAdaptor(this, friends);

        final ListView lv = (ListView) findViewById(R.id.friendslist_list_friends);
        lv.setAdapter(friendsAdaptor);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                IFriend friend = (IFriend) lv.getItemAtPosition(position);

                Intent intent1 = new Intent(friendsListActivity, ViewFriend.class);
                intent1.putExtra("friendToView", friend.getID());

                startActivity(intent1);
            }
        });
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                selectedFriendForDeletion = (IFriend) lv.getItemAtPosition(position);

                AlertDialog.Builder builder = new AlertDialog.Builder(friendsListActivity);

                builder.setTitle(R.string.action_delete_friend);
                builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        UserProfile me = (UserProfile) getApplication();
                        if (!selectedFriendForDeletion.DeleteFromDatabase(DBHelper.getInstance(getApplicationContext()))) {
                            Toast.makeText(getApplicationContext(), "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
                        } else {
                            me.deleteFriend(selectedFriendForDeletion);
                            friendsAdaptor.notifyDataSetChanged();
                        }
                        selectedFriendForDeletion = null;
                        deleteFriendDialog.dismiss();
                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        selectedFriendForDeletion = null;
                        dialogInterface.dismiss();
                    }
                });

                deleteFriendDialog = builder.create();
                deleteFriendDialog.show();

                return true;
            }
        });
        lv.setEmptyView(findViewById(R.id.friendslist_empty));



        ImageButton draftsButtom = (ImageButton)findViewById(R.id.friendslist_senddraft);
        if(up.getDrafts().size() == 0)
            draftsButtom.setVisibility(View.GONE);
        else
            draftsButtom.setVisibility(View.VISIBLE);



        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.friendslist_swipe_container);
        swipeContainer.setOnRefreshListener(this);
        swipeContainer.setColorScheme(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        EventBus.getDefault().register(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        //causes redraw so if had viewed all unseen photos for someone, it's no longer marked as such
        friendsAdaptor.notifyDataSetChanged();
    }



    // Menu Operations

    public void addFriend(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();

        builder.setView(inflater.inflate(R.layout.dialog_addfriend, null));
        builder.setTitle(R.string.action_add_friend);
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        scanFriendDialog = builder.create();
        scanFriendDialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_friends_list, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        UserProfile up = (UserProfile)getApplication();
        switch (item.getItemId()) {
            case R.id.action_add_friend:
                addFriend(null);
                return true;
            //TODO: Show search icon if have lot of friends
            //case R.id.action_search:
            //openSearch();
            //return true;
            case R.id.action_sentmessages:
                Intent intent1 = new Intent(this, SentMessages.class);
                startActivity(intent1);
                return true;
            case R.id.action_settings:
                Intent intent2 = new Intent(this, Settings.class);
                startActivity(intent2);
                return true;

            /* Debug Tools
            case R.id.action_reregister:
                up.getJobManager().addJob(new ServerRequestJob(JobPriorityUtil.JobType.RegisterJob));
                up.getJobManager().addJob(new ServerRequestJob(JobPriorityUtil.JobType.TokenJob));
                return true;
            case R.id.action_testbug:
                ACRA.getErrorReporter().handleException(new Exception("test exception"));
                return true;
            case R.id.action_cleandirectory:
                up.getJobManager().addJob(new FileCleanupJob());
                return true;
            case R.id.action_reprocessthumbs:
                for(IFriend f : up.getFriends())
                    for(ReceivedPhoto p : f.getPhotos())
                        p.FreeMemory();

                for(DraftPhoto d : up.getDrafts())
                    d.FreeMemory();

                Cursor drafts = DBHelper.getDrafts(up);
                while(drafts.moveToNext()) {
                    try {
                        DraftPhoto photo = DraftPhoto.FromDatabase(up, drafts, false);
                        Bitmap thumbnail = BitmapUtils.createThumbnail(photo.getDecryptedPhoto());

                        File thumbnailFile = photo.getThumbnailFile();
                        try {
                            FileOutputStream outputStream = new FileOutputStream(thumbnailFile);
                            CipherOutputStream ciphertextStream = CryptoUtil.wrapinCipherOutputStream(outputStream, photo.key, photo.thumbnailIv);

                            thumbnail.compress(Bitmap.CompressFormat.JPEG, 95, ciphertextStream);
                            ciphertextStream.flush();
                            ciphertextStream.close();
                        } catch (IOException e) {
                            Log.e(TAG, "Cause IOException trying to write encrypted thumbnail file.");
                            ACRA.getErrorReporter().handleException(e);
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Cause IOException trying to read draft photo.");
                        ACRA.getErrorReporter().handleException(e);
                    }
                }
                drafts.close();

                Cursor photos = DBHelper.getAllPhotos(up);
                while(photos.moveToNext()) {
                    try {
                        ReceivedPhoto photo = ReceivedPhoto.FromDatabase(up, photos, false);
                        Bitmap thumbnail = BitmapUtils.createThumbnail(photo.getDecryptedPhoto());

                        File thumbnailFile = photo.getThumbnailFile();
                        try {
                            FileOutputStream outputStream = new FileOutputStream(thumbnailFile);
                            CipherOutputStream ciphertextStream = CryptoUtil.wrapinCipherOutputStream(outputStream, photo.key, photo.thumbnailIv);

                            thumbnail.compress(Bitmap.CompressFormat.JPEG, 95, ciphertextStream);
                            ciphertextStream.flush();
                            ciphertextStream.close();
                        } catch (IOException e) {
                            ACRA.getErrorReporter().handleException(new StrangeUsageException("Cause IOException trying to write encrypted thumbnail file.", e));
                        }
                    } catch(FileNotFoundException e) {
                        ACRA.getErrorReporter().handleException(new StrangeUsageException("Caught FileNotFoundException while reprocessing thumbnails...", e));
                    }
                }
                photos.close();
                return true;
                */
            default:
                return super.onOptionsItemSelected(item);
        }
    }



    // Photo Operations

    private static final int ACTIVITY_REQUEST_IMAGE_CAPTURE = 1;
    @SuppressWarnings("unused")
    public  void takePhoto(View view) {
        SharedActivityLogic.TakePhoto(TAG, this, ACTIVITY_REQUEST_IMAGE_CAPTURE);
    }

    @SuppressWarnings("unused")
    public void sendDraft(View view)
    {
        Intent intent = new Intent(this, ChooseDraft.class);
        startActivity(intent);
    }



    // QR Code Operations

    @SuppressWarnings("unused")
    public void getScanned(View view) {
        scanFriendDialog.dismiss();
        UserProfile up = (UserProfile)getApplication();

        PreKeyRecord rec = up.getNewPreKey();
        SignedPreKeyRecord srec = up.getSignedPrekey();
        PreKeyBundle bundle = new PreKeyBundle(up.getLocalRegistrationId(), up.getLocalDeviceId(), rec.getId(), rec.getKeyPair().getPublicKey(), srec.getId(), srec.getKeyPair().getPublicKey(), srec.getSignature(), up.getIdentityKeyPair().getPublicKey());

        String uri = ProfileURIBuilder.buildURI(up.getDisplayName(), bundle);
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.addExtra("ENCODE_SHOW_CONTENTS", false);
        integrator.shareText(uri);
    }

    @SuppressWarnings("unused")
    public void scanFriend(View view) {
        scanFriendDialog.dismiss();
        new IntentIntegrator(FriendsListActivity.this).initiateScan();
    }



    // Activitiy Result

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
    protected void onActivityResult(int requestCode, int resultCode, Intent resultIntent)
    {
        switch(requestCode) {
            case ACTIVITY_REQUEST_IMAGE_CAPTURE :
                Intent intent = SharedActivityLogic.HandleImageCapture(this, resultCode, resultIntent);
                if(intent != null)
                    startActivity(intent);
                break;
            case 49374://zxing magic constant
                if (resultCode == RESULT_OK) {
                    IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, resultIntent);
                    if (scanResult != null) {
                        String qrContents = scanResult.getContents();
                        UserProfile up = (UserProfile) getApplication();

                        IFriend f;
                        try {
                            f = ProfileURIBuilder.parseURI(qrContents);

                            boolean alreadyKnowthisFriend = false;
                            for(IFriend existingF : up.getFriends()) {
                                if(f.getFingerprint().equals(existingF.getFingerprint())) {
                                    alreadyKnowthisFriend = true;
                                    break;
                                }
                            }

                            if(alreadyKnowthisFriend) {
                                Toast.makeText(getApplicationContext(), "You have already added this friend.", Toast.LENGTH_SHORT).show();
                            } else {
                                if (!f.saveToDatabase(getApplicationContext())) {
                                    Toast.makeText(getApplicationContext(), "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
                                } else {
                                    up.addFriend(f);
                                    View v = findViewById(R.id.friendslist_empty);
                                    v.setVisibility(View.GONE);
                                    friendsAdaptor.notifyDataSetChanged();
                                }
                            }
                        } catch (UnsupportedEncodingException|InvalidKeyException e) {
                            String msg = "In onActivityResult() for 49374, caught " + e.getClass().getName();
                            Log.e(TAG, msg);
                            ACRA.getErrorReporter().handleException(new ExceptionWrapper(msg, e));
                            Toast.makeText(getApplicationContext(), "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
                        } catch (InvalidParameterException e) {
                            Toast.makeText(getApplicationContext(), "Scanning friend failed, please try again.", Toast.LENGTH_SHORT).show();
                        }
                    }
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

    @SuppressWarnings("unused")
    public void onEvent(ServerResultEvent event)
    {
        SharedActivityLogic.HandleServerResponse(TAG, this, event, friendsAdaptor, swipeContainer);
    }
    public void onEvent(DataAdaptorRefreshEvent event)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                friendsAdaptor.notifyDataSetChanged();
            }
        });
    }
}
