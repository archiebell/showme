package invalid.showme.activities.settings;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import info.guardianproject.netcipher.proxy.OrbotHelper;
import invalid.showme.R;
import invalid.showme.activities.SharedActivityLogic;

public class SettingsFragment extends PreferenceFragment
{
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        Preference torPref = findPreference("pref_usetor");
        torPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                boolean enableTor = (boolean)o;
                if(enableTor && !OrbotHelper.isOrbotInstalled(getActivity())) {

                    SharedActivityLogic.PromptForOrbot(getActivity());
                    return false;
                } else if(enableTor && !OrbotHelper.isOrbotRunning(getActivity())) {
                    OrbotHelper.requestStartTor(getActivity());
                    return true;
                } else {
                    return true;
                }
            }
        });
    }
}
