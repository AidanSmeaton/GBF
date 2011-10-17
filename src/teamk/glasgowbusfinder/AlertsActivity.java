package teamk.glasgowbusfinder;

import java.util.Date;

import teamk.glasgowbusfinder.alerts.BusDueAlert;
import teamk.glasgowbusfinder.data.BusStop;
import teamk.glasgowbusfinder.data.DatabaseHelper;
import teamk.glasgowbusfinder.quickactions.AlertQuickAction;
import teamk.glasgowbusfinder.ui.preferences.AlertsPreferencesActivity;
import teamk.glasgowbusfinder.util.TimeFormatter;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class AlertsActivity extends CursorListActivity {
	private enum AlertsState {PAUSED, ACTIVE, DESTROYED};
	
	private AlertsState state;
	private SharedPreferences sp;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.alerts);
		
		cursor = db.getAlerts();
		
		setListAdapter(new AlertAdapter(this, cursor));
		
		ListView list = this.getListView();
		
		list.setOnItemClickListener(new OnItemClickListener() {
            @Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            	BusStop stop = new BusStop((String) view.getTag(R.id.stopcode),getBaseContext());
            	
            	qa = new AlertQuickAction(view, stop, AlertsActivity.this);
            	qa.show();
            }
		});
		
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		state = AlertsState.ACTIVE;
		
		sp = PreferenceManager.getDefaultSharedPreferences(this);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		state = AlertsState.PAUSED;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		state = AlertsState.DESTROYED;
	}
	
	/**
	 * Removes expired events and updates the
	 * database cursor. This causes the AlertAdapter
	 * to refresh itself.
	 */
	@Override
	public void updateCursor() {
		/* If alerts are set to auto-remove,
		 * check if any are needing removal.
		 */
		if (sp.getBoolean(getString(R.string.pref_alertsAutoRemove), true)) {
			db.removeExpiredEvents(Integer.valueOf(sp.getString(getString(R.string.pref_alertsAutoRemoveTime), "10")));
		}
		
		super.updateCursor();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.alertsmenu, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.alertsmenu_preferences:
			Intent intent = new Intent(this, AlertsPreferencesActivity.class);
			this.startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	/**
	 * Adds a new alert using the default number of minutes for notifications.
	 */
	public static void addAlert(View v, String serviceId, String destination, String stopCode, String time){
		
		/* Get the shared preferences - so we can get
		 * the amount of time before a bus is due to
		 * send the alert.
		 */
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(v.getContext());
		
		/* Get minutes, then convert to ms */
		int minutesBefore = Integer.parseInt((sp.getString(v.getContext().getString(R.string.pref_alertsBeforeTime), "3"))) * 60000;
		
		addAlert(v, serviceId, destination, stopCode, time, minutesBefore);
		
	}
	
	/**
	 * Adds a new alert using a specified number of minutes for notifications.
	 */
	public static void addAlert(View v, String serviceId, String destination, String stopCode, String time, long minutesBefore){
		
		/* Get index from shared preferences */
        Resources res = v.getContext().getResources();
        SharedPreferences sp = v.getContext().getApplicationContext().getSharedPreferences(res.getString(R.string.sharedPrefsName), Context.MODE_PRIVATE);
        int index = sp.getInt(res.getString(R.string.pref_alertIndex), 0);
		
        /* Add alert to database */
		DatabaseHelper.getInstance(v.getContext()).addAlert(index, serviceId, destination, stopCode, time, minutesBefore / 60000);
		
		/* Create the alarm for this bus alert */
		AlarmManager alarmManager = (AlarmManager) v.getContext().getSystemService(Context.ALARM_SERVICE);
		
		Intent intent = new Intent(v.getContext(), BusDueAlert.class);
		intent.putExtra("serviceId", serviceId);
		intent.putExtra("time", time);
		intent.putExtra("destination", destination);
		intent.putExtra("alertId", index);
		
		PendingIntent action = PendingIntent.getBroadcast(
				v.getContext(),
				index,
				intent,
				PendingIntent.FLAG_ONE_SHOT);
		
		/* Time to send alert at */
		Date timeDate = TimeFormatter.formattedTimeToDate(time);
		long timeLong = timeDate.getTime();
		
		/* It is possible that the user is setting an alert
		 * for a bus which falls within the reminder time window.
		 * In this situation, just fire the event off now. Uses an
		 * arbitrary 3 second delay.
		 */
		long currentTimeMs = System.currentTimeMillis();
		
		if (timeLong - minutesBefore <= currentTimeMs) {
			timeLong = currentTimeMs + 3000;
		} else {
			timeLong -= minutesBefore;
		}
		
		alarmManager.set(AlarmManager.RTC_WAKEUP, timeLong, action);
		
		/* Increment alert counter in alert preferences */
		SharedPreferences.Editor prefEditor = sp.edit();
		prefEditor.putInt(v.getContext().getString(R.string.pref_alertIndex), index + 1);
		prefEditor.commit();

	}
	
	
	/**
	 * Creates a new alert using a specified number of minutes for the notification.
	 * Deletes the original one.
	 */
	public static void amendAlert(View v, int alertID, long minutesBefore) {
		
		Cursor c = DatabaseHelper.getInstance(v.getContext()).getAlert(alertID);
		c.moveToFirst();
		
		String serviceId = c.getString(DatabaseHelper.ALERT_SERVICE);
		String destination = c.getString(DatabaseHelper.ALERT_DESTINATION);
		String stopCode = c.getString(DatabaseHelper.ALERT_STOP_CODE);
		String time = c.getString(DatabaseHelper.ALERT_TIME);
		
		// add new alert
		addAlert(v, serviceId, destination, stopCode, time, minutesBefore);
		// delete old alert
		DatabaseHelper.getInstance(v.getContext()).deleteAlert(alertID);
				
	}
	
	
	
	/**
	 * Adapter class for a database cursor to all alerts.
	 * 
	 * @author Aidan Smeaton
	 * @author Euan Freeman
	 */
	private class AlertAdapter extends CursorAdapter {
		private LayoutInflater inflater;
		/* Update interval ~10seconds */
		private final static long updateInterval = 10000;
		
		public AlertAdapter(Context context, Cursor cursor) {
			super(context, cursor);
			
			inflater = LayoutInflater.from(context);
			
			/* Create a Handler to queue a Runnable to run
			 * on the thread which creates AlertAdapter, i.e.
			 * the main UI thread. This means we can update the
			 * UI periodically.
			 */
			final Handler handler = new Handler();
			
			/* Post a delayed message after 'updateInterval'
			 * which tells the AlertsActivity class to
			 * update its cursor. This causes this adapter
			 * to refresh, hence updating the time remaining text.
			 */
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					/* If state is ACTIVE, the alert is on
					 * screen. In this state, we want to update.
					 */
					if (state == AlertsState.ACTIVE) {
						AlertsActivity.this.updateCursor();
					/* If state is DESTROYED, the alert has
					 * been popped from the Activity Stack
					 * and there is no need to update any more.
					 */
					} else if (state == AlertsState.DESTROYED) {
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
        public void bindView(View view, Context context, Cursor cursor) {
			view.setTag(R.id.stopcode, cursor.getString(DatabaseHelper.ALERT_STOP_CODE));
            view.setTag(R.id.alerts_header, cursor.getString(DatabaseHelper.ALERT_ID)); //
            
            TextView service = (TextView) view.findViewById(R.id.alerts_service);
            service.setText(cursor.getString(DatabaseHelper.ALERT_SERVICE));
            
            TextView destination = (TextView) view.findViewById(R.id.alerts_destination);
            destination.setText(cursor.getString(DatabaseHelper.ALERT_DESTINATION));
            
            TextView stopName = (TextView) view.findViewById(R.id.alerts_stop);
            String stopCode = cursor.getString(DatabaseHelper.ALERT_STOP_CODE).trim(); // get stop code
            stopName.setText(DatabaseHelper.getInstance(AlertsActivity.this).getAliasOrName(stopCode));
            
            TextView alarm = (TextView) view.findViewById(R.id.alerts_alarm);
            alarm.setText(cursor.getString(DatabaseHelper.ALERT_MINUTES_BEFORE) + " mins");
            
            TextView time = (TextView) view.findViewById(R.id.alerts_time);
            String storedTime = cursor.getString(DatabaseHelper.ALERT_TIME);
            time.setText(TimeFormatter.getHoursAndMins(cursor.getString(DatabaseHelper.ALERT_TIME)));
            
            TextView countDown = (TextView) view.findViewById(R.id.alerts_countdown);
            countDown.setTextColor(Color.WHITE);
            
            long[] times = TimeFormatter.getTimeComponents(storedTime);
            boolean rightNow = (times[0] == 0 && times[1] == 0 && times[2] == 0);
            
            if (!rightNow) {
                    String countDownString = TimeFormatter.writeCountDownString(times[0], times[1], times[2]);
                    boolean alertIsOld = (times[3] == 1);
                    
                    if (alertIsOld) {
                            countDown.setText(countDownString + " ago");
                            countDown.setTextColor(Color.RED);
                    } else {
                            countDown.setText("in " + countDownString);
                    }
            }  else {
                    countDown.setText("Due right now");
                    countDown.setTextColor(Color.YELLOW);
            }
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
                return inflater.inflate(R.layout.alerts_row, null);
        }
	}

}
