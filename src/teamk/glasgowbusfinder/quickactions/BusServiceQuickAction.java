package teamk.glasgowbusfinder.quickactions;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
import teamk.glasgowbusfinder.AlertsActivity;
import teamk.glasgowbusfinder.DeparturesActivity;
import teamk.glasgowbusfinder.R;
import teamk.glasgowbusfinder.StopMapActivity;
import teamk.glasgowbusfinder.data.DatabaseHelper;
import teamk.glasgowbusfinder.data.Departure;
import teamk.glasgowbusfinder.util.TimeFormatter;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Rect;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Quick-Action popup for a bus service.
 * 
 * @author Aidan Smeaton
 */
public class BusServiceQuickAction extends QuickAction {
	private ActionItem serviceActionItem;
	private ActionItem setAlertActionItem;
	protected String stopCode;
	protected String serviceId;
	protected boolean serviceExists;
	protected String time;
	protected String destination;
	protected DatabaseHelper db;
	protected DeparturesActivity dA;
	
	public BusServiceQuickAction(View view, DeparturesActivity dA, DatabaseHelper db, String stopCode, String serviceId, boolean serviceExists, String destination, String time) {
		super(view);
		this.db = db;
		this.stopCode = stopCode;
		this.serviceId = serviceId;
		this.serviceExists = serviceExists;
		this.destination = destination;
		this.time = time;
		this.dA = dA;
			
		/* View Service actionitem */
		serviceActionItem = new ActionItem(view.getResources().getDrawable(R.drawable.ic_quickaction_map_service));
		serviceActionItem.setTitle(view.getResources().getString(R.string.service_on_map));
		
		serviceActionItem.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onServiceClick(v);
			}
		});
		addActionItem(serviceActionItem);
		
		/* Set Alert actionitem */
		setAlertActionItem = new ActionItem();
				
		if (db.hasAlert(new Departure(serviceId, destination, time), stopCode)){
			setAlertActionItem.setIcon(view.getResources().getDrawable(R.drawable.ic_quickaction_cancel_alert));
			setAlertActionItem.setTitle(view.getResources().getString(R.string.remove_alert));
			setAlertActionItem.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					onAlertRemoveClick(v);
				}
			});
		}
		else {
			setAlertActionItem.setIcon(view.getResources().getDrawable(R.drawable.ic_quickaction_alerts));
			setAlertActionItem.setTitle(view.getResources().getString(R.string.set_alert));
			setAlertActionItem.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					onAlertClick(v);
				}
			});
		}
		
		addActionItem(setAlertActionItem);		
		
		/* Add all items to the action list */
		createActionList();
		
		setAnchor(view);
		createWindow();
		
		TextView quickActionTitle = (TextView) getRoot().findViewById(R.id.quickaction_stopname);
		quickActionTitle.setText("The " + serviceId + " at " + time);
	}
	
	protected void onAlertClick(View v) {
		
		String formattedTime = TimeFormatter.formatTime(time);
		
		AlertsActivity.addAlert(v, serviceId, destination, stopCode, formattedTime);
		
		Toast.makeText(v.getContext(), "Alert saved.", Toast.LENGTH_SHORT).show();
		
		this.dismiss();
		dA.invalidateAdapter();
		
	}
	
	
	protected void onAlertRemoveClick(View v) {
		
		Cursor c = DatabaseHelper.getInstance(getContext()).getAlert(new Departure(serviceId, destination, time), stopCode);
		c.moveToFirst();
		DatabaseHelper.getInstance(getContext()).deleteAlert(c.getInt(DatabaseHelper.ALERT_ID));
		this.dismiss();
		dA.invalidateAdapter();	
		
	}
	
	
	protected void onServiceClick(View v) {
		
		if (serviceExists) {
	    	Intent mapIntent = new Intent(getContext(), StopMapActivity.class);
			mapIntent.putExtra("stopcode", stopCode);
			mapIntent.putExtra("route", serviceId);
			getContext().startActivity(mapIntent);
		} else {
			Toast.makeText(v.getContext(), R.string.no_service_message, Toast.LENGTH_LONG).show();
		}
		
	}
	
	
	/**
	 * Show popup window
	 */
	@Override
	public void show() {
		preShow();

		int[] location = new int[2];

		anchor.getLocationOnScreen(location);

		Rect anchorRect = new Rect(location[0], location[1], location[0]
				+ anchor.getWidth(), location[1] + anchor.getHeight());

		getRoot().setLayoutParams(
				new LayoutParams(LayoutParams.WRAP_CONTENT,
						LayoutParams.WRAP_CONTENT));
		getRoot().measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

		int rootWidth = getRoot().getMeasuredWidth();
		int rootHeight = getRoot().getMeasuredHeight();

		int screenWidth = windowManager.getDefaultDisplay().getWidth();

		int xPos = (screenWidth - rootWidth) / 2;
		int yPos = anchorRect.top - rootHeight;

		boolean onTop = true;

		// display on bottom
		if (rootHeight > anchor.getTop()) {
			yPos = anchorRect.bottom;
			onTop = false;
		}

		showArrow(((onTop) ? R.id.arrow_down : R.id.arrow_up),
				anchorRect.centerX());
		setAnimationStyle(screenWidth, anchorRect.centerX(), onTop);

		showAt(xPos, yPos);
	}
}
