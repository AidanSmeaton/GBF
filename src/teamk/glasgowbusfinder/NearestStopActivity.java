package teamk.glasgowbusfinder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.londatiga.android.QuickAction;

import teamk.glasgowbusfinder.data.BusStop;
import teamk.glasgowbusfinder.data.DatabaseHelper;
import teamk.glasgowbusfinder.location.LocationServiceChecker;
import teamk.glasgowbusfinder.location.MyLocationListener;
import teamk.glasgowbusfinder.quickactions.BusStopListQuickAction;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author Aidan Smeaton
 */
public class NearestStopActivity extends SearchResultActivity {
	private static final double METRES_TO_DEGREES_APPROX_RATIO = 0.00001; // an overestimated ratio
	private Location loc;
	private LocationManager lm;
	private int maxDistance;
	private double maxDegrees;
	private QuickAction qa;
	private ArrayList<BusStop> results;
	private String place;
	private Location placeLoc;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.neareststopresults);
		
		place = getIntent().getStringExtra("place");
		
		lm = (LocationManager) getSystemService(LOCATION_SERVICE);
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2, 0, MyLocationListener.getInstance());
		
		/* Call the location listener to see if a
		 * GPS fix is available.
		 */
		loc = MyLocationListener.getInstance().getLocation();
		
		/* If no location fix is available, we cannot search
		 * for nearest stop. Inform the user.
		 */
		if (loc == null && place == null) {
			Toast.makeText(this, R.string.cannot_find_location, Toast.LENGTH_LONG).show();
			return;
		}
		
		Location searchByLocation = null;
		if (place != null){
			placeLoc = new Location("place");
			placeLoc.setLatitude(Double.parseDouble(getIntent().getStringExtra("latitude")));
			placeLoc.setLongitude(Double.parseDouble(getIntent().getStringExtra("longitude")));
			searchByLocation = placeLoc;
		}
		else {
			searchByLocation = loc;
		}
				
		double lat = searchByLocation.getLatitude();
		double lon = searchByLocation.getLongitude();
		
		maxDistance = Integer.parseInt(getIntent().getStringExtra("maxDistance"));
		maxDegrees = maxDistance * METRES_TO_DEGREES_APPROX_RATIO;
		
		cursor = db.getBusStopsInBoundary(lat - maxDegrees, lat + maxDegrees, lon - maxDegrees, lon + maxDegrees);
		
		results = arrangeByNearest(this, cursor, searchByLocation, maxDistance);
						
		setListAdapter(new BusStopAdapter(this, R.layout.neareststopresults_row, results));
		
		ListView list = this.getListView();
		
		list.setOnItemClickListener(new OnItemClickListener() {
            @Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            	
            	BusStop stop = (BusStop) view.getTag(R.id.stopfavourite);
             	
             	qa = new BusStopListQuickAction(view, stop, NearestStopActivity.this);
             	qa.show();
            }
		});
		
		TextView resultCount = (TextView) findViewById(R.id.neareststopresults_count);
		
		String extra = "";
		if (place != null){
			extra += " of '" + place + "'";
		}
		
		resultCount.setText("" + results.size() + " " + getString(R.string.results_within) + " " + maxDistance + " " + getString(R.string.metres) + extra);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		lm.removeUpdates(MyLocationListener.getInstance());
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		/* This activity needs GPS enabled; check
		 * if the service is enabled. */
		LocationServiceChecker.getInstance(this).checkForService(LocationServiceChecker.GPS_LOCATION_SERVICE);
		
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2, 0, MyLocationListener.getInstance());
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		try {
			qa.dismiss();
		} catch (Exception e) {
			/* Either the popup was already hidden, or
			 * not even created. Can be safely ignored.
			 */
		}
		
		lm.removeUpdates(MyLocationListener.getInstance());
	}
	
	@Override
	public void updateCursor() {
		super.updateCursor();
		
		// arrange by distance from place
		if (place != null){
			results = arrangeByNearest(this, cursor, placeLoc, maxDistance);
		}
		
		// arrange by distance from user
		else {
			results = arrangeByNearest(this, cursor, loc, maxDistance);
		}
		
		setListAdapter(new BusStopAdapter(this, R.layout.neareststopresults_row, results));
	}
	
	public static ArrayList<BusStop> arrangeByNearest(Context ctx, Cursor cursor, Location loc, int maxDistance) {
		
		ArrayList<BusStop> arrayList = new ArrayList<BusStop>();
		
		for(cursor.moveToFirst(); cursor.moveToNext(); cursor.isAfterLast()) {
		    // The Cursor is now set to the right position
			String stopCode =  cursor.getString(DatabaseHelper.BUS_STOP_CODE);
			
			String name = DatabaseHelper.getInstance(ctx).hasAlias(cursor.getString(DatabaseHelper.BUS_STOP_CODE)) ? 
    						cursor.getString(DatabaseHelper.BUS_STOP_ALIAS) :
    						cursor.getString(DatabaseHelper.BUS_STOP_NAME);
			
			String street = cursor.getString(DatabaseHelper.BUS_STOP_STREET);
			String locality = cursor.getString(DatabaseHelper.BUS_STOP_LOCALITY);
			boolean favourite = cursor.getInt(DatabaseHelper.BUS_STOP_FAVOURITE) == 1;
			float longitude = cursor.getFloat(DatabaseHelper.BUS_STOP_LONGITUDE);
			float latitude = cursor.getFloat(DatabaseHelper.BUS_STOP_LATITUDE);
			
			Location stopLocation = new Location("Stop");
			stopLocation.setLongitude(longitude);
			stopLocation.setLatitude(latitude);
			float distance = loc.distanceTo(stopLocation);
			
			BusStop stop = new BusStop(stopCode, name, street, locality, favourite, distance, latitude, longitude);
			
			if (distance < maxDistance){
				arrayList.add(stop);
			}
		}
		
		Collections.sort(arrayList);
		
		return arrayList;
	}	
	
	/**
	 * Adapter class for an array of BusStop, to display bus stops.
	 * 
	 * @author Aidan Smeaton
	 */
	private class BusStopAdapter extends ArrayAdapter<BusStop> {
		private List<BusStop> stopList;

		public BusStopAdapter(Context context, int layoutId, List<BusStop> stopList) {
			super(context, layoutId, stopList);
			
			this.stopList = stopList;
		}

		@Override
		public int getCount() {
			return stopList.size();
		}

		@Override
		public BusStop getItem(int position) {
			return stopList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup viewGroup) {
			ViewHolder holder;
			
			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(R.layout.neareststopresults_row, null);
				
				holder = new ViewHolder();
				holder.stopname = (TextView) convertView.findViewById(R.id.neareststopresults_stopname);
				holder.stopstreet = (TextView) convertView.findViewById(R.id.neareststopresults_stopstreet);
				holder.distance_you = (TextView) convertView.findViewById(R.id.neareststopresults_distance_you_num);
				holder.distance_place = (TextView) convertView.findViewById(R.id.neareststopresults_distance_place_num);
				holder.star = (ImageView) convertView.findViewById(R.id.neareststopresults_star);
				
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			
			BusStop stop = stopList.get(position);
			
			convertView.setTag(R.id.stopfavourite, stop);
			
			holder.stopname.setText(stop.getName());
			holder.stopstreet.setText(stop.getStreet() + ", " + stop.getLocality());
			
			if (!stop.isFavourite()) {
				holder.star.setVisibility(View.INVISIBLE);
			} else {
				holder.star.setVisibility(View.VISIBLE);
			}
			
			if (place != null) {
				Location stopLocation = new Location("Stop");
				stopLocation.setLongitude(stop.getLongitude());
				stopLocation.setLatitude(stop.getLatitude());
				
				float distance_you = loc.distanceTo(stopLocation);
				holder.distance_you.setText(((int) (distance_you / 10) * 10)
						+ " " + getString(R.string.metres) + " from ");
				
				float distance_place = placeLoc.distanceTo(stopLocation);
				holder.distance_place.setText(((int) (distance_place / 10) * 10)
						+ " " + getString(R.string.metres) + " from ");
			} else {
				holder.distance_you.setText(((int) (stop.getDistance() / 10) * 10)
						+ " " + getString(R.string.metres) + " from ");
				
				// hide 'distance from place', not applicable
				TextView tv = (TextView) convertView.findViewById(R.id.neareststopresults_distance_place_num);
				tv.setVisibility(-1);
				
			}
			
			return convertView;
		}
	}
	
	/*
	 * This class holds views for fast recycling of list
	 * views. This pattern is discussed by Romain Guy
	 * of Google about 11 minutes into this video:
	 * http://www.youtube.com/watch?v=N6YdwzAvwOA
	 */
	private static class ViewHolder {
		TextView stopname;
		TextView stopstreet;
		TextView distance_you;
		TextView distance_place;
		ImageView star;
	}
}