/*
 * Copyright (C) 2016 Player One
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package player.efis.pfd;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.view.View;
import android.widget.ListView;

public class EFISPrefManage extends PreferenceActivity
{

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.manage);
        updateSummary();
    }


    @SuppressWarnings("deprecation")
    private void updateSummary()
    {
        ListPreference lp;
        lp = (ListPreference) findPreference("AircraftModel");
        lp.setSummary(lp.getEntry());

        //lp = (ListPreference) findPreference("AirportDatabase");
        //lp.setSummary(lp.getEntry());

        lp = (ListPreference) findPreference("sensorBias");
        lp.setSummary(lp.getEntry());


        // Get the version number of the app
        PackageInfo pInfo = null;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        }
        catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        String version = pInfo.versionName;


        PreferenceScreen ps;
        ps = (PreferenceScreen) findPreference("version");
        ps.setSummary(version);
    }
}


/*
import android.os.Bundle;
import android.preference.PreferenceFragment;

public class AppAirplane extends PreferenceFragment
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{ 
		super.onCreate(savedInstanceState); // Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.airplane);
	}
}
*/

/*
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class UserSettingActivity extends PreferenceActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.settings);

	}
}
*/