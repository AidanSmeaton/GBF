package teamk.glasgowbusfinder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * @author Aidan Smeaton
 * @author Calum McCall
 * @author Euan Freeman
 */
public class FindPlaceResultsActivity extends SearchResultActivity {
	/* Identifier for the dialog. */
	private static final int PROGRESS_DIALOG_ID = 1;
	
	private ProgressDialog geocodingProgressDialog;
	private GeocodingTask geocodingTask;
	private String search;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.findstopresults);
		
		search = getIntent().getStringExtra("search");
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		if (!isSearched()) {
			updateCursor();
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case PROGRESS_DIALOG_ID:
			geocodingProgressDialog = new ProgressDialog(this);
			geocodingProgressDialog.setIndeterminate(true);
			geocodingProgressDialog.setTitle("");
			geocodingProgressDialog.setMessage(getString(R.string.doing_geocoding_message));
			
			return geocodingProgressDialog;
		default:
			return null;
		}
	}
	
	@Override
	public void updateCursor() {
		geocodingTask = new GeocodingTask();
		geocodingTask.execute(search);
	}
	
	/**
	 * Adapter class for a list of Address objects.
	 * 
	 * @author Aidan Smeaton
	 */
	private class PlaceAdapter extends ArrayAdapter<Address> {
		private List<Address> listOfPlaces;

		public PlaceAdapter(Context context, int layoutId, List<Address> listOfPlaces) {
			super(context, layoutId);
			
			this.listOfPlaces = listOfPlaces;
		}

		@Override
		public int getCount() {
			return listOfPlaces.size();
		}

		@Override
		public Address getItem(int position) {
			return listOfPlaces.get(position);
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
				convertView = inflater.inflate(R.layout.findplaceresults_row, null);
				
				holder = new ViewHolder();
				holder.one = (TextView) convertView.findViewById(R.id.place_address0);
				holder.two = (TextView) convertView.findViewById(R.id.place_address1);
				holder.three = (TextView) convertView.findViewById(R.id.place_address2);
				
				convertView.setTag(holder);
			
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			
			Address place = listOfPlaces.get(position);
			
			String one = place.getAddressLine(0);
			
			if (one != null) {
				holder.one.setText(one);
				holder.one.setVisibility(View.VISIBLE);
			} else {
				holder.one.setVisibility(View.INVISIBLE);
			}
			
			String two = place.getAddressLine(1);
			
			if (two != null) {
				holder.two.setText(two);
				holder.two.setVisibility(View.VISIBLE);
			} else {
				holder.two.setVisibility(View.INVISIBLE);
			}
			
			String three = place.getAddressLine(2);
			
			if (three != null) {
				holder.three.setText(three);
				holder.three.setVisibility(View.VISIBLE);
			} else {
				holder.three.setVisibility(View.INVISIBLE);
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
		TextView one;
		TextView two;
		TextView three;
	}
	
	private class GeocodingTask extends AsyncTask<String, Integer, List<Address>> {
		@Override
		protected void onPreExecute() {
			/* Show the progress dialog. */
			showDialog(PROGRESS_DIALOG_ID);
		}
		
		@Override
		protected List<Address> doInBackground(String... places) {
			Geocoder geocoder = new Geocoder(FindPlaceResultsActivity.this, Locale.getDefault());
			
			List<Address> results = null;
			
			try {
				/*
				 * Sometimes geocoding unexpectedly fails. Make at most
				 * 3 attempts to geocode the place name.
				 */
				for (int i = 0; results == null || i < 3; i++){
					results = geocoder.getFromLocationName(search, FindStopActivity.MAX_GEOCODE_RESULTS,
							FindStopActivity.BOUNDARY_LOWER_LEFT_LATITUDE, FindStopActivity.BOUNDARY_LOWER_LEFT_LONGITUDE,
							FindStopActivity.BOUNDARY_UPPER_RIGHT_LATITUDE, FindStopActivity.BOUNDARY_UPPER_RIGHT_LONGITUDE);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			/* If 'results' is null, GeoCoding failed. In order for
			 * the ListView in the activity to display the "no results"
			 * message, it still needs a list, even if it is empty.
			 */
			return results == null ? new ArrayList<Address>(1) : results;
		}
		
		@Override
		protected void onPostExecute(final List<Address> results) {
			/* Set up the list view. */
			ListView list = FindPlaceResultsActivity.this.getListView();
			list.setAdapter(new PlaceAdapter(FindPlaceResultsActivity.this, R.layout.findplaceresults_row, results));
			
			/* Register each item to find stops near the chosen place on click. */
			list.setOnItemClickListener(new OnItemClickListener() {
	            @Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	            	TextView tv = (TextView) view.findViewById(R.id.place_address0);
	            	CharSequence placeNameCS = tv.getText();
	            	String placeNameString = placeNameCS.toString();
	            	
	            	Intent nearestStopIntent = new Intent(FindPlaceResultsActivity.this, TabbedSearchActivity.class);
	            	nearestStopIntent.putExtra("firstTab", "searchPlaceResult2");
	        		nearestStopIntent.putExtra("maxDistance", "" + FindStopActivity.DEFAULT_MAX_DISTANCE);
					nearestStopIntent.putExtra("latitude", "" + results.get(position).getLatitude());
					nearestStopIntent.putExtra("longitude", "" + results.get(position).getLongitude());
					nearestStopIntent.putExtra("place", placeNameString);
					startActivity(nearestStopIntent);
	            }
			});
			
			setSearched(true);
			
			try {
				/* Dismiss the progress dialog. */
				dismissDialog(PROGRESS_DIALOG_ID);
			} catch (Exception e) {
				/* It's possible that this dialog
				 * has disappeared (i.e. interrupted
				 * by device rotation) before this task
				 * completed. This situation can be ignored
				 * because the download finished.
				 */
			}
		}
	}
}
