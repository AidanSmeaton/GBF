package teamk.glasgowbusfinder.quickactions;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
import teamk.glasgowbusfinder.R;
import teamk.glasgowbusfinder.data.BusStop;
import teamk.glasgowbusfinder.data.DatabaseHelper;
import teamk.glasgowbusfinder.location.MyLocationListener;
import android.graphics.Rect;
import android.location.Location;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Toast;

import com.google.android.maps.OverlayItem;

/**
 * Provides a quick-action popup for a bus stop map
 * overlay item. This class implements the singleton
 * pattern to avoid having to instantiate several
 * quick actions, each for a different bus stop. 
 * 
 * @author Euan Freeman
 * @author Calum McCall
 * @see MapQuickAction
 */
public class MapBusStopQuickAction extends BusStopQuickAction {
	private ActionItem distanceToActionItem;
	private OverlayItem item;
	
	public MapBusStopQuickAction(View view, OverlayItem item) {
		/* Ugly, but it's the only way around the rule that the call to
		 * constructor has to come first...
		 */
		super(view, new BusStop(item.getTitle(),
				DatabaseHelper.getInstance(
						view.getContext()).hasAlias(item.getTitle()) ? 
								DatabaseHelper.getInstance(view.getContext()).getAlias(item.getTitle()) :
								item.getSnippet(),
				DatabaseHelper.getInstance(view.getContext()).isFavourite(item.getTitle())));
		
		this.item = item;
		
		setAnimStyle(QuickAction.ANIM_AUTO);
		
		distanceToActionItem = new ActionItem(view.getResources().getDrawable(R.drawable.ic_quickaction_distanceto));
		distanceToActionItem.setTitle(view.getResources().getString(R.string.distance_to));
		
		distanceToActionItem.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onDistanceToClick(v);
			}
		});
		
		clearTrack();
		
		addActionItem(distanceToActionItem);
		
		createActionList();
	}
	
	protected void onDistanceToClick(View v) {
		dismiss();
		
		Location source = MyLocationListener.getInstance().getLocation();
		
		if (source == null) {
			Toast.makeText(v.getContext(), R.string.no_location_message, Toast.LENGTH_LONG);
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

		int screenWidth = windowManager.getDefaultDisplay().getWidth();
		int screenHeight = windowManager.getDefaultDisplay().getHeight();
		
		int xPos = (screenWidth - rootWidth) / 2;
		
		int arrowHeight = this.getContext().getResources().getDrawable(R.drawable.quickaction_arrow_up).getIntrinsicHeight();
		
		int yPos = screenHeight / 2 + arrowHeight + 5;

		showArrow(R.id.arrow_up, anchorRect.centerX());
		setAnimationStyle(screenWidth, anchorRect.centerX(), false);
		
		showAt(xPos, yPos);
	}
}