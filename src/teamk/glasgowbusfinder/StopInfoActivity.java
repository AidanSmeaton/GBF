package teamk.glasgowbusfinder;

import java.util.ArrayList;
import java.util.List;

import teamk.glasgowbusfinder.data.DatabaseHelper;
import teamk.glasgowbusfinder.data.StopInformationPair;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Displays bus stop information, including
 * location information, and a list of services
 * which call at this stop.
 * 
 * @author Aidan Smeaton
 * @author Euan Freeman
 */
public class StopInfoActivity extends Activity {
	private ListView list;
	private SeparatedListAdapter adapter;
	private ServiceAdapter serviceAdapter;
	private InfoAdapter infoAdapter;
	private ArrayList<StopInformationPair> info;
	private Cursor stopCursor;
	private Cursor serviceCursor;
	private String stopCode;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/* Fetch stop code from intent. */
		stopCode = getIntent().getStringExtra("stopcode");

		/* Prepare database cursors. */
		serviceCursor = DatabaseHelper.getInstance(this.getApplicationContext()).getBusStopServices(stopCode);
		serviceCursor.moveToFirst();

		stopCursor = DatabaseHelper.getInstance(this.getApplicationContext()).getBusStop(stopCode);
		stopCursor.moveToFirst();

		setContentView(R.layout.stopinfo);
		
		/* Set up the list view. */
		list = (ListView) findViewById(R.id.stopinfo_list);

		/* If an alias for this stop is available, use it */
		String stopName = DatabaseHelper.getInstance(this).getAliasOrName(stopCode);
		
		/* Set up the list of stop information. */
		info = new ArrayList<StopInformationPair>();
		
		String landmark = stopCursor.getString(DatabaseHelper.BUS_STOP_LANDMARK);
		/* If no "Landmark" is known, append the indicator to the regular stop name */
		String description = stopCursor.getString(DatabaseHelper.BUS_STOP_INDICATOR) + " " +
			((landmark == null || landmark.length() == 0) ? stopCursor.getString(DatabaseHelper.BUS_STOP_NAME) : landmark);
		
		info.add(new StopInformationPair(getString(R.string.stop_name_title), stopName));
		info.add(new StopInformationPair(getString(R.string.stop_code_title), stopCursor.getString(DatabaseHelper.BUS_STOP_CODE)));
		info.add(new StopInformationPair(getString(R.string.description), description));
		info.add(new StopInformationPair(getString(R.string.street), stopCursor.getString(DatabaseHelper.BUS_STOP_STREET)));
		info.add(new StopInformationPair(getString(R.string.locality), stopCursor.getString(DatabaseHelper.BUS_STOP_LOCALITY)));
		// info.add(new StopInformationPair(getString(R.string.direction), stopCursor.getString(DatabaseHelper.BUS_STOP_DIRECTION)));		

		/* Create the separated list adapter. */
		adapter = new SeparatedListAdapter(this);

		serviceAdapter = new ServiceAdapter(this, serviceCursor);
		infoAdapter = new InfoAdapter(this, R.layout.stopinfo_info_row, info);
		
		/* Set up item click listener. */
		list.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
				String type = (String) view.getTag(R.id.type);
				
				if (type != null && type.equals("service")) {
					TextView tv = (TextView) view.findViewById(R.id.stopinfo_service);
                    String routeNumber = tv.getText().toString();
                    
                    Intent routeIntent = new Intent(StopInfoActivity.this, StopMapActivity.class);
                    routeIntent.putExtra("stopcode", stopCode);
                    routeIntent.putExtra("route", routeNumber);
                    StopInfoActivity.this.startActivity(routeIntent);
				}
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();

		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		
		/* Check the shared preferences to determine which order
		 * to display stop information in.
		 */
		adapter.clear();
		
		/* Services on top */
		if (sp.getBoolean(getString(R.string.pref_stopInfoServicesOnTop), false)) {
			adapter.addSection(getString(R.string.stopinfo_services), serviceAdapter);
			adapter.addSection(getString(R.string.stopinfo_general), infoAdapter);
		/* Stop info on top */
		} else {
			adapter.addSection(getString(R.string.stopinfo_general), infoAdapter);
			adapter.addSection(getString(R.string.stopinfo_services), serviceAdapter);
		}
		
		list.setAdapter(adapter);
		
		/* Refresh the cursor. */
		if (stopCursor != null) {
			stopCursor.requery();

			stopCursor.moveToFirst();

			/* Set up the favourite stop toggle button. */
			//CheckBox favouriteButton = (CheckBox) findViewById(R.id.stopinfo_favourite);
			//favouriteButton.setChecked(stopCursor.getInt(DatabaseHelper.BUS_STOP_FAVOURITE) == 1);
		}

		if (serviceCursor != null)
			serviceCursor.requery();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		/* Free up all resources associated
		 * with this cursor.
		 */
		if (stopCursor != null)
			stopCursor.close();

		if (serviceCursor != null)
			serviceCursor.close();
	}

	public void seeRouteClicked(View view) {
		ViewParent parentView = view.getParent();

		View serviceView = ((View) parentView).findViewById(R.id.stopinfo_service);
		TextView serviceTextView = (TextView)serviceView;
		CharSequence text = serviceTextView.getText();
		String textString = text.toString();
		
		Intent mapIntent = new Intent(this, StopMapActivity.class);
		mapIntent.putExtra("stopcode", stopCode);
		mapIntent.putExtra("route", textString);
		this.startActivity(mapIntent);
	}

	private class InfoAdapter extends ArrayAdapter<StopInformationPair> {
		private List<StopInformationPair> infoList;

		public InfoAdapter(Context context, int layoutId, List<StopInformationPair> infoList) {
			super(context, layoutId, infoList);
			
			this.infoList = infoList;
		}

		@Override
		public int getCount() {
			return infoList.size();
		}

		@Override
		public StopInformationPair getItem(int position) {
			return infoList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup viewGroup) {
			InfoViewHolder holder;
			
			StopInformationPair pair = infoList.get(position);
			
			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(R.layout.stopinfo_info_row, null);
				
				holder = new InfoViewHolder();
				holder.title = (TextView) convertView.findViewById(R.id.stopinfo_title);
				holder.value = (TextView) convertView.findViewById(R.id.stopinfo_entry);
				
				convertView.setTag(holder);
			} else {
				holder = (InfoViewHolder) convertView.getTag();
			}
			
			holder.title.setText(pair.getTitle());
			holder.value.setText(pair.getEntry());

			return convertView;
		}
	}

	/*
	 * This class holds views for fast recycling of list
	 * views. This pattern is discussed by Romain Guy
	 * of Google about 11 minutes into this video:
	 * http://www.youtube.com/watch?v=N6YdwzAvwOA
	 */
	private static class InfoViewHolder {
		TextView title;
		TextView value;
	}
	
	private class ServiceAdapter extends CursorAdapter {
		private LayoutInflater inflater;

		public ServiceAdapter(Context context, Cursor cursor) {
			super(context, cursor);

			inflater = LayoutInflater.from(context);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			TextView serviceID = (TextView) view.findViewById(R.id.stopinfo_service);
			serviceID.setText(cursor.getString(DatabaseHelper.STOPSERVICES_SERVICE_ID));
			
			view.setTag(R.id.type, "service");
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return inflater.inflate(R.layout.stopinfo_service_row, null);
		}
	}
}