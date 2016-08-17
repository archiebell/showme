package invalid.showme.activities.sentmessages;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import de.greenrobot.event.EventBus;
import invalid.showme.R;
import invalid.showme.activities.SharedActivityLogic;
import invalid.showme.activities.settings.Settings;
import invalid.showme.activities.viewphoto.ViewPhotoNormal;
import invalid.showme.activities.viewphoto.ViewPhotoTransient;
import invalid.showme.layoutobjects.DataAdaptorRefreshEvent;
import invalid.showme.model.UserProfile;
import invalid.showme.model.db.DBHelper;
import invalid.showme.model.photo.SentPhoto;
import invalid.showme.model.photo.getthumbnail.ReceivedPhotoThumbnailLoadEvent;
import invalid.showme.model.server.ServerRequestJob;
import invalid.showme.model.server.result.ServerResultEvent;
import invalid.showme.util.JobPriorityUtil;

public class SentMessages extends ActionBarActivity implements SwipeRefreshLayout.OnRefreshListener, SharedPreferences.OnSharedPreferenceChangeListener
{
    private static final String TAG = "SentMessages";

    private SentMessagesAdaptor photosAdaptor;
    private GridView gv;
    private SwipeRefreshLayout swipeContainer;

    private AlertDialog deleteSentDialog;
    private SentPhoto selectedPhotoForDeletion;

    private LinearLayout nonStandardSettingAlertContainer;
    private TextView savingSentMessagesDisabledAlert;
    private TextView savingPrivateMessagesEnabledAlert;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedActivityLogic.ConfirmInitialization(getApplicationContext(), false);
        setContentView(R.layout.activity_view_sent);

        final UserProfile up = (UserProfile)getApplication();
        EventBus.getDefault().register(this);

        final SentMessages viewSentMessagesActivity = this;


        List<SentPhoto> photos = up.getSent();
        for(SentPhoto p : photos)
        {
            if(p.ShouldBeDeleted()) {
                photos.remove(p);
                p.DeleteFiles();
                if(!SentPhoto.DeleteFromDatabase(DBHelper.getInstance(this), p.getID())) {
                    Toast.makeText(getApplicationContext(), "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
                }
            }
        }



        nonStandardSettingAlertContainer = (LinearLayout)findViewById(R.id.viewsent_nonstandardsettingalertcontainer);
        savingSentMessagesDisabledAlert = (TextView)findViewById(R.id.viewsent_savingsentmessagesdisabledalert);
        savingPrivateMessagesEnabledAlert = (TextView)findViewById(R.id.viewsent_savingprivatemessagesenabledalert);

        recalculateSettingsViews();



        gv = (GridView) findViewById(R.id.viewsent_list_photos);
        photosAdaptor = new SentMessagesAdaptor(this, gv, up, photos);
        gv.setAdapter(photosAdaptor);
        gv.setEmptyView(findViewById(R.id.viewsent_empty));
        gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SentPhoto photo = (SentPhoto) gv.getItemAtPosition(position);

                Intent intent1;
                if (photo.PrivatePhoto) {
                    intent1 = new Intent(viewSentMessagesActivity, ViewPhotoTransient.class);
                } else {
                    intent1 = new Intent(viewSentMessagesActivity, ViewPhotoNormal.class);
                }
                intent1.putExtra("sentToView", photo.getID());

                startActivity(intent1);
            }
        });
        gv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                selectedPhotoForDeletion = (SentPhoto) gv.getItemAtPosition(position);

                AlertDialog.Builder builder = new AlertDialog.Builder(viewSentMessagesActivity);

                builder.setTitle(R.string.action_delete_sentphoto);
                builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (!SentPhoto.DeleteFromDatabase(DBHelper.getInstance(getApplicationContext()), selectedPhotoForDeletion.getID())) {
                            Toast.makeText(getApplicationContext(), "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
                        } else {
                            List<SentPhoto> photos = up.getSent();
                            for(SentPhoto s : photos)
                                if(s.getID() == selectedPhotoForDeletion.getID()) {
                                    photos.remove(s);
                                    break;
                                }
                            photosAdaptor.notifyDataSetChanged();
                        }
                        selectedPhotoForDeletion = null;
                        deleteSentDialog.dismiss();
                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        selectedPhotoForDeletion = null;
                        dialog.dismiss();
                    }
                });

                deleteSentDialog = builder.create();
                deleteSentDialog.show();

                return true;
            }
        });



        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.viewsent_swipe_container);
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
        recalculateSettingsViews();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_view_sentmessages, menu);
        return true;
    }



    // Activity Result

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

    public void editSettings(View view)
    {
        Intent intent = new Intent(this, Settings.class);
        startActivity(intent);
    }



    // Gestures

    @Override
    public void onRefresh() {
        UserProfile up = (UserProfile)getApplication();
        swipeContainer.setRefreshing(true);
        up.getJobManager().addJob(new ServerRequestJob(JobPriorityUtil.JobType.StatusJob));
    }

    public void onEvent(ServerResultEvent event)
    {
        SharedActivityLogic.HandleServerResponse(TAG, this, event, photosAdaptor, swipeContainer);
    }
    public void onEvent(DataAdaptorRefreshEvent event)
    {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                photosAdaptor.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        photosAdaptor.notifyDataSetChanged();
        recalculateSettingsViews();
    }

    private void recalculateSettingsViews() {
        Boolean saveSentMessages = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("pref_savesentmessages", true);
        Boolean saveSentPrivateMessages = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("pref_savesentprivatemessages", false);
        savingSentMessagesDisabledAlert.setVisibility(saveSentMessages ? View.GONE : View.VISIBLE);
        savingPrivateMessagesEnabledAlert.setVisibility(saveSentPrivateMessages ? View.VISIBLE : View.GONE);
        nonStandardSettingAlertContainer.setVisibility(!saveSentMessages || saveSentPrivateMessages ? View.VISIBLE : View.GONE);
    }
}
