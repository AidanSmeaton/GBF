package teamk.glasgowbusfinder.quickactions;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
import teamk.glasgowbusfinder.R;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.google.android.maps.OverlayItem;

/**
 * A base class for map quick-action popups. Has one
 * action item to calculate the distance from the user's
 * last known location to a selected overlay item.
 * 
 * @author Euan Freeman
 */
public class MapQuickAction extends QuickAction {
	private OverlayItem item;
	private ActionItem distanceToActionItem;
	
	public MapQuickAction(View view, OverlayItem item) {
		super(view);
		
		this.item = item;
		
		distanceToActionItem = new ActionItem(view.getResources().getDrawable(R.drawable.ic_quickaction_distanceto));
		distanceToActionItem.setTitle(view.getResources().getString(R.string.distance_to));
		
		distanceToActionItem.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onDistanceToClick(v);
			}
		});
		
		addActionItem(distanceToActionItem);
	}
	
	protected OverlayItem getItem() {
		return item;
	}

	protected void setItem(OverlayItem item) {
		this.item = item;
	}

	protected void onDistanceToClick(View v) {
		dismiss();
		
		LocationManager locManager = (LocationManager) v.getContext().getSystemService(Context.LOCATION_SERVICE);
		
		Location source = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		
		if (source == null) {
			Toast.makeText(v.getContext(), "Your current location is not available.", Toast.LENGTH_LONG);
			return;
		}
		
		Location destination = new Location("Destination");
		destination.setLongitude(item.getPoint().getLongitudeE6() / 1e6);
		destination.setLatitude(item.getPoint().getLatitudeE6() / 1e6);
		
		float distance = source.distanceTo(destination);
		
		Toast.makeText(v.getContext(), "Distance is approximately " + getDistanceString(distance) + ".", Toast.LENGTH_SHORT).show();
	}
	
	public static String getDistanceString(float distance) {
		if (distance >= 1000.0) {
			return String.format("%.2fkm", distance / 1000);
		} else {
			return String.format("%dm", (int) distance);
		}
	}
}