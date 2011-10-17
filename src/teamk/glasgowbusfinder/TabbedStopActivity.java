package teamk.glasgowbusfinder;

import teamk.glasgowbusfinder.data.DatabaseHelper;
import teamk.glasgowbusfinder.ui.preferences.StopInfoPreferencesActivity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.Toast;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;

/**
 * This activity presents Departures and
 * Stop Information in a tabbed layout.
 * 
 * @author Aidan Smeaton
 * @author Euan Freeman
 * 
 * @author Josh Clemm
 * @see http://joshclemm.com/blog/?p=136
 * @see http://code.google.com/p/android-custom-tabs/
 */
public class TabbedStopActivity extends TabActivity {
	/* Constants which define tab indices. Other
	 * activities can add one of these indices
	 * to the Intent to specify which tab to
	 * start at.
	 */
	public static final int DEPARTURES = 0;
	public static final int INFORMATION = 1;
	
	private TabHost tabHost;
	private Cursor cursor;
	private String stopCode;
	private DatabaseHelper db;
	
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    setContentView(R.layout.tabbed_stop);
	    
	    stopCode = getIntent().getStringExtra("stopcode");
	    assert(stopCode != null);
	    
	    int startTab = getIntent().getIntExtra("startTab", 0);
	    
	    db = DatabaseHelper.getInstance(this);
	    cursor = db.getBusStop(stopCode);
		cursor.moveToFirst();
		
	    setupHeader();
	    
	    setupTabHost();

	    // Create an Intent to launch an Activity for the tab (to be reused)
	    Intent intent = new Intent().setClass(this, DeparturesActivity.class);
	    intent.putExtra("stopcode", stopCode);

	    setupTab("Departures", intent);
	    
	    // Do the same for the other tabs
	    intent = new Intent().setClass(this, StopInfoActivity.class);
	    intent.putExtra("stopcode", stopCode);
	    
	    setupTab("Information", intent);

	    tabHost.setCurrentTab(startTab);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		if (cursor != null) {
			cursor.requery();
			cursor.moveToFirst();
		}
		
		setupHeader();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.busstopmenu, menu);

		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		
		MenuItem toggleFavourites = menu.findItem(R.id.busstopmenu_favourite);
		
		if (toggleFavourites != null)
			toggleFavourites.setTitle((isStopFavourite() ? "Remove" : "Add") + " Favourite");
		
		MenuItem stopAlias = menu.findItem(R.id.busstopmenu_alias);
		
		if (stopAlias != null)
			stopAlias.setTitle((hasAlias() ? "Edit" : "Add") + " Nickname");
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.busstopmenu_map:
			Intent mapIntent = new Intent(this, StopMapActivity.class);
			mapIntent.putExtra("stopcode", stopCode);
			this.startActivity(mapIntent);
			return true;
		case R.id.busstopmenu_favourite:
			favouritesClicked(findViewById(R.id.busstop_imageview_favourite));
			return true;
		case R.id.busstopmenu_alias:
			aliasClicked();
			return true;
		case R.id.busstopmenu_information:
			infoClicked();
			return true;
		case R.id.busstopmenu_preferences:
			Intent intent = new Intent(this, StopInfoPreferencesActivity.class);
			this.startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	/* Determines if the current stop is a favourite stop. */
	private boolean isStopFavourite() {
		/* Force a move to first position, in case
		 * a re-query occurred (this is likely).
		 */
		cursor.moveToFirst();
		
		return cursor.getInt(DatabaseHelper.BUS_STOP_FAVOURITE) == 1;
	}
	
	/* Determines if the current stop has an alias. */
	private boolean hasAlias() {
		/* Force a move to first position, in case
		 * a re-query occurred (this is likely).
		 */
		cursor.moveToFirst();
		
		return cursor.getString(DatabaseHelper.BUS_STOP_ALIAS) != null && cursor.getString(DatabaseHelper.BUS_STOP_ALIAS).length() > 0;
	}
	
	private void aliasClicked() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle(R.string.edit_alias);
		alert.setMessage(R.string.enter_alias);

		final EditText alias = new EditText(this);
		
		String knownAlias = DatabaseHelper.getInstance(this).getAlias(stopCode);
		
		if (knownAlias != null) {
			alias.setText(knownAlias);
		}
		
		alert.setView(alias);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String newAlias = alias.getText().toString();

				if (newAlias.length() > DatabaseHelper.MAX_ALIAS_LENGTH) {
					newAlias = newAlias.substring(0, DatabaseHelper.MAX_ALIAS_LENGTH);
				}
				
				DatabaseHelper.getInstance(TabbedStopActivity.this).updateAlias(stopCode, newAlias);
				
				cursor.requery();
				
				setupHeader();
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Ignore
			}
		});

		alert.show();
	}
	
	private void infoClicked() {
		final Dialog dialog = new Dialog(this);
		dialog.setContentView(R.layout.busstop_info);
		dialog.setTitle(R.string.information);
		dialog.setCancelable(true);
		
		Button closeButton = (Button) dialog.findViewById(R.id.busstop_info_close);
		
		closeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
		
		TextView linkText = (TextView) dialog.findViewById(R.id.departures_attribution);
		
		linkText.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				/* Open the web page of the departure source */
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(getString(R.string.departures_source_url)));
				startActivity(i);
			}
		});
		
		dialog.show();
	}
	
	/**
	 * Toggles the favourite field of the
	 * selected bus stop.
	 * @param v View which triggered this callback.
	 */
	public void favouritesClicked(View v) {
		CheckBox favouritesButton;
		
		try {
			favouritesButton = (CheckBox) v;
		} catch (Exception e ) { return; }
		
		boolean isFavourite = isStopFavourite();
		
		/* Toggle checkbox state. */
		favouritesButton.setChecked(!isFavourite);
		
		/* Update database with toggled state. */
		if (db.updateFavourite(cursor.getString(DatabaseHelper.BUS_STOP_CODE), !isFavourite)) {
			/* Update the cursor so that future
			 * toggles aren't working off stale data.
			 */
			cursor.requery();
		}
		
		if (!isFavourite){
			Toast.makeText(this, "Added to favourites", Toast.LENGTH_SHORT).show();
		}
		else {
			Toast.makeText(this, "Removed from favourites", Toast.LENGTH_SHORT).show();
		}
		
		
	}
	
	private void setupHeader() {
		cursor.moveToFirst();
		
		/* Set up the favourite stop toggle button. */
		CheckBox favouriteButton = (CheckBox) findViewById(R.id.busstop_imageview_favourite);
		favouriteButton.setChecked(cursor.getInt(DatabaseHelper.BUS_STOP_FAVOURITE) == 1);
		
		/* Set the stop name in the header. */
		TextView stopName = (TextView) findViewById(R.id.busstop_textview_stopname);
		stopName.setText(DatabaseHelper.getInstance(this).getAliasOrName(stopCode));
	}
	
	/**
	 * @author Josh Clemm
	 * @see http://code.google.com/p/android-custom-tabs/
	 * @see http://joshclemm.com/blog/?p=136
	 */
	private void setupTab(final String tag, Intent intent) {
		View tabview = createTabView(tabHost.getContext(), tag);

		TabSpec setContent = tabHost.newTabSpec(tag).setIndicator(tabview).setContent(intent);
		
		tabHost.addTab(setContent);
	}

	/**
	 * @author Josh Clemm
	 * @see http://code.google.com/p/android-custom-tabs/
	 * @see http://joshclemm.com/blog/?p=136
	 */
	private void setupTabHost() {
		tabHost = (TabHost) findViewById(android.R.id.tabhost);
		tabHost.setup();
	}
	
	/**
	 * @author Josh Clemm
	 * @see http://code.google.com/p/android-custom-tabs/
	 * @see http://joshclemm.com/blog/?p=136
	 */
	private static View createTabView(final Context context, final String text) {
		View view = LayoutInflater.from(context).inflate(R.layout.tabs_bg, null);
		
		TextView tv = (TextView) view.findViewById(R.id.tabsText);
		tv.setText(text);
		
		return view;
	}
}
