package teamk.glasgowbusfinder.quickactions;

import net.londatiga.android.ActionItem;
import teamk.glasgowbusfinder.AlertsActivity;
import teamk.glasgowbusfinder.R;
import teamk.glasgowbusfinder.StopMapActivity;
import teamk.glasgowbusfinder.data.BusStop;
import teamk.glasgowbusfinder.data.DatabaseHelper;
import teamk.glasgowbusfinder.location.MyLocationListener;
import teamk.glasgowbusfinder.util.InputValidator;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Quick-Action popup for an alert.
 * 
 * @author Aidan Smeaton
 */
public class AlertQuickAction extends BusStopListQuickAction {
	private int alertID;
	private ActionItem serviceActionItem;
	private ActionItem amendAlertActionItem;
	private ActionItem removeAlertActionItem;
	
	private ActionItem distanceToActionItem;
	
	public AlertQuickAction(View view, BusStop stop, final AlertsActivity alertsActivity) {
		super(view, stop, alertsActivity);
		
		this.alertID = Integer.parseInt((String)view.getTag(R.id.alerts_header));
		
		/* Amend Alert actionitem */
		amendAlertActionItem = new ActionItem(view.getResources().getDrawable(R.drawable.ic_quickaction_alter_alert));
		amendAlertActionItem.setTitle(view.getResources().getString(R.string.amend_alert));
		
		amendAlertActionItem.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onAmendAlertClick(v, alertsActivity);
			}
		});
		
		/* Remove Alert actionitem */
		removeAlertActionItem = new ActionItem(view.getResources().getDrawable(R.drawable.ic_quickaction_cancel_alert));
		removeAlertActionItem.setTitle(view.getResources().getString(R.string.remove_alert));
		
		removeAlertActionItem.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				onRemoveAlertClick(v, alertsActivity);
			}
		});
		
		/* View Service actionitem */
		serviceActionItem = new ActionItem(view.getResources().getDrawable(R.drawable.ic_quickaction_map_service));
		serviceActionItem.setTitle(view.getResources().getString(R.string.service_on_map));
		
		serviceActionItem.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onServiceMapClick(v);
			}
		});
		
		/* Distance actionitem */
		distanceToActionItem = new ActionItem(view.getResources().getDrawable(R.drawable.ic_quickaction_distanceto));
		distanceToActionItem.setTitle(view.getResources().getString(R.string.distance_to));
		
		distanceToActionItem.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onDistanceToClick(v);
			}
		});
		
		
		/* Add all items to the action list */
		clearTrack();
		
		addActionItemToHead(distanceToActionItem);
		addActionItemToHead(serviceActionItem);
		addActionItemToHead(removeAlertActionItem);
		addActionItemToHead(amendAlertActionItem);
		
		createActionList();
		
		TextView quickActionTitle = (TextView) getRoot().findViewById(R.id.quickaction_stopname);
		quickActionTitle.setText(R.string.alert_options);
	}
	
	protected void onDistanceToClick(View v) {
		dismiss();
		
		Location source = MyLocationListener.getInstance().getLocation();
		
		if (source == null) {
			Toast.makeText(v.getContext(), R.string.no_location_message, Toast.LENGTH_LONG);
			return;
		}
		
		Cursor alertCursor = DatabaseHelper.getInstance(getContext()).getAlert(alertID);
		alertCursor.moveToFirst();
		
		Cursor stopCursor = DatabaseHelper.getInstance(getContext()).getBusStop(alertCursor.getString(DatabaseHelper.ALERT_STOP_CODE));
		stopCursor.moveToFirst();
		
		Location destination = new Location("Destination");
		destination.setLongitude(stopCursor.getDouble(DatabaseHelper.BUS_STOP_LONGITUDE));
		destination.setLatitude(stopCursor.getDouble(DatabaseHelper.BUS_STOP_LATITUDE));
		
		alertCursor.close();
		stopCursor.close();
		
		float distance = source.distanceTo(destination);
		
		Toast.makeText(v.getContext(), "Distance is approximately " +
				MapBusStopQuickAction.getDistanceString(distance) +
				".", Toast.LENGTH_SHORT).show();
	}

	protected void onServiceMapClick(View v) {
		Cursor alertCursor = DatabaseHelper.getInstance(getContext()).getAlert(alertID);
		alertCursor.moveToFirst();
		
		boolean serviceExists = DatabaseHelper.getInstance(getContext()).serviceExists(alertCursor.getString(DatabaseHelper.ALERT_SERVICE));
		
		if (serviceExists) {
	    	Intent mapIntent = new Intent(getContext(), StopMapActivity.class);
			mapIntent.putExtra("stopcode", alertCursor.getString(DatabaseHelper.ALERT_STOP_CODE));
			mapIntent.putExtra("route", alertCursor.getString(DatabaseHelper.ALERT_SERVICE));
			getContext().startActivity(mapIntent);
		} else {
			Toast.makeText(v.getContext(), R.string.no_service_message, Toast.LENGTH_LONG).show();
		}
		
		alertCursor.close();
	}

	protected void onRemoveAlertClick(View v, AlertsActivity alertsPage) {
		DatabaseHelper.getInstance(getContext()).deleteAlert(alertID);
		alertsPage.onResume();
		this.dismiss();
		
	}

	protected void onAmendAlertClick(View v, AlertsActivity alertsPage) {
		final Dialog dialog = new Dialog(getContext());
		dialog.setContentView(R.layout.alert_amend);
		dialog.setTitle(R.string.amend_alert);
		dialog.setCancelable(true);
		
		EditText searchBar = (EditText) dialog.findViewById(R.id.amend_alert_text);
		
		Button changeButton = (Button) dialog.findViewById(R.id.amend_alert_change);
		
		Button cancelButton = (Button) dialog.findViewById(R.id.amend_alert_cancel);
				
		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
		
		changeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
				// parse user-entered minutes
				EditText searchBar = (EditText) dialog.findViewById(R.id.amend_alert_text);
				String text = searchBar.getText().toString();
				
				if (InputValidator.validateInteger(getContext(),text)){
					
					long minutesBefore = Long.parseLong(text) * 60000;
					
					// amend the alert
					AlertsActivity.amendAlert(v, alertID, minutesBefore);
				}
				
				dialog.dismiss();
			}
		});
		
		searchBar.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// do nothing
			}
		});
		
		dialog.show();
		alertsPage.onResume();
		
	}

}
