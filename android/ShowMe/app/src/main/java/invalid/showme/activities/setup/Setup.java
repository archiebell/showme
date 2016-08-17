package invalid.showme.activities.setup;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import org.whispersystems.libaxolotl.ecc.Curve;
import org.whispersystems.libaxolotl.ecc.ECKeyPair;

import info.guardianproject.netcipher.proxy.OrbotHelper;
import invalid.showme.R;
import invalid.showme.activities.SharedActivityLogic;
import invalid.showme.activities.friendslist.FriendsListActivity;
import invalid.showme.model.UserProfile;
import invalid.showme.model.server.ServerRequestJob;
import invalid.showme.util.JobPriorityUtil;


public class Setup extends ActionBarActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    final static private String TAG = "Setup";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_setup);

        EditText displayName = (EditText) findViewById(R.id.setup_name);
        displayName.setText(getDefaultDisplayName());

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Button fillInName = (Button) findViewById(R.id.setup_button_fillinname);
            fillInName.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        //No
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_setup, menu);
        return true;
    }

    private String getDefaultDisplayName() {
        final AccountManager manager = AccountManager.get(this);
        final Account[] accounts = manager.getAccountsByType("com.google");
        if(accounts.length > 0 && accounts[0].name != null)
        {
            Cursor c = getApplication().getContentResolver().query(
                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Email.DATA + " = ?",
                    new String[] { accounts[0].name },
                    null);
            if(c.moveToNext()) {
                String name = c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                c.close();
                return name;
            }
        }
        return "Unnamed";
    }

    final static private int PERMISSIONS_REQUEST_GET_ACCOUNTS = 2;
    public void fillInName(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)  {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.GET_ACCOUNTS, Manifest.permission.READ_CONTACTS},
                        PERMISSIONS_REQUEST_GET_ACCOUNTS);
            }
            else {
                EditText displayName = (EditText) findViewById(R.id.setup_name);
                displayName.setText(getDefaultDisplayName());
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_GET_ACCOUNTS: {
                if (grantResults.length > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    EditText displayName = (EditText) findViewById(R.id.setup_name);
                    displayName.setText(getDefaultDisplayName());
                }
            }
        }
    }

    @SuppressWarnings({"unused", "UnusedParameters"})
    public void performSetup(View view) throws Exception {
        EditText editText = (EditText) findViewById(R.id.setup_name);
        CheckBox useTor = (CheckBox) findViewById(R.id.setup_usetor);

        if(useTor.isChecked()) {
            PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putBoolean("pref_usetor", true).commit();
            if(!OrbotHelper.isOrbotInstalled(getApplicationContext())) {
                SharedActivityLogic.PromptForOrbot(this);
                return;
            } else {
                OrbotHelper.requestStartTor(getApplicationContext());
            }
        } else {
            PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putBoolean("pref_usetor", false).commit();
        }

        ECKeyPair identityKey = Curve.generateKeyPair();

        UserProfile profile = (UserProfile)getApplication();
        profile.createSelf(editText.getText().toString(), identityKey);
        if(!profile.saveToDatabase(getApplicationContext())) {
            Toast.makeText(getApplicationContext(), "Unknown error, check logcat?", Toast.LENGTH_SHORT).show();
        } else {
            profile.initializeFriends();
            profile.initializeDrafts();
            profile.initializeSent();

            profile.getJobManager().addJob(new ServerRequestJob(JobPriorityUtil.JobType.RegisterJob));
            profile.getJobManager().addJob(new ServerRequestJob(JobPriorityUtil.JobType.TokenJob));

            Intent intent = new Intent(this, FriendsListActivity.class);
            startActivity(intent);
        }
    }
}
