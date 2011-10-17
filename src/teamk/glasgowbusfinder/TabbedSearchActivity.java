package teamk.glasgowbusfinder;

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.TabHost.TabSpec;

/**
 * This activity presents Search Stop and
 * Nearest Stops in a tabbed layout.
 * 
 * @author Aidan Smeaton
 * @author Euan Freeman
 * 
 * @author Josh Clemm
 * @see http://joshclemm.com/blog/?p=136
 * @see http://code.google.com/p/android-custom-tabs/
 */
public class TabbedSearchActivity extends TabActivity {
	/* Constants which define tab indices. Other
	 * activities can add one of these indices
	 * to the Intent to specify which tab to
	 * start at.
	 */
	public static final int SEARCH = 0;
	public static final int NEAREST = 1;
	public static final int DEFAULT_MAX_DISTANCE = 1000;
	
	private TabHost tabHost;
	
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    setContentView(R.layout.tabbed_search);
	    String firstTab = getIntent().getStringExtra("firstTab");
	    
	    // Create an Intent to launch an Activity for the tab (to be reused)
	    Intent intent = new Intent();
	    
	    setupTabHost();
	    
	    if (firstTab.equals("searchStopResult")){
	    	
	    	String search = getIntent().getStringExtra("search");
	    	String criteria = getIntent().getStringExtra("criteria");
		    	    	
	    	intent = new Intent(this, FindStopResultsActivity.class);
	    	intent.putExtra("search", search);
	    	intent.putExtra("criteria", criteria);
	    	
	    	setupTab("Search", intent);
	    }
	    
	    else if (firstTab.equals("searchPlaceResult")){
	    	
	    	String text = getIntent().getStringExtra("search");
		    	    	
	    	/* Search and display the place name results */
			intent = new Intent(this, FindPlaceResultsActivity.class);
			intent.putExtra("search", text);
	    	
	    	setupTab("Search", intent);
	    }
	    
	    else if (firstTab.equals("searchPlaceResult2")){
	    	
	    	String search = getIntent().getStringExtra("search");
	    	String maxDistance = getIntent().getStringExtra("maxDistance");
	    	String lat = getIntent().getStringExtra("latitude");
	    	String lon = getIntent().getStringExtra("longitude");
	    	String place = getIntent().getStringExtra("place");
	    			    	    	
	    	/* Search and display the place name results */
			intent = new Intent(this, NearestStopActivity.class);
			intent.putExtra("search", search);
			intent.putExtra("maxDistance", maxDistance);
			intent.putExtra("latitude", lat);
			intent.putExtra("longitude", lon);
			intent.putExtra("place", place);
	    	
	    	setupTab("Search", intent);
	    }
	    
	    else {
	    	
	    	intent = new Intent().setClass(this, FindStopActivity.class);
	    	setupTab("Search", intent);
		           	
	    }
	    
	    int startTab = SEARCH;
	    	      
	    
	    // Do the same for the other tabs
	    intent = new Intent().setClass(this, NearestStopActivity.class);
	    intent.putExtra("maxDistance", "" + DEFAULT_MAX_DISTANCE);
	    
	    setupTab("Nearest", intent);

	    tabHost.setCurrentTab(startTab);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
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
