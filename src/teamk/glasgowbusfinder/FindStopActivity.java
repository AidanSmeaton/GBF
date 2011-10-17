package teamk.glasgowbusfinder;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

/**
 * This activity provides the user with different ways to
 * locate a bus stop. These include searching by text,
 * showing the current location on a map, or showing the
 * user their favourites.
 * 
 * @author Euan Freeman
 * @author Aidan Smeaton
 * @author Calum McCall
 */
public class FindStopActivity extends SearchActivity {
	public static final int DEFAULT_MAX_DISTANCE = 1000;
	public static final int MAX_GEOCODE_RESULTS = 5;
	public static final double BOUNDARY_LOWER_LEFT_LATITUDE = 50.0; //55
	public static final double BOUNDARY_LOWER_LEFT_LONGITUDE = -10.0; //-6
	public static final double BOUNDARY_UPPER_RIGHT_LATITUDE = 60.0; //57
	public static final double BOUNDARY_UPPER_RIGHT_LONGITUDE = 1.0; //-2
	
	private RadioButton stopRadio;
	private RadioButton placeRadio;
	private CheckBox streetCheck;
	private CheckBox townCheck;
	private CheckBox descriptionCheck;
	private CheckBox numberCheck;
	private SharedPreferences sharedPrefs;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.findstop);

		searchBar = (EditText) findViewById(R.id.findstop_edittext_search);

		searchBar.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
					searchBar = (EditText) findViewById(R.id.findstop_edittext_search);

					searchClicked(searchBar);
					
					return true;
				}
				return false;
			}
		});

		/* Set up the check boxes and radio buttons. */
		stopRadio = (RadioButton) findViewById(R.id.radio_stop);
		
		stopRadio.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				streetCheck.setVisibility(View.VISIBLE);
				townCheck.setVisibility(View.VISIBLE);
				descriptionCheck.setVisibility(View.VISIBLE);
				numberCheck.setVisibility(View.VISIBLE);
			}
		});
		
		placeRadio = (RadioButton) findViewById(R.id.radio_place);
		
		placeRadio.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				streetCheck.setVisibility(View.INVISIBLE);
				townCheck.setVisibility(View.INVISIBLE);
				descriptionCheck.setVisibility(View.INVISIBLE);
				numberCheck.setVisibility(View.INVISIBLE);
			}
		});

		streetCheck = (CheckBox) findViewById(R.id.checkbox_street);
		townCheck = (CheckBox) findViewById(R.id.checkbox_town);
		descriptionCheck = (CheckBox) findViewById(R.id.checkbox_stopdescription);
		numberCheck = (CheckBox) findViewById(R.id.checkbox_stopnumber);

		Resources res = getResources();
		sharedPrefs = getApplicationContext().getSharedPreferences(res.getString(R.string.sharedPrefsName), Context.MODE_PRIVATE);
		
		streetCheck.setChecked(sharedPrefs.getBoolean(res.getString(R.string.pref_findStreetName), true));
		townCheck.setChecked(sharedPrefs.getBoolean(res.getString(R.string.pref_findTownName), true));
		descriptionCheck.setChecked(sharedPrefs.getBoolean(res.getString(R.string.pref_findDescription), true));
		numberCheck.setChecked(sharedPrefs.getBoolean(res.getString(R.string.pref_findNumber), true));
			
		placeRadio.setChecked(true);
	
	}
	
	/**
	 * Called when the FindStopActivity is
	 * resumed. Resets the state of the
	 * checkboxes and radio buttons.
	 */
	@Override
	public void onResume() {
		super.onResume();
		
		boolean searchByStop = sharedPrefs.getBoolean(getResources().getString(R.string.pref_searchByStop), false);
		
		stopRadio.setChecked(searchByStop);
		placeRadio.setChecked(!searchByStop);
		
		streetCheck.setVisibility(searchByStop ? View.VISIBLE : View.INVISIBLE);
		townCheck.setVisibility(searchByStop ? View.VISIBLE : View.INVISIBLE);
		descriptionCheck.setVisibility(searchByStop ? View.VISIBLE : View.INVISIBLE);
		numberCheck.setVisibility(searchByStop ? View.VISIBLE : View.INVISIBLE);
	}
	
	/**
	 * Called when this activity gets paused.
	 * Writes shared preferences.
	 */
	@Override
	public void onPause() {
		super.onPause();
		
		SharedPreferences.Editor prefEditor = sharedPrefs.edit();
		Resources res = getResources();
		
		prefEditor.putBoolean(res.getString(R.string.pref_searchByStop), stopRadio.isChecked());
		prefEditor.putBoolean(res.getString(R.string.pref_findStreetName), streetCheck.isChecked());
		prefEditor.putBoolean(res.getString(R.string.pref_findTownName), townCheck.isChecked());
		prefEditor.putBoolean(res.getString(R.string.pref_findDescription), descriptionCheck.isChecked());
		prefEditor.putBoolean(res.getString(R.string.pref_findNumber), numberCheck.isChecked());
				
		prefEditor.commit();
	}

	/**
	 * Handles click events on the search
	 * EditText widget. Determines which
	 * search mode to use.
	 * @param v
	 */
	public void searchClicked(View v) {
		searchBar = (EditText) findViewById(R.id.findstop_edittext_search);

		String text = searchBar.getText().toString();
		
		/* http://stackoverflow.com/questions/1109022/how-to-close-hide-the-android-soft-keyboard/1109108#1109108 */
		if (text.length() > 0) {
			/* Get the on-screen keyboard and hide if shown */
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			
			if (imm != null) {
				imm.hideSoftInputFromWindow(searchBar.getWindowToken(), 0);
			}
		}
		
		if (placeRadio.isChecked()){
			searchPlace(text);
		} else {
			searchStop(text);
		}
	}
	
	/*
	 * Handle regular stop searches. Searches
	 * within the database for the search term.
	 */
	private void searchStop(String text) {
		if (isValidSearchText(text)) {
			Intent findStopResultsIntent = new Intent(this, TabbedSearchActivity.class);
			findStopResultsIntent.putExtra("firstTab", "searchStopResult");
			findStopResultsIntent.putExtra("search", text.trim());
			String criteria = writeCriteriaString();
			
			if (criteria.equals("0000")){
				Toast.makeText(this, "At least one checkbox must be ticked", Toast.LENGTH_LONG).show();
			} else {
				findStopResultsIntent.putExtra("criteria", criteria);
				startActivity(findStopResultsIntent);
			}
		} else {
			/* Clear the search bar */
			searchBar.setText("");
		}
	}

	/*
	 * Handle location searches. Attempts to geocode
	 * the search term and find stops nearby.
	 */
	private void searchPlace(String text) {
		if (!isValidSearchText(text)) {
			/* Invalid search - clear the search
			 * bar and return.
			 */
			searchBar.setText("");
			return;
		}

		/* Search and display the place name results */
		Intent findPlaceIntent = new Intent(this, TabbedSearchActivity.class);
		findPlaceIntent.putExtra("firstTab", "searchPlaceResult");
		findPlaceIntent.putExtra("search", String.valueOf(text));
		startActivity(findPlaceIntent);
		
	}

	/*
	 * Produce a string representation of
	 * a bit-masking of the checkbox states.
	 */
	private String writeCriteriaString(){
		String criteria = "";
		
		for(int i=0;i<4;i++){
			switch(i){
			case (0):
				if (streetCheck.isChecked()){
					criteria += "1";
				}
				else {criteria +="0";}
				break;
			case (1):
				if (townCheck.isChecked()){
					criteria += "1";
				}
				else {criteria +="0";}
				break;
			case (2):
				if (descriptionCheck.isChecked()){
					criteria += "1";
				}
				else {criteria +="0";}
				break;
			case (3):
				if (numberCheck.isChecked()){
					criteria += "1";
				}
				else {criteria +="0";}
				break;
			}
		}
		
		return criteria;
	}

	public void locationClicked(View v) {
		Intent mapIntent = new Intent(this, StopMapActivity.class);
		startActivity(mapIntent);
	}

	public void favouritesClicked(View v) {
		Intent favouritesIntent = new Intent(this, FavouritesActivity.class);
		startActivity(favouritesIntent);
	}

	public void nearestClicked(View v) {
		int defaultMaxDistance = 1000;
		Intent nearestStopIntent = new Intent(v.getContext(), NearestStopActivity.class);
		nearestStopIntent.putExtra("maxDistance", "" + defaultMaxDistance);
		startActivity(nearestStopIntent);
	}

}