package invalid.showme.activities.draftslist;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import org.acra.ACRA;

import de.greenrobot.event.EventBus;
import invalid.showme.R;
import invalid.showme.activities.SharedActivityLogic;
import invalid.showme.activities.chooserecipient.ChooseRecipient;
import invalid.showme.exceptions.ActivityResultException;
import invalid.showme.layoutobjects.DataAdaptorRefreshEvent;
import invalid.showme.model.UserProfile;
import invalid.showme.model.db.DBHelper;
import invalid.showme.model.photo.DraftPhoto;
import invalid.showme.model.photo.getthumbnail.DraftThumbnailLoadEvent;

public class ChooseDraft extends ActionBarActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    private final static String TAG = "ChooseDraft";

    private long friendToSendTo;

    private DraftPhoto selectedDraftForDeletion = null;
    private AlertDialog deleteDraftDialog;

    private DraftsListArrayAdaptor draftsAdaptor;
    private GridView gv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedActivityLogic.ConfirmInitialization(getApplicationContext(), true);
        setContentView(R.layout.activity_choose_draft);

        Intent intent = getIntent();
        this.friendToSendTo = intent.getLongExtra("friendToSendTo", -1);

        UserProfile up = (UserProfile)getApplication();
        EventBus.getDefault().register(this);

        final ChooseDraft chooseDraftActivity = this;
        gv = (GridView) findViewById(R.id.choosedraft_list_photos);
        draftsAdaptor = new DraftsListArrayAdaptor(this, gv, up, up.getDrafts());
        gv.setAdapter(draftsAdaptor);
        gv.setEmptyView(findViewById(R.id.choosedraft_empty));
        gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DraftPhoto p = (DraftPhoto) gv.getItemAtPosition(position);

                Intent intent1 = new Intent(chooseDraftActivity, ChooseRecipient.class);
                intent1.putExtra("draftToSend", p.getID());
                if (friendToSendTo != -1)
                    intent1.putExtra("limitRecipients", friendToSendTo);
                startActivity(intent1);
                chooseDraftActivity.finish();
            }
        });
        gv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                selectedDraftForDeletion = (DraftPhoto) gv.getItemAtPosition(position);

                AlertDialog.Builder builder = new AlertDialog.Builder(chooseDraftActivity);

                builder.setTitle(R.string.action_delete_draft);
                builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        UserProfile me = (UserProfile) getApplication();
                        if (!DraftPhoto.DeleteFromDatabase(DBHelper.getInstance(getApplicationContext()), selectedDraftForDeletion.getID())) {
                            Toast.makeText(getApplicationContext(), "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
                        } else {
                            me.getDrafts().remove(selectedDraftForDeletion);
                            selectedDraftForDeletion.DeleteFiles();
                            draftsAdaptor.notifyDataSetChanged();
                        }
                        selectedDraftForDeletion = null;
                        deleteDraftDialog.dismiss();
                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        selectedDraftForDeletion = null;
                        dialog.dismiss();
                    }
                });

                deleteDraftDialog = builder.create();
                deleteDraftDialog.show();

                return true;
            }
        });
    }

    public void onEvent(DraftThumbnailLoadEvent event) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                gv.invalidateViews();
            }
        });
    }
    public void onEvent(DraftSaveJobEvent event) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                draftsAdaptor.notifyDataSetChanged();
            }
        });
    }
    public void onEvent(DataAdaptorRefreshEvent event)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                draftsAdaptor.notifyDataSetChanged();
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu; adds items to action bar if it present.
        getMenuInflater().inflate(R.menu.menu_choose_draft, menu);
        return true;
    }




    // Photo Operations

    private static final int ACTIVITY_REQUEST_IMAGE_CAPTURE = 1;
    public  void takePhoto(View view) {
        SharedActivityLogic.TakePhoto(TAG, this, ACTIVITY_REQUEST_IMAGE_CAPTURE);
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
            default:
                String msg = "Got requestCode wasn't expecting:" + requestCode;
                Log.e(TAG, msg);
                ACRA.getErrorReporter().handleException(new ActivityResultException(msg));
                Toast.makeText(getApplicationContext(), "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
        }
    }
}
