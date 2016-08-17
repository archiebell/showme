package invalid.showme.activities.settings;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

public class Settings extends ActionBarActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
}
