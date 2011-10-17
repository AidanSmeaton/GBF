package teamk.glasgowbusfinder.location;

import android.location.Location;

/* This interface must be implemented by
 * any object which wishes to receive
 * location updates from MyLocationListener.
 */
public interface LocationUpdateObserver {
	/**
	 * Receive a location update from MyLocationListener.
	 */
	public void locationUpdate(Location loc);
}
