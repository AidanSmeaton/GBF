package teamk.glasgowbusfinder.ui.preferences;

import teamk.glasgowbusfinder.R;
import teamk.glasgowbusfinder.util.InputValidator;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;

/**
 * Allows the user to set preferences for
 * the Alerts functionality of the app.
 * 
 * @author Euan Freeman
 */
public class AlertsPreferencesActivity extends PreferenceActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/* Inflate UI preference options */
		addPreferencesFromResource(R.xml.preferences_alerts);
		
		Preference temp = getPreferenceScreen().findPreference(getString(R.string.pref_alertsBeforeTime));
		temp.setOnPreferenceChangeListener(numberCheckListener);
		
		temp = getPreferenceScreen().findPreference(getString(R.string.pref_alertsAutoRemoveTime));
		temp.setOnPreferenceChangeListener(numberCheckListener);
	}

	/* 
	 * http://stackoverflow.com/questions/3206765/number-preferences-in-preference-activity-in-android
	 */
	private OnPreferenceChangeListener numberCheckListener = new OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			return InputValidator.validateInteger(getBaseContext(),newValue.toString());
		}
		
		/*
		private boolean validateInput(String newValue) {
			//* -? is optional negative symbol
			// * \\d+ means one or more integers
			// 
			if (newValue.length() > 0 && newValue.matches("-?\\d+")) {
				if (Integer.parseInt(newValue) >= 0) {
					return true;
				} else {
					Toast.makeText(AlertsPreferencesActivity.this,
							getResources().getString(R.string.must_be_positive_number),
							Toast.LENGTH_SHORT).show();
				}
			} else {
				Toast.makeText(AlertsPreferencesActivity.this,
						newValue+" "+getResources().getString(R.string.is_an_invalid_number),
						Toast.LENGTH_SHORT).show();
			}
			
			return false;
		}
	*/
	};
}