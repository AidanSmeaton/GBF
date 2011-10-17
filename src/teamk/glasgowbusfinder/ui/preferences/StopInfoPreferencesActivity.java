package teamk.glasgowbusfinder.ui.preferences;

import teamk.glasgowbusfinder.R;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class StopInfoPreferencesActivity extends PreferenceActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/* Inflate UI preference options */
		addPreferencesFromResource(R.xml.preferences_stopinfo);
	}
}
