package teamk.glasgowbusfinder.ui.preferences;

import teamk.glasgowbusfinder.R;
import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Allows the user to set preferences for
 * the map activity.
 * 
 * @author Euan Freeman
 */
public class MapPreferencesActivity extends PreferenceActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/* Inflate UI preference options */
		addPreferencesFromResource(R.xml.preferences_map);
	}
}
