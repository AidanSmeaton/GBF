package teamk.glasgowbusfinder;

import java.net.URL;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import teamk.glasgowbusfinder.data.Departure;
import teamk.glasgowbusfinder.data.NextBusesParser;
import teamk.glasgowbusfinder.quickactions.BusServiceQuickAction;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

/**
 * This activity displays a list of departures.
 * 
 * @author Euan Freeman
 */
public class DeparturesActivity extends CursorListActivity implements OnItemClickListener {
	private enum ActivityState {PAUSED, ACTIVE, DESTROYED};
	private ActivityState state;
	
	/* Identifier for the dialog. */
	private static final int PROGRESS_DIALOG_ID = 1;
	private DepartureAdapter adapter;
	private ProgressDialog downloadProgressDialog;
	private DownloadDeparturesTask downloadTask;
	private List<Departure> departures;
	private NextBusesParser parser;
	private String stopCode;
	private boolean downloaded;
	private SharedPreferences sp;
	private boolean showTimeUntil;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.departures);
		
		parser = new NextBusesParser();
		downloaded = false;
		
		/* Get the stopcode bundled with the intent to this activity. */
		stopCode = getIntent().getStringExtra("stopcode");
		
		cursor = db.getBusStop(stopCode);
		cursor.moveToFirst();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		state = ActivityState.ACTIVE;
		
		sp = PreferenceManager.getDefaultSharedPreferences(this);
		showTimeUntil = sp.getBoolean(getString(R.string.pref_departureTimeUntilArrival), false);
		
		/* Refresh the cursor. */
		if (cursor != null) {
			cursor.moveToFirst();
		}
		
		/* Get the departures from NextBuses.mobi */
		if (!downloaded) {
			downloadTask = new DownloadDeparturesTask();
			downloadTask.execute(stopCode);
		}
		
		if (adapter != null) {
			/* Force the list to update - in case the time display
			 * type was changed in the preferences.
			 */
			adapter.notifyDataSetChanged();
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		state = ActivityState.PAUSED;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		state = ActivityState.DESTROYED;
		
		try {
			removeDialog(PROGRESS_DIALOG_ID);
			
			if (downloadTask.getStatus() != AsyncTask.Status.FINISHED) {
				downloadTask.cancel(true);
			}
		} catch (Exception e) {
			/* The dialog is already hidden. This exception
			 * can be ignored.
			 */
		}
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		
		TextView serviceTextView = (TextView) view.findViewById(R.id.departuresrow_textview_service);
		String serviceId = serviceTextView.getText().toString().trim();
		
		final boolean serviceExists = db.serviceExists(serviceId);
		
		//TextView timeTextView = (TextView) view.findViewById(R.id.departuresrow_textview_time);
		//String time = timeTextView.getText().toString().trim();
		String time = ((ViewHolder) view.getTag()).originalTime;
		
		TextView destinationTextView = (TextView) view.findViewById(R.id.departuresrow_textview_destination);
		String destination = destinationTextView.getText().toString().trim();
		
		qa = new BusServiceQuickAction(view, this, db, stopCode, serviceId, serviceExists, destination, time);
    	qa.show();
    	
    }
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case PROGRESS_DIALOG_ID:
			downloadProgressDialog = new ProgressDialog(this);
			downloadProgressDialog.setIndeterminate(true);
			downloadProgressDialog.setTitle("");
			downloadProgressDialog.setMessage(getString(R.string.downloading_departures_message));
			
			return downloadProgressDialog;
		default:
			return null;
		}
	}
	
	public void refresh() {
		downloadTask = new DownloadDeparturesTask();
        downloadTask.execute(stopCode);
	}
	
	/**
	 * Asks the adapter to update itself.
	 */
	public void invalidateAdapter() {
		if (adapter != null)
			adapter.notifyDataSetChanged();
	}
	
	public void departuresRefreshClick(View v) {
		refresh();
	}
	
	/**
	 * Adapter class for a list of BusStop objects. Inflates a new row
	 * layout then displays the appropriate information.
	 * 
	 * @author Euan Freeman
	 */
	private class DepartureAdapter extends ArrayAdapter<Departure> {
		/* Update interval ~30seconds */
		private final static long updateInterval = 30000;
		
		private List<Departure> departures;
		
		public DepartureAdapter(Context context, int textViewResourceId, List<Departure> departures) {
			super(context, textViewResourceId, departures);
			this.departures = departures;
			
			/* Create a Handler to queue a Runnable to run
			 * on the thread which creates AlertAdapter, i.e.
			 * the main UI thread. This means we can update the
			 * UI periodically.
			 */
			final Handler handler = new Handler();
			
			/* Post a delayed message after 'updateInterval'
			 * which tells the adapter to update itself. This causes this rows
			 * to refresh, hence updating the time remaining text.
			 */
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					/* If state is ACTIVE, the departures are on
					 * screen. In this state, we want to update.
					 */
					if (state == ActivityState.ACTIVE) {
						/* If showing time until a bus is due, update */
						if (sp.getBoolean(DeparturesActivity.this.getString(R.string.pref_departureTimeUntilArrival), false)) {
							adapter.notifyDataSetChanged();
						}
					/* If state is DESTROYED, the activity has
					 * been popped from the Activity Stack
					 * and there is no need to update any more.
					 */
					} else if (state == ActivityState.DESTROYED) {
						return;
					}
					
					/* Recursively call this Runnable again to keep
					 * the update cycle going.
					 */
					handler.postDelayed(this, updateInterval);
				}
			}, updateInterval);
		}
		
		@Override
		public int getCount() {
			return departures.size();
		}

		@Override
		public Departure getItem(int position) {
			return departures.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			
			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(R.layout.departuresrow, null);
				
				holder = new ViewHolder();
				holder.serviceTextView = (TextView) convertView.findViewById(R.id.departuresrow_textview_service);
				holder.destinationTextView = (TextView) convertView.findViewById(R.id.departuresrow_textview_destination);
				holder.timeTextView = (TextView) convertView.findViewById(R.id.departuresrow_textview_time);
				holder.alertImageView = (ImageView) convertView.findViewById(R.id.departuresrow_alert);
				holder.timeString = "";
				
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			
			Departure departure = departures.get(position);
			
			convertView.setTag(R.id.departure, departure);
			
			holder.serviceTextView.setText(String.format("%-4s", departure.getService()));
			holder.destinationTextView.setText(departure.getDestination());
			holder.alertImageView.setVisibility(-1);
			
			/* We store the original time so that the QA popup
			 * can show this, even if the activity is set up
			 * to show time remaining.
			 */
			holder.originalTime = departure.getTime();
			
			if (showTimeUntil) {
				/* Show time until arrival */
				String time = departure.getTime();
				
				GregorianCalendar depTime = new GregorianCalendar();
				depTime.set(GregorianCalendar.HOUR_OF_DAY, Integer.parseInt(time.substring(0, 2)));
				depTime.set(GregorianCalendar.MINUTE, Integer.parseInt(time.substring(3, 5)));
				
				/* This time is of format: HH:mm (DAY), meaning it is
				 * the next day. Add 1 day.
				 */
				if (time.length() > 5) {
					depTime.add(GregorianCalendar.DATE, 1);
				}
				
				long depTimeMs = depTime.getTimeInMillis();
				
				long timeUntil = depTimeMs - System.currentTimeMillis();
				
				if (timeUntil <= -60000) {
					holder.timeString = String.format(" %d mins ago", -(timeUntil / 60000));
				} else if (timeUntil <= 0) {
					holder.timeString = String.format(" now");
				} else if (timeUntil < 60000) {
					holder.timeString = String.format(" in %d min", timeUntil / 60000);
				} else if (timeUntil < 3600000) {
					holder.timeString = String.format(" in %d mins", timeUntil / 60000);
				} else if (timeUntil < 6400000) {
					holder.timeString = String.format(" over %d hr", timeUntil / 3600000);
				} else {
					holder.timeString = String.format(" over %d hrs", timeUntil / 3600000);
				}
			} else {
				/* Show time of arrival */
				holder.timeString = departure.getTime();
			}
			
			holder.timeTextView.setText(holder.timeString);
			
			if (db.hasAlert(departure,stopCode)){
				holder.alertImageView.setVisibility(1);
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
		TextView serviceTextView;
		TextView destinationTextView;
		TextView timeTextView;
		ImageView alertImageView;
		String timeString;
		String originalTime;
	}
	
	private class DownloadDeparturesTask extends AsyncTask<String, Integer, ArrayList<Departure>> {
		@Override
		protected void onPreExecute() {
			/* Show the progress dialog. */
			showDialog(PROGRESS_DIALOG_ID);
			
			parser.prepare();
		}
		
		@Override
		protected ArrayList<Departure> doInBackground(String... stopCodes) {
			try {
				SAXParserFactory spf = SAXParserFactory.newInstance();
				SAXParser sp = spf.newSAXParser();
				XMLReader xr = sp.getXMLReader();

				URL sourceUrl = new URL("http://nextbuses.mobi/departureboard?stopCode=" + stopCodes[0]);
				xr.setContentHandler(parser);
				xr.parse(new InputSource(sourceUrl.openStream()));
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return parser.getDepartures();
		}
		
		@Override
		protected void onPostExecute(ArrayList<Departure> result) {
			if (! this.isCancelled()) {
				/* If the departures list has already been created,
				 * just clear and add to this. Otherwise, set
				 * departures as the result of this download.
				 */
				if (departures != null) {
					departures.clear();
					departures.addAll(result);
					
					adapter.notifyDataSetChanged();
				} else {
					departures = result;
					
					adapter = new DepartureAdapter(DeparturesActivity.this, R.layout.departuresrow, departures);
					
					setListAdapter(adapter);
					
					ListView list = getListView();
					
					list.setOnItemClickListener(DeparturesActivity.this);
				}
				
				downloaded = true;
			}
			
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
