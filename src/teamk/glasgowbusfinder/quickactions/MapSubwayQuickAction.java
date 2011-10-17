package teamk.glasgowbusfinder.quickactions;

import com.google.android.maps.OverlayItem;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
import teamk.glasgowbusfinder.R;
import teamk.glasgowbusfinder.data.DatabaseHelper;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

/**
 * Provides a quick-action popup for subway map
 * overlay items. Displays some non-interactive action
 * items which display whether or not the station
 * has particular features.
 * 
 * @author Euan Freeman
 */
public class MapSubwayQuickAction extends MapQuickAction {
	private ActionItem parkAndRide;
	private ActionItem railInterchange;
	private ActionItem busInterchange;
	private Drawable yesDrawable;
	private Drawable noDrawable;
	
	public MapSubwayQuickAction(View view, OverlayItem item) {
		super(view, item);
		
		setAnimStyle(QuickAction.ANIM_AUTO);
		
		yesDrawable = view.getResources().getDrawable(R.drawable.tick);
		noDrawable = view.getResources().getDrawable(R.drawable.cross);
		
		/* Get information about subway station from db */
		Cursor c = DatabaseHelper.getInstance(view.getContext()).getSubwayStation(getItem().getTitle());
		
		c.moveToFirst();
		
		boolean isParkAndRide = c.getInt(DatabaseHelper.SUBWAY_STATION_PARKANDRIDE) == 1;
		boolean isBusInterchange = c.getInt(DatabaseHelper.SUBWAY_STATION_BUSINTERCHANGE) == 1;
		boolean isRailInterchange = c.getInt(DatabaseHelper.SUBWAY_STATION_RAILINTERCHANGE) == 1;
		
		/* Create ActionItems. */
		
		if (isParkAndRide) {
			parkAndRide = new ActionItem(yesDrawable);
		} else {
			parkAndRide = new ActionItem(noDrawable);
		}
		
		parkAndRide.setTitle("Park & Ride");
		addActionItem(parkAndRide);
		
		if (isRailInterchange) {
			railInterchange = new ActionItem(yesDrawable);
		} else {
			railInterchange = new ActionItem(noDrawable);
		}
		
		railInterchange.setTitle("Rail Interchange");
		addActionItem(railInterchange);
		
		if (isBusInterchange) {
			busInterchange = new ActionItem(yesDrawable);
		} else {
			busInterchange = new ActionItem(noDrawable);
		}
		
		busInterchange.setTitle("Bus Interchange");
		addActionItem(busInterchange);
		
		createActionList();
		
		TextView stopName = (TextView) getRoot().findViewById(R.id.quickaction_stopname);
		stopName.setText(c.getString(DatabaseHelper.SUBWAY_STATION_NAME) + " Subway Station");
	}
}