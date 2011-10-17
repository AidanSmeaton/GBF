package teamk.glasgowbusfinder.mapoverlays;

import teamk.glasgowbusfinder.data.BusStop;
import android.graphics.drawable.Drawable;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

/**
 * A map overlay item for a bus stop.
 * 
 * @author Euan Freeman
 */
public class BusStopOverlayItem extends OverlayItem {
	private BusStop stop;
	private Drawable drawable;
	
	public BusStopOverlayItem(GeoPoint point, BusStop stop) {
		super(point, stop.getStopCode(), stop.getName());
		
		this.stop = stop;
	}

	public BusStop getStop() {
		return stop;
	}

	public void setStop(BusStop stop) {
		this.stop = stop;
	}

	public Drawable getDrawable() {
		return drawable;
	}

	public void setDrawable(Drawable drawable) {
		this.drawable = drawable;
	}
}
