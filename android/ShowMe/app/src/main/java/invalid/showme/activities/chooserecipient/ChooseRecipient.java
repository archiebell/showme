package invalid.showme.activities.chooserecipient;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.acra.ACRA;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import invalid.showme.R;
import invalid.showme.activities.SharedActivityLogic;
import invalid.showme.activities.choosemessage.ChooseMessage;
import invalid.showme.activities.friendslist.FriendsListArrayAdaptor;
import invalid.showme.activities.viewphoto.ViewPhotoNormal;
import invalid.showme.exceptions.ActivityResultException;
import invalid.showme.exceptions.IntentDataException;
import invalid.showme.exceptions.SharedPhotoException;
import invalid.showme.model.Friend;
import invalid.showme.model.IFriend;
import invalid.showme.model.UserProfile;
import invalid.showme.model.db.DBHelper;
import invalid.showme.model.message.OutgoingMessage;
import invalid.showme.model.photo.DraftPhoto;
import invalid.showme.model.photo.SentPhoto;
import invalid.showme.model.photo.savejob.DraftSaveJob;
import invalid.showme.model.photo.savejob.SentSaveJob;
import invalid.showme.model.server.ServerRequestJob;
import invalid.showme.util.BitmapUtils;
import invalid.showme.util.IOUtil;
import invalid.showme.util.JobPriorityUtil;
import invalid.showme.util.PhotoFileManager;
import invalid.showme.util.TimeUtil;

public class ChooseRecipient extends ActionBarActivity {
    private final String TAG = "ChooseRecipient";

    private static final int NONE = 0;
    private static final int HAVEDRAFT = 1;
    private static final int HAVEPHOTO = 2;
    private static final int SHAREDPHOTO = 3;
    private int mode = NONE;

    private String message;
    private File imgFile;
    private File thumbnailFile;
    private long draftToSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedActivityLogic.ConfirmInitialization(getApplicationContext(), true);
        setTitle(R.string.title_activity_choose_recipient);
        setContentView(R.layout.activity_choose_recipient_with_photo);

        final UserProfile up = (UserProfile)getApplicationContext();
        final ChooseRecipient chooseRecipientActivity = this;

        Intent intent = getIntent();



        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            mode = SHAREDPHOTO;
            if(intent.getType() == null || !intent.getType().startsWith("image/")) {
                String msg = "Somehow got photo shared with us that not photo? " + intent.getType();
                Log.e(TAG, msg);
                ACRA.getErrorReporter().handleException(new SharedPhotoException(msg));
                Toast.makeText(getApplicationContext(), "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
                finish();
            }
            else {
                Uri tmpSharedPhotoUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (tmpSharedPhotoUri == null) {
                    String msg = "Somehow got photo shared with us with null URI? ";
                    Log.e(TAG, msg);
                    ACRA.getErrorReporter().handleException(new SharedPhotoException(msg));
                    Toast.makeText(getApplicationContext(), "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    try {
                        Bitmap tmpSharedPhotoFullSize = MediaStore.Images.Media.getBitmap(getContentResolver(), tmpSharedPhotoUri);
                        try {
                            this.imgFile = PhotoFileManager.createTempImageFile(getApplicationContext(), tmpSharedPhotoFullSize);
                            tmpSharedPhotoFullSize.recycle();
                            this.thumbnailFile = PhotoFileManager.createThumbnailFromFullSizedFileToFile(this.imgFile);
                        }catch (IOException e) {
                            String msg = "IOException saving files temporarily!!";
                            Log.e(TAG, msg);
                            ACRA.getErrorReporter().handleException(new SharedPhotoException(msg, e));
                            Toast.makeText(getApplicationContext(), "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
                        }
                    } catch (IOException e) {
                        String msg = "couldn't read from URI.";
                        Log.e(TAG, msg);
                        ACRA.getErrorReporter().handleException(new SharedPhotoException(msg, e));
                        Toast.makeText(getApplicationContext(), "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }



        DraftPhoto draftPhoto = null;
        draftToSend = intent.getLongExtra("draftToSend", -1);
        if(draftToSend != -1 && mode != NONE) {
            String msg = "Somehow have both draft and shared photo!";
            Log.e(TAG, msg);
            ACRA.getErrorReporter().putCustomData("intentData", IOUtil.intentToString(intent));
            ACRA.getErrorReporter().handleException(new IntentDataException(msg));
            ACRA.getErrorReporter().removeCustomData("intentData");
            Toast.makeText(getApplicationContext(), "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
        } else if(draftToSend != -1) {
            mode = HAVEDRAFT;
            draftPhoto = up.findDraft(draftToSend);
        }



        String tmpImgPath = intent.getStringExtra("photoPath");
        String tmpThumbnailPath = intent.getStringExtra("thumbnailPath");
        if(tmpImgPath != null && mode != NONE) {
            String msg = "Somehow have multiple modes!";
            Log.e(TAG, msg);
            ACRA.getErrorReporter().putCustomData("intentData", IOUtil.intentToString(intent));
            ACRA.getErrorReporter().handleException(new IntentDataException(msg));
            ACRA.getErrorReporter().removeCustomData("intentData");

            Toast.makeText(getApplicationContext(), "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
        }
        else if(tmpImgPath != null) {
            mode = HAVEPHOTO;
            this.imgFile = new File(tmpImgPath);
            this.thumbnailFile = new File(tmpThumbnailPath);
        }



        if(mode == NONE) {
            String msg = "Somehow didn't get any mode!";
            Log.e(TAG, msg);
            ACRA.getErrorReporter().putCustomData("intentData", IOUtil.intentToString(getIntent()));
            ACRA.getErrorReporter().handleException(new IntentDataException(msg));
            ACRA.getErrorReporter().removeCustomData("intentData");
        }



        final CheckBox chkBox = (CheckBox)findViewById(R.id.chooserecipient_private_checkbox);
        if(savedInstanceState != null) {
            this.message = savedInstanceState.getString("message");
            updateMessageOptions();
        } else if(mode == HAVEDRAFT) {
            this.message = draftPhoto.Message;
            updateMessageOptions();
            chkBox.setChecked(draftPhoto.PrivatePhoto);
        }



        final ImageView iv = (ImageView) findViewById(R.id.chooserecipient_photo_thumbnail);
        if(mode == HAVEDRAFT) {
            BitmapUtils.setImageForView(iv, draftPhoto.getRealThumbnail(getApplicationContext()));
        }
        else if(mode == HAVEPHOTO || mode == SHAREDPHOTO) {
            BitmapUtils.setImageForView(iv, chooseRecipientActivity.thumbnailFile);
        } else {
            String msg = "Mode isn't Draft or Photo!! (in onCreate)";
            Log.e(TAG, msg);
            ACRA.getErrorReporter().putCustomData("intentData", IOUtil.intentToString(intent));
            ACRA.getErrorReporter().handleException(new IntentDataException(msg));
            ACRA.getErrorReporter().removeCustomData("intentData");
            Toast.makeText(getApplicationContext(), "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
        }
        /*
        Seems not necessary and layout scales image automatically?
        hasBeenScaled = false;
        ViewTreeObserver viewTree = iv.getViewTreeObserver();
        viewTree.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                if (!hasBeenScaled) {
                    if(mode == HAVEDRAFT) {
                        if(draftToSend == -1) {
                            Log.e(TAG, "Somehow got into addOnPreDrawListener with mode == HAVEDRAFT and draftToSend == -1");
                            Toast.makeText(getApplicationContext(), "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            DraftPhoto d = up.getDrafts().get(draftToSend);
                            BitmapUtils.scaleImageForView(iv, d.getThumbnail());
                        }
                    }
                    else if(mode == HAVEPHOTO || mode == SHAREDPHOTO) {
                        BitmapUtils.scaleImageForView(iv, chooseRecipientActivity.thumbnailFile);
                    } else {
                        Log.e(TAG, "Mode isn't Draft or Photo!! (in addOnPreDrawListener)");
                        Toast.makeText(getApplicationContext(), "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
                    }
                    hasBeenScaled = true;
                }
                return true;
            }
        });*/



        ArrayList<IFriend> friends = (ArrayList<IFriend>)up.getFriends();
        if(draftToSend == -1) {
            friends = (ArrayList<IFriend>) friends.clone();
            Friend saveForLater = new Friend("Save For Later");
            friends.add(0, saveForLater);
        }



        long limitRecipient = intent.getLongExtra("limitRecipients", -1);
        if(limitRecipient != -1) {
            IFriend specialFriend = null;
            int indexOfSpecialFriend = -1;
            for(int i=0; i<friends.size(); i++)
                if(friends.get(i).getID() == limitRecipient) {
                    indexOfSpecialFriend = i;
                    specialFriend = friends.get(i);
                    break;
                }

            if(specialFriend != null) {
                friends.remove(indexOfSpecialFriend);
                friends.add(0, specialFriend);
            }
        }


        FriendsListArrayAdaptor friendsAdaptor = new FriendsListArrayAdaptor(this, friends);
        final ListView lv = (ListView)findViewById(R.id.chooserecipient_list_friends);
        lv.setAdapter(friendsAdaptor);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                IFriend recipient = (IFriend) lv.getItemAtPosition(position);
                UserProfile me = (UserProfile) getApplication();
                Boolean privatePhoto = chkBox.isChecked();

                try {
                    if (recipient.getPublicKey() == null) {//Save For Later
                        if (mode == HAVEDRAFT) {
                            String msg = "Somehow user chose 'Save For Later' as recipient even though had chosen to send draft.";
                            Log.e(TAG, msg);
                            ACRA.getErrorReporter().putCustomData("intentData", IOUtil.intentToString(getIntent()));
                            ACRA.getErrorReporter().handleException(new IntentDataException(msg));
                            ACRA.getErrorReporter().removeCustomData("intentData");
                            Toast.makeText(getApplicationContext(), "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
                        } else if(mode == HAVEPHOTO || mode == SHAREDPHOTO) {
                            DraftPhoto draftPhoto = new DraftPhoto(chooseRecipientActivity.message, privatePhoto, chooseRecipientActivity.imgFile.getAbsolutePath(), chooseRecipientActivity.thumbnailFile.getAbsolutePath());
                            me.getDrafts().add(0, draftPhoto);
                            me.getJobManager().addJob(new DraftSaveJob(draftPhoto.getID(), draftPhoto));
                            Toast.makeText(getApplicationContext(), R.string.draft_saved, Toast.LENGTH_LONG).show();
                        } else {
                            String msg= "Mode isn't Draft or Photo!! (in setOnItemClickListener for save later)";
                            Log.e(TAG, msg);
                            ACRA.getErrorReporter().putCustomData("intentData", IOUtil.intentToString(getIntent()));
                            ACRA.getErrorReporter().handleException(new IntentDataException(msg));
                            ACRA.getErrorReporter().removeCustomData("intentData");

                            Toast.makeText(getApplicationContext(), "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        OutgoingMessage m = null;
                        SentPhoto sentPhoto = null;
                        Boolean shouldSaveSentPhoto =
                                (!privatePhoto && PreferenceManager.getDefaultSharedPreferences(chooseRecipientActivity.getApplicationContext()).getBoolean("pref_savesentmessages", true)) ||
                                (privatePhoto && PreferenceManager.getDefaultSharedPreferences(chooseRecipientActivity.getApplicationContext()).getBoolean("pref_savesentprivatemessages", false));
                        if(mode == HAVEDRAFT) {
                            DraftPhoto d = up.findDraft(draftToSend);
                            if(shouldSaveSentPhoto) {
                                sentPhoto = new SentPhoto(SentPhoto.SentPhotoStatus.Encrypting, "", recipient.getID(), chooseRecipientActivity.message, privatePhoto, TimeUtil.GetNow(), d);
                                if (!sentPhoto.saveToDatabase(DBHelper.getInstance(me)))
                                    throw new RuntimeException("Could not save sent photo to database.");
                            }
                            m = new OutgoingMessage(shouldSaveSentPhoto ? sentPhoto.getID() : -1, d, chooseRecipientActivity.message, privatePhoto, recipient, me);
                        }
                        else if(mode == HAVEPHOTO || mode == SHAREDPHOTO) {
                            if(shouldSaveSentPhoto) {
                                //need to duplicate original file; otherwise Sent Photo job and Send Photo job
                                // will remove same file, or leave file around.
                                File imgDuped = PhotoFileManager.createTempImageFile(me);
                                PhotoFileManager.CopyFile(chooseRecipientActivity.imgFile, imgDuped);
                                sentPhoto = new SentPhoto(SentPhoto.SentPhotoStatus.Encrypting, null, recipient.getID(), chooseRecipientActivity.message, privatePhoto, TimeUtil.GetNow(), imgDuped.getAbsolutePath(), chooseRecipientActivity.thumbnailFile.getAbsolutePath());
                                if (!sentPhoto.saveToDatabase(DBHelper.getInstance(me)))
                                    throw new RuntimeException("Could not save sent photo to database.");
                            }

                            m = new OutgoingMessage(shouldSaveSentPhoto ? sentPhoto.getID() : -1, chooseRecipientActivity.imgFile.getAbsolutePath(), chooseRecipientActivity.message, privatePhoto, recipient, me);
                        } else {
                            String msg = "Mode isn't Draft or Photo!! (in setOnItemClickListener for send now)";
                            Log.e(TAG, msg);
                            ACRA.getErrorReporter().putCustomData("intentData", IOUtil.intentToString(getIntent()));
                            ACRA.getErrorReporter().handleException(new IntentDataException(msg));
                            ACRA.getErrorReporter().removeCustomData("intentData");
                            Toast.makeText(getApplicationContext(), "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
                            chooseRecipientActivity.finish();
                            return;
                        }
                        if(shouldSaveSentPhoto) {
                            me.getSent().add(0, sentPhoto);
                            me.getJobManager().addJob(new SentSaveJob(sentPhoto.getID()));
                        }
                        me.getJobManager().addJobInBackground(new ServerRequestJob(JobPriorityUtil.JobType.PutJob, m));
                        Toast.makeText(getApplicationContext(), R.string.message_sending, Toast.LENGTH_LONG).show();
                    }
                } catch(IOException e) {
                    ACRA.getErrorReporter().handleException(e);
                    Toast.makeText(getApplicationContext(), "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
                }
            chooseRecipientActivity.finish();
            }
        });
    }

    private void updateMessageOptions() {
        TextView msg = (TextView)findViewById(R.id.chooserecipient_message);
        msg.setText(this.message);

        Button addEditMsg = (Button)findViewById(R.id.chooserecipient_addedit_message);
        if(this.message != null && this.message.length() > 0)
            addEditMsg.setText("Edit Message");
        else
            addEditMsg.setText("Add Message");
    }

    @SuppressWarnings({"unused", "UnusedParameters"})
    public void chooseMessage(View view) {
        Intent intent = new Intent(this, ChooseMessage.class);
        intent.putExtra("message", this.message);
        if(mode == HAVEDRAFT)
            intent.putExtra("draftToSend", this.draftToSend);
        else if(mode == HAVEPHOTO || mode == SHAREDPHOTO)
            intent.putExtra("thumbnailPath", this.thumbnailFile.getAbsolutePath());

        startActivityForResult(intent, 1);
    }

    @SuppressWarnings({"unused", "UnusedParameters"})
    public void showPhoto(View view){
        Intent intent = new Intent(this, ViewPhotoNormal.class);
        if(mode == HAVEPHOTO || mode == SHAREDPHOTO)
            intent.putExtra("thumbnailPath", this.thumbnailFile.getAbsolutePath());
        else if(mode == HAVEDRAFT)
            intent.putExtra("draftToView", this.draftToSend);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1) {
            this.message = data.getStringExtra("message");
            updateMessageOptions();
        } else {
            String msg = "Got requestCode wasn't expecting:" + requestCode;
            Log.e(TAG, msg);
            ACRA.getErrorReporter().handleException(new ActivityResultException(msg));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putString("message", this.message);
    }
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        this.message = savedInstanceState.getString("message");
        updateMessageOptions();
    }



    // Menu Operations

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_choose_recipient, menu);
        return true;
    }

}
